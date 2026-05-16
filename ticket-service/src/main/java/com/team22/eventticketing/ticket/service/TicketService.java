package com.team22.eventticketing.ticket.service;

import com.team22.eventticketing.ticket.adapter.CassandraRowAdapter;
import com.team22.eventticketing.ticket.adapter.ObjectArrayDtoAdapter;
import com.team22.eventticketing.ticket.adapter.PostgresRowAdapter;
import com.team22.eventticketing.ticket.messaging.BookingEventContext;
import com.team22.eventticketing.ticket.messaging.TicketEventPublisher;
import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketStatus;
import com.team22.eventticketing.ticket.model.dto.BatchTicketRequestDTO;
import com.team22.eventticketing.ticket.model.dto.EventAttendanceSummaryDTO;
import com.team22.eventticketing.ticket.model.dto.NearbyTicketDTO;
import com.team22.eventticketing.ticket.model.dto.TicketAnalyticsDTO;
import com.team22.eventticketing.ticket.model.dto.TicketItemDTO;
import com.team22.eventticketing.ticket.model.dto.UnusedTicketDTO;
import com.team22.eventticketing.ticket.observer.EntityObserver;
import com.team22.eventticketing.ticket.repository.TicketRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private static final String[] WRITE_INVALIDATION_PATTERNS = {
            "S4-F1", "S4-F5", "S4-F6", "S4-F8", "S4-F9", "S4-F10"
    };

    private final TicketRepository ticketRepository;
    private final CacheEvictionService cacheEviction;
    private final TicketEventPublisher ticketEventPublisher;
    private final BookingEventContext bookingEventContext;
    private final List<EntityObserver> observers = new ArrayList<>();

    public TicketService(TicketRepository ticketRepository,
                         CacheEvictionService cacheEviction,
                         TicketEventPublisher ticketEventPublisher,
                         BookingEventContext bookingEventContext) {
        this.ticketRepository = ticketRepository;
        this.cacheEviction = cacheEviction;
        this.ticketEventPublisher = ticketEventPublisher;
        this.bookingEventContext = bookingEventContext;
    }

    public void register(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String action, Object payload) {
        for (EntityObserver o : observers)
            o.onEvent(action, payload);
    }

    // ─── Shared guards ───────────────────────────────────────────────────────────
    private void requireBookingExists(Long bookingId) {
        if (!ticketRepository.existsBookingById(bookingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
    }

    private void requireUniqueTicketCode(String ticketCode) {
        if (ticketRepository.existsByTicketCode(ticketCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket code already exists");
        }
    }

    private void evictAll(String... patterns) {
        for (String p : patterns) cacheEviction.evictPattern(p);
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────────
    @CacheEvict(value = "ticket-service::S4-F10", allEntries = true)
    public Ticket createTicket(Ticket ticket) {
        Ticket saved = ticketRepository.save(ticket);
        notifyObservers("TICKET_CREATED", Map.of("ticketId", saved.getId()));
        ticketEventPublisher.publishTicketIssued(
                saved.getId(),
                saved.getBookingId(),
                saved.getAttendeeName(),
                saved.getTicketCode());
        return saved;
    }

    @Cacheable(cacheNames = "ticket-service::ticket", key = "#id")
    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ticket not found with id: " + id));
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll(); // list — NOT cached
    }

    @CacheEvict(value = "ticket-service::S4-F10", allEntries = true)
    public Ticket updateTicket(Long id, Ticket updated) {
        Ticket existing = getTicketById(id);

        TicketStatus previousStatus = existing.getStatus();

        if (updated.getBookingId() != null)
            existing.setBookingId(updated.getBookingId());
        if (updated.getAttendeeName() != null)
            existing.setAttendeeName(updated.getAttendeeName());
        if (updated.getTicketCode() != null)
            existing.setTicketCode(updated.getTicketCode());
        if (updated.getStatus() != null)
            existing.setStatus(updated.getStatus());
        if (updated.getIssuedAt() != null)
            existing.setIssuedAt(updated.getIssuedAt());
        if (updated.getMetadata() != null)
            existing.setMetadata(updated.getMetadata());

        Ticket saved = ticketRepository.save(existing);
        cacheEviction.evictEntityAndFeatures("ticket", id, WRITE_INVALIDATION_PATTERNS);
        notifyObservers("TICKET_UPDATED", Map.of("ticketId", saved.getId()));

        TicketStatus newStatus = saved.getStatus();
        if (newStatus != null && newStatus != previousStatus) {
            if (newStatus == TicketStatus.CANCELLED) {
                ticketEventPublisher.publishTicketCancelled(saved.getId(), saved.getBookingId());
            } else {
                ticketEventPublisher.publishTicketStatusChanged(
                        saved.getId(), saved.getBookingId(), newStatus.name());
            }
        }
        return saved;
    }

    public void deleteTicket(Long id) {
        Ticket existing = getTicketById(id);
        Long bookingId = existing.getBookingId();
        ticketRepository.delete(existing);
        cacheEviction.evictEntityAndFeatures("ticket", id, WRITE_INVALIDATION_PATTERNS);
        notifyObservers("TICKET_DELETED", Map.of("ticketId", id));
        ticketEventPublisher.publishTicketCancelled(id, bookingId);
    }

    // ─── [S4-F1] Get Latest Ticket by Booking ────────────────────────────────────

    @Cacheable(cacheNames = "ticket-service::S4-F1", key = "#bookingId")
    public Ticket getLatestTicketByBooking(Long bookingId) {
        // Hot path returns immediately without a second query when a ticket is found.
        return ticketRepository.findTopByBookingIdOrderByIssuedAtDesc(bookingId)
                .orElseThrow(() -> {
                    if (!ticketRepository.existsBookingById(bookingId)) {
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
                    }
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No tickets found for this booking");
                });
    }

    // ─── [S4-F2] Issue Ticket (write) ────────────────────────────────────────────

    public Ticket issueTicket(Long bookingId, String attendeeName, String ticketCode,
                              Map<String, Object> metadata) {
        requireBookingExists(bookingId);
        requireUniqueTicketCode(ticketCode);

        Long eventId = bookingEventContext.lookupEventId(bookingId).orElse(null);
        Ticket ticket = new Ticket(
                bookingId,
                eventId,
                attendeeName,
                ticketCode,
                TicketStatus.VALID,
                LocalDateTime.now(),
                metadata);

        Ticket saved = ticketRepository.save(ticket);
        evictAll("S4-F1", "S4-F9", "S4-F10");
        notifyObservers("TICKET_ISSUED", Map.of("ticketId", saved.getId(), "bookingId", bookingId));
        ticketEventPublisher.publishTicketIssued(
                saved.getId(),
                saved.getBookingId(),
                saved.getAttendeeName(),
                saved.getTicketCode());
        return saved;
    }

    // ─── [S4-F3] Find Nearby Tickets ─────────────────────────────────────────────

    @Cacheable(cacheNames = "ticket-service::S4-F3", key = "#lat + ':' + #lon + ':' + #radiusKm")
    public List<NearbyTicketDTO> findNearbyTickets(double lat, double lon, double radiusKm) {
        if (radiusKm <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "radiusKm must be > 0");
        }
        ObjectArrayDtoAdapter adapter = new ObjectArrayDtoAdapter();
        return ticketRepository.findNearbyTickets(lat, lon, radiusKm).stream()
                .map(adapter::adapt)
                .toList();
    }

    // ─── [S4-F4] Batch Ticket Issuance ───────────────────────────────────────────

    @Transactional
    public int batchIssueTickets(BatchTicketRequestDTO request) {
        Long bookingId = request.getBookingId();
        List<TicketItemDTO> items = request.getTickets();

        requireBookingExists(bookingId);

        Set<String> codes = new HashSet<>();
        for (TicketItemDTO item : items) {
            if (!codes.add(item.getTicketCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate ticket code in batch: " + item.getTicketCode());
            }
        }

        List<String> existing = ticketRepository.findExistingTicketCodes(codes);
        if (!existing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ticket code already exists: " + existing.get(0));
        }

        Long eventId = bookingEventContext.lookupEventId(bookingId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> tickets = items.stream()
                .map(item -> new Ticket(
                        bookingId,
                        eventId,
                        item.getAttendeeName(),
                        item.getTicketCode(),
                        TicketStatus.VALID,
                        now,
                        item.getMetadata()))
                .toList();

        List<Ticket> persisted = ticketRepository.saveAll(tickets);
        evictAll("S4-F1", "S4-F5", "S4-F6", "S4-F9", "S4-F10");
        notifyObservers("TICKETS_BATCH_ISSUED",
                Map.of("bookingId", bookingId, "count", persisted.size()));
        for (Ticket t : persisted) {
            ticketEventPublisher.publishTicketIssued(
                    t.getId(),
                    t.getBookingId(),
                    t.getAttendeeName(),
                    t.getTicketCode());
        }
        return persisted.size();
    }

    // ─── [S4-F7] Purge Expired Tickets ───────────────────────────────────────────
    @Transactional
    public int purgeExpiredTickets(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        int count = ticketRepository.purgeExpiredAndCancelledTickets(cutoff);
        evictAll("S4-F1", "S4-F6", "S4-F9", "S4-F10");
        notifyObservers("TICKETS_PURGED",
                Map.of("olderThanDays", olderThanDays, "purgedCount", count));
        return count;
    }

    // ─── [S4-F8] Event Attendance Summary ────────────────────────────────────────

    @Cacheable(cacheNames = "ticket-service::S4-F8", key = "#eventId")
    public EventAttendanceSummaryDTO getSummary(Long eventId) {
        Map<String, Object> data = ticketRepository.getAttendanceSummaryData(eventId);

        long total = (data == null) ? 0L
                : ((Number) data.getOrDefault("total", 0)).longValue();
        if (total == 0L) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No tickets found for event: " + eventId);
        }

        // Adapter expects "eventId" inside the map; native query doesn't return it.
        data.putIfAbsent("eventId", eventId);
        return new CassandraRowAdapter().adapt(data);
    }

    // ─── [S4-F9] Unused Upcoming Tickets ─────────────────────────────────────────

    @Cacheable(cacheNames = "ticket-service::S4-F9", key = "'all'")
    public List<UnusedTicketDTO> getUnusedUpcomingTickets() {
        PostgresRowAdapter adapter = new PostgresRowAdapter();
        return ticketRepository.findUnusedUpcomingTicketsNative().stream()
                .map(adapter::adapt)
                .toList();
    }

    // ─── [S4-F6] Ticket History (date range report) ──────────────────────────────

    @Cacheable(cacheNames = "ticket-service::S4-F6", key = "T(String).valueOf(#startDate) + ':' + T(String).valueOf(#endDate) + ':' + T(String).valueOf(#status)")
    public List<Ticket> getTicketHistory(LocalDate startDate, LocalDate endDate, String status) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be on or after startDate");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        if (status == null || status.isBlank()) {
            return ticketRepository.findByIssuedAtBetween(start, end);
        }

        String normalized = status.toUpperCase();
        try {
            TicketStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status: " + status + ". Allowed: VALID, USED, EXPIRED, CANCELLED");
        }

        return ticketRepository.findByIssuedAtBetweenAndStatus(start, end, normalized);
    }

    // ─── [S4-F5] Filter Tickets by Metadata (JSONB) ──────────────────────────────

    @Cacheable(cacheNames = "ticket-service::S4-F5", key = "#key + ':' + #operator + ':' + #value")
    public List<Ticket> filterByMetadata(String key, String operator, String value) {
        if (key == null || key.isBlank() || operator == null || value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key, operator, and value are required");
        }

        return switch (operator) {
            case "eq" -> ticketRepository.findByMetadataEquals(key, value);
            case "gt" -> ticketRepository.findByMetadataGreaterThan(key, value);
            case "lt" -> ticketRepository.findByMetadataLessThan(key, value);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid operator: " + operator + ". Allowed: eq, gt, lt");
        };
    }

    // ─── [S4-F10] Ticket Analytics ───────────────────────────────────────────────
    @Cacheable(value = "ticket-service::S4-F10", key = "#startDate.toString() + '_' + #endDate.toString()")
    public TicketAnalyticsDTO getAnalytics(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Ticket> tickets = ticketRepository.findAllByIssuedAtBetween(startDateTime, endDateTime);

        Map<String, Long> ticketsByStatus = tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus().name(),
                        Collectors.counting()));

        long usedCount = ticketsByStatus.getOrDefault("USED", 0L);
        long validCount = ticketsByStatus.getOrDefault("VALID", 0L);
        long expiredCount = ticketsByStatus.getOrDefault("EXPIRED", 0L);
        long cancelledCount = ticketsByStatus.getOrDefault("CANCELLED", 0L);

        long totalIssued = tickets.size();
        double attendanceRate = totalIssued == 0 ? 0.0 : (double) usedCount / totalIssued;

        return TicketAnalyticsDTO.builder()
                .totalIssued(totalIssued)
                .usedCount(usedCount)
                .validCount(validCount)
                .expiredCount(expiredCount)
                .cancelledCount(cancelledCount)
                .attendanceRate(attendanceRate)
                .ticketsByStatus(ticketsByStatus)
                .build();
    }
}
