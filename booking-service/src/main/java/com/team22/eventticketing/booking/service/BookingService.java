package com.team22.eventticketing.booking.service;

import com.team22.eventticketing.booking.adapter.Neo4jRecordAdapter;
import com.team22.eventticketing.booking.dto.*;
import com.team22.eventticketing.booking.model.*;
import com.team22.eventticketing.booking.observer.EntityObserver;
import com.team22.eventticketing.booking.repository.BookingItemRepository;
import com.team22.eventticketing.booking.repository.BookingRepository;
import com.team22.eventticketing.booking.repository.EventNodeRepository;
import com.team22.eventticketing.booking.repository.UserNodeRepository;
import com.team22.eventticketing.booking.security.AuthContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team22.eventticketing.booking.repository.UserNodeCustomRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.neo4j.core.Neo4jClient;

@Service
public class BookingService {

    private static final String ESTIMATE_CACHE_PREFIX = "booking-service::S3-F3::";

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final CacheEvictionService cacheEviction;
    private final RedisTemplate<String, Object> redisTemplate;
    private final List<EntityObserver> observers = new ArrayList<>();


    //Injecting nodejs variables
    private final UserNodeRepository userNodeRepository;
    private final EventNodeRepository eventNodeRepository;
    private final UserNodeCustomRepository userNodeCustomRepository;

    public BookingService(BookingRepository bookingRepository,
                          BookingItemRepository bookingItemRepository,
                          CacheEvictionService cacheEviction,
                          RedisTemplate<String, Object> redisTemplate,
                          EventNodeRepository eventNodeRepository, UserNodeRepository userNodeRepository,List<EntityObserver> observers,UserNodeCustomRepository userNodeCustomRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.cacheEviction = cacheEviction;
        this.redisTemplate = redisTemplate;
        this.eventNodeRepository= eventNodeRepository;
        this.userNodeRepository=  userNodeRepository;
        this.observers.addAll(observers);
        this.userNodeCustomRepository = userNodeCustomRepository;

    }

    public void register(EntityObserver observer) { observers.add(observer); }
    public void unregister(EntityObserver observer) { observers.remove(observer); }

    private void notifyObservers(String action, Object payload) {
        for (EntityObserver o : observers) o.onEvent(action, payload);
    }

    // ─── [S3-F1] Search Bookings ──────────────────────────────────────────────────

    @Cacheable(cacheNames = "booking-service::S3-F1",
            key = "T(String).valueOf(#status) + ':' + T(String).valueOf(#startDate) + ':' + T(String).valueOf(#endDate)")
    public List<Booking> searchBookings(BookingStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        if (status != null) {
            return bookingRepository.findByStatusAndBookingDateBetweenOrderByBookingDateDesc(status, startDate, endDate);
        } else {
            return bookingRepository.findByBookingDateBetweenOrderByBookingDateDesc(startDate, endDate);
        }
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    public Booking create(Booking booking) {
        if (booking.getBookingDate() == null) {
            booking.setBookingDate(LocalDateTime.now());
        }
        Booking saved = bookingRepository.save(booking);
        notifyObservers("BOOKING_CREATED", Map.of("bookingId", saved.getId()));
        return saved;
    }

    // ─── [CRUD] Read by ID ────────────────────────────────────────────────────────

    @Cacheable(cacheNames = "booking-service::booking", key = "#id")
    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found with id: " + id));
    }

    // ─── Read all (NOT cached) ────────────────────────────────────────────────────

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public List<Booking> findByUserId(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    // ─── [CRUD] Update ────────────────────────────────────────────────────────────

    public Booking update(Long id, Booking patch) {
        Booking existing = findById(id);

        if (patch.getContactEmail() != null) existing.setContactEmail(patch.getContactEmail());
        if (patch.getStatus() != null)       existing.setStatus(patch.getStatus());
        if (patch.getTotalAmount() != null)  existing.setTotalAmount(patch.getTotalAmount());
        if (patch.getMetadata() != null)     existing.setMetadata(patch.getMetadata());
        if (patch.getConfirmedAt() != null)  existing.setConfirmedAt(patch.getConfirmedAt());
        if (patch.getEventId() != null)      existing.setEventId(patch.getEventId());

        Booking saved = bookingRepository.save(existing);
        cacheEviction.evictEntityAndFeatures("booking", id, "S3-F1", "S3-F5", "S3-F6", "S3-F9", "S3-F10");
        notifyObservers("BOOKING_UPDATED", Map.of("bookingId", saved.getId()));
        return saved;
    }

    // ─── [CRUD] Delete ────────────────────────────────────────────────────────────

    public void delete(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Booking not found with id: " + id);
        }
        bookingRepository.deleteById(id);
        cacheEviction.evictEntityAndFeatures("booking", id, "S3-F1", "S3-F5", "S3-F6", "S3-F9", "S3-F10");
        notifyObservers("BOOKING_DELETED", Map.of("bookingId", id));
    }

