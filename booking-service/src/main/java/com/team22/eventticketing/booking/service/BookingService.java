package com.team22.eventticketing.booking.service;

import com.team22.eventticketing.booking.dto.BookingAnalyticsDTO;
import com.team22.eventticketing.booking.dto.BookingCostEstimateDTO;
import com.team22.eventticketing.booking.dto.BookingDetailsDTO;
import com.team22.eventticketing.booking.dto.BookingItemDTO;
import com.team22.eventticketing.booking.dto.BookingItemRequest;
import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.model.BookingItem;
import com.team22.eventticketing.booking.model.BookingItemStatus;
import com.team22.eventticketing.booking.model.BookingStatus;
import com.team22.eventticketing.booking.repository.BookingItemRepository;
import com.team22.eventticketing.booking.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;

    public BookingService(BookingRepository bookingRepository, BookingItemRepository bookingItemRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
    }

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
        return bookingRepository.save(booking);
    }

    // ─── Read by ID ───────────────────────────────────────────────────────────────

    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found with id: " + id));
    }

    // ─── Read all ─────────────────────────────────────────────────────────────────

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    // ─── Read by userId ───────────────────────────────────────────────────────────

    public List<Booking> findByUserId(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    // ─── Update ───────────────────────────────────────────────────────────────────

    public Booking update(Long id, Booking patch) {
        Booking existing = findById(id);

        if (patch.getContactEmail() != null) existing.setContactEmail(patch.getContactEmail());
        if (patch.getStatus() != null)       existing.setStatus(patch.getStatus());
        if (patch.getTotalAmount() != null)  existing.setTotalAmount(patch.getTotalAmount());
        if (patch.getMetadata() != null)     existing.setMetadata(patch.getMetadata());
        if (patch.getConfirmedAt() != null)  existing.setConfirmedAt(patch.getConfirmedAt());
        if (patch.getEventId() != null)      existing.setEventId(patch.getEventId());

        return bookingRepository.save(existing);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────────

    public void delete(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Booking not found with id: " + id);
        }
        bookingRepository.deleteById(id);
    }

    // ─── S3-F4: Complete Booking ──────────────────────────────────────────────────

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

        return saved;
    }

    // ─── S3-F5: Filter Bookings by Metadata ──────────────────────────────────────

    public List<Booking> filterByMetadata(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Key and value must not be blank");
        }
        return bookingRepository.findByMetadataField(key, value);
    }

    // ─── S3-F6: Booking Analytics ─────────────────────────────────────────────────

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
    }

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

        return bookingRepository.findByIdWithItems(bookingId).get();
    }

    public BookingDetailsDTO getBookingDetails(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithItems(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found with id: " + bookingId));

        List<BookingItemDTO> itemDTOs = booking.getBookingItems() == null
                ? List.of()
                : booking.getBookingItems().stream()
                .sorted((a, b) -> Integer.compare(a.getEventOrder(), b.getEventOrder()))
                .map(item -> BookingItemDTO.builder()
                        .id(item.getId())
                        .eventOrder(item.getEventOrder())
                        .sessionTitle(item.getSessionTitle())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .status(item.getStatus().name())
                        .metadata(item.getMetadata()).build()).collect(Collectors.toList());


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

        return bookingRepository.save(booking);
    }

    // ─── S3-F3: Get Booking Cost Estimate ─────────────────────────────────────────

    public BookingCostEstimateDTO estimateCost(Long eventId, Integer ticketCount, String ticketTier) {
        // 1. Get Average Capacity
        Double avgCapacity = bookingRepository.getAverageSessionCapacity(eventId);
        if (avgCapacity == null) {
            avgCapacity = 0.0;
        }

        // 2. Calculate Base Price and Tier Multiplier
        double basePrice = avgCapacity / 10.0;
        double tierMultiplier = "VIP".equalsIgnoreCase(ticketTier) ? 2.5 : 1.0;

        // 3. Calculate Ticket Cost
        double ticketCost = basePrice * tierMultiplier * ticketCount;

        // 4. Calculate Service Fee (15%)
        double serviceFee = ticketCost * 0.15;

        // 5. Determine Demand Multiplier
        Long activeBookings = bookingRepository.countActiveBookingsForEvent(eventId);
        double demandMultiplier = 1.0;
        if (activeBookings != null) {
            if (activeBookings > 200) {
                demandMultiplier = 1.5;
            } else if (activeBookings > 50) {
                demandMultiplier = 1.25;
            }
        }

        // 6. Calculate Estimated Total
        double estimatedTotal = (ticketCost + serviceFee) * demandMultiplier;

        // Return the mapped DTO
        return BookingCostEstimateDTO.builder()
                .ticketCost(ticketCost)
                .serviceFee(serviceFee)
                .estimatedTotal(estimatedTotal)
                .demandMultiplier(demandMultiplier)
                .build();
    }
}