    // ─── [S3-F4] Complete Booking (write — invalidates) ──────────────────────────

    @Transactional
    public Booking completeBooking(Long id) {
        Booking booking = bookingRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found with id: " + id));

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only CHECKED_IN bookings can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);

        if (booking.getTotalAmount() == null && booking.getBookingItems() != null) {
            double total = booking.getBookingItems().stream()
                    .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                    .sum();
            booking.setTotalAmount(total);
        }

        Booking saved = bookingRepository.save(booking);

        bookingRepository.insertTicketSale(
                saved.getId(),
                saved.getUserId(),
                saved.getTotalAmount() != null ? saved.getTotalAmount() : 0.0
        );

        cacheEviction.evictEntityAndFeatures("booking", id, "S3-F1", "S3-F6", "S3-F9", "S3-F10");
        notifyObservers("BOOKING_COMPLETED", Map.of("bookingId", saved.getId()));
        return saved;
    }

    // ─── [S3-F5] Filter Bookings by Metadata (JSONB) ─────────────────────────────

    @Cacheable(cacheNames = "booking-service::S3-F5", key = "#key + ':' + #value")
    public List<Booking> filterByMetadata(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Key and value must not be blank");
        }
        return bookingRepository.findByMetadataField(key, value);
    }

    // ─── [S3-F6] Booking Analytics (report) ──────────────────────────────────────

    @Cacheable(cacheNames = "booking-service::S3-F6",
            key = "T(String).valueOf(#start) + ':' + T(String).valueOf(#end)")
    public BookingAnalyticsDTO getAnalytics(LocalDateTime start, LocalDateTime end) {
        long total     = bookingRepository.countInRange(start, end);
        long completed = bookingRepository.countCompletedInRange(start, end);
        long cancelled = bookingRepository.countCancelledInRange(start, end);
        double revenue = bookingRepository.sumRevenueInRange(start, end);
        double avg     = completed > 0 ? revenue / completed : 0.0;
        double rate    = total > 0 ? (completed * 100.0) / total : 0.0;

        return BookingAnalyticsDTO.builder()
                .totalBookings(total)
                .completedBookings(completed)
                .cancelledBookings(cancelled)
                .totalRevenue(revenue)
                .averageBookingAmount(avg)
                .completionRate(rate)
                .build();
    }

    // ─── [S3-F2] Cancel Booking (write — invalidates) ────────────────────────────

    @Transactional
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found with id: " + id));

        if (booking.getStatus() != BookingStatus.PENDING &&
                booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PENDING or CONFIRMED bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        bookingRepository.cancelValidTicketsForBooking(id);
        cacheEviction.evictEntityAndFeatures("booking", id, "S3-F1", "S3-F6", "S3-F9", "S3-F10");
        notifyObservers("BOOKING_CANCELLED", Map.of("bookingId", id));
    }

    // ─── [S3-F8] Add Items to Booking (write — invalidates) ──────────────────────

    @Transactional
    public Booking addItemsToBooking(Long bookingId, List<BookingItemRequest> requests) {
        Booking booking = bookingRepository.findByIdWithItems(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING &&
                booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot add items to a booking that is not PENDING or CONFIRMED");
        }

        for (BookingItemRequest req : requests) {
            if (req.getSessionId() == null || req.getSessionTitle() == null ||
                    req.getSessionTitle().isBlank() || req.getQuantity() == null ||
                    req.getUnitPrice() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each item must have sessionId, sessionTitle, quantity, and unitPrice");
            }
        }

        int currentMax = bookingRepository.findMaxEventOrderByBookingId(bookingId);

        for (BookingItemRequest req : requests) {
            currentMax++;
            BookingItem item = new BookingItem();
            item.setEventOrder(currentMax);
            item.setSessionId(req.getSessionId());
            item.setSessionTitle(req.getSessionTitle());
            item.setQuantity(req.getQuantity());
            item.setUnitPrice(req.getUnitPrice());
            item.setStatus(BookingItemStatus.RESERVED);
            item.setMetadata(req.getMetadata());
            item.setBooking(booking);
            bookingItemRepository.save(item);
        }

        Booking result = bookingRepository.findByIdWithItems(bookingId).get();
        cacheEviction.evictEntityAndFeatures("booking", bookingId, "S3-F1", "S3-F6", "S3-F9", "S3-F10");
        notifyObservers("BOOKING_ITEMS_ADDED", Map.of("bookingId", bookingId));
        return result;
    }

    // ─── [S3-F9] Booking Details DTO ─────────────────────────────────────────────

    @Cacheable(cacheNames = "booking-service::S3-F9", key = "#bookingId")
    public BookingDetailsDTO getBookingDetails(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithItems(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found with id: " + bookingId));

        List<BookingItemDTO> itemDTOs = booking.getBookingItems() == null
                ? List.of()
                : booking.getBookingItems().stream()
                .sorted((a, b) -> Integer.compare(a.getEventOrder(), b.getEventOrder()))
                .map(item -> new BookingItemDTO(
                        item.getId(),
                        item.getEventOrder(),
                        item.getSessionTitle(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getStatus().name(),
                        item.getMetadata()
                ))
                .collect(Collectors.toList());

        long confirmedItems = itemDTOs.stream()
                .filter(i -> i.getStatus().equals("CONFIRMED"))
                .count();

        return BookingDetailsDTO.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .eventId(booking.getEventId())
                .status(booking.getStatus().name())
                .totalAmount(booking.getTotalAmount())
                .metadata(booking.getMetadata())
                .items(itemDTOs)
                .totalItems(itemDTOs.size())
                .confirmedItems(confirmedItems)
                .build();
    }

    // ─── [S3-F7] Confirm Booking (write — invalidates) ───────────────────────────

    @Transactional
    public Booking confirmBooking(Long bookingId, Long eventId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        String eventStatus = bookingRepository.findEventStatusById(eventId);
        if (eventStatus == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event with ID " + eventId + " not found");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is not PENDING. Current status: " + booking.getStatus());
        }

        if (!"UPCOMING".equals(eventStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is not UPCOMING. Current status: " + eventStatus);
        }

        booking.setEventId(eventId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);
        cacheEviction.evictEntityAndFeatures("booking", bookingId, "S3-F1", "S3-F9", "S3-F10");
        notifyObservers("BOOKING_CONFIRMED", Map.of("bookingId", saved.getId(), "eventId", eventId));
        return saved;
    }

    // ─── [S3-F3] Booking Cost Estimate (POST — manual cache by body hash) ─────────

    public BookingCostEstimateDTO estimateCost(Long eventId, Integer ticketCount, String ticketTier) {
        String cacheKey = ESTIMATE_CACHE_PREFIX + DigestUtils.sha256Hex(eventId + ":" + ticketCount + ":" + ticketTier);

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof BookingCostEstimateDTO dto) return dto;
        } catch (Exception ignored) {}

        Double avgCapacity = bookingRepository.getAverageSessionCapacity(eventId);
        if (avgCapacity == null) avgCapacity = 0.0;

        double basePrice = avgCapacity / 10.0;
        double tierMultiplier = "VIP".equalsIgnoreCase(ticketTier) ? 2.5 : 1.0;
        double ticketCost = basePrice * tierMultiplier * ticketCount;
        double serviceFee = ticketCost * 0.15;

        Long activeBookings = bookingRepository.countActiveBookingsForEvent(eventId);
        double demandMultiplier = 1.0;
        if (activeBookings != null) {
            if (activeBookings > 200)     demandMultiplier = 1.5;
            else if (activeBookings > 50) demandMultiplier = 1.25;
        }

        double estimatedTotal = (ticketCost + serviceFee) * demandMultiplier;
        BookingCostEstimateDTO result = BookingCostEstimateDTO.builder()
                .ticketCost(ticketCost)
                .serviceFee(serviceFee)
                .estimatedTotal(estimatedTotal)
                .demandMultiplier(demandMultiplier)
                .build();

        try {
            redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(5));
        } catch (Exception ignored) {}

        return result;
    }


    public void recordAttendance(Long bookingId) {
        // 1) Load booking from PostgreSQL
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found with id: " + bookingId
                ));

        // 2) Booking must be COMPLETED
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking must be COMPLETED"
            );
        }

        // 3) Booking must have an assigned event
        if (booking.getEventId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking has no assigned event"
            );
        }

        Long userId = booking.getUserId();
        Long eventId = booking.getEventId();

        // 4) Native SQL lookups for cross-service data
        String userName = bookingRepository.findUserNameById(userId);
        if (userName == null || userName.isBlank()) {
            userName = "Unknown";
        }

        List<Object[]> eventDetails = bookingRepository.findEventDetailsById(eventId);
        String eventName = "Unknown";
        String eventCategory = "UNKNOWN";

        if (eventDetails != null && !eventDetails.isEmpty()) {
            Object[] row = eventDetails.get(0);
            if (row.length > 0 && row[0] != null) {
                eventName = row[0].toString();
            }
            if (row.length > 1 && row[1] != null) {
                eventCategory = row[1].toString();
            }
        }

        System.out.println("userId=" + userId + ", eventId=" + eventId + ", bookingId=" + bookingId);

        // 5) Neo4j idempotency check
        if (userNodeCustomRepository.countRecorded(userId, eventId, bookingId) > 0) {
            return; // idempotent no-op
        }

        // 6-8) Write to Neo4j using raw driver — bypasses broken SDN template
        userNodeCustomRepository.recordAttendance(
                userId, userName,
                eventId, eventName, eventCategory,
                bookingId
        );

        // 9) Emit Mongo event
        notifyObservers(
                "INTERACTION_RECORDED",
                Map.of(
                        "bookingId", bookingId,
                        "userId", userId,
                        "eventId", eventId
                )
        );
    }
}
