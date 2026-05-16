package com.team22.eventticketing.ticket.service;

import com.datastax.oss.driver.api.core.cql.Row;
import com.team22.eventticketing.ticket.adapter.CassandraRowAdapter;
import com.team22.eventticketing.ticket.adapter.ObjectArrayDtoAdapter;
import com.team22.eventticketing.ticket.adapter.PostgresRowAdapter;

import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketScanEvent;
import com.team22.eventticketing.ticket.model.TicketStatus;
import com.team22.eventticketing.ticket.model.dto.BatchTicketRequestDTO;
import com.team22.eventticketing.ticket.model.dto.EventAttendanceSummaryDTO;
import com.team22.eventticketing.ticket.model.dto.NearbyTicketDTO;
import com.team22.eventticketing.ticket.model.dto.TicketAnalyticsDTO;
import com.team22.eventticketing.ticket.model.dto.TicketItemDTO;
import com.team22.eventticketing.ticket.model.dto.UnusedTicketDTO;
import com.team22.eventticketing.ticket.observer.EntityObserver;
import com.team22.eventticketing.ticket.repository.TicketRepository;

import dto.EventDTO;
import feign.BookingServiceClient;
import feign.EventServiceClient;
import dto.VenueCoordsDTO;
import dto.BookingDTO;


import java.util.HashMap;
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

    private final TicketRepository ticketRepository;
    private final CacheEvictionService cacheEviction;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final BookingServiceClient bookingServiceClient;
    private final EventServiceClient eventServiceClient;

    public TicketService(TicketRepository ticketRepository, CacheEvictionService cacheEviction, BookingServiceClient bookingServiceClient, EventServiceClient eventServiceClient) {
        this.ticketRepository = ticketRepository;
        this.cacheEviction = cacheEviction;
        this.bookingServiceClient = bookingServiceClient;
        this.eventServiceClient = eventServiceClient;
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

    // ─── CRUD
    // ─────────────────────────────────────────────────────────────────────
    @CacheEvict(value = "ticket-service::S4-F10", allEntries = true)
    public Ticket createTicket(Ticket ticket) {
        Ticket saved = ticketRepository.save(ticket);
        notifyObservers("TICKET_CREATED", Map.of("ticketId", saved.getId()));
        return saved;
    }

    @Cacheable(cacheNames = "ticket-service::ticket", key = "#id")
    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found with id: " + id));
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll(); // list — NOT cached
    }

    @CacheEvict(value = "ticket-service::S4-F10", allEntries = true)
    public Ticket updateTicket(Long id, Ticket updated) {
        Ticket existing = getTicketById(id);

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
        cacheEviction.evictEntityAndFeatures("ticket", id, "S4-F1", "S4-F5", "S4-F6", "S4-F8", "S4-F9", "S4-F10");
        notifyObservers("TICKET_UPDATED", Map.of("ticketId", saved.getId()));
        return saved;
    }

    public void deleteTicket(Long id) {
        Ticket existing = getTicketById(id);
        ticketRepository.delete(existing);
        cacheEviction.evictEntityAndFeatures("ticket", id, "S4-F1", "S4-F5", "S4-F6", "S4-F8", "S4-F9", "S4-F10");
        notifyObservers("TICKET_DELETED", Map.of("ticketId", id));
    }

    // ─── [S4-F4] Batch Ticket Issuance (write — invalidates) ─────────────────────
    @Transactional
    public int batchIssueTickets(BatchTicketRequestDTO request) {

        Long bookingId = request.getBookingId();

        List<TicketItemDTO> items = request.getTickets();

        // validate booking + retrieve booking data
        BookingDTO booking;

        try {

            booking = bookingServiceClient.getBooking(bookingId);

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Booking not found with id: " + bookingId
            );
        }

        // NEW M3 FIELD
        Long eventId = booking.eventId();

        // duplicate ticket codes inside batch
        Set<String> codesInBatch = new HashSet<>();

        for (TicketItemDTO item : items) {

            if (!codesInBatch.add(item.getTicketCode())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate ticket code in batch: "
                                + item.getTicketCode()
                );
            }
        }

        // duplicate ticket codes in DB
        for (String code : codesInBatch) {

            if (ticketRepository.existsByTicketCode(code)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Ticket code already exists: " + code
                );
            }
        }

        LocalDateTime now = LocalDateTime.now();

        List<Ticket> tickets = items.stream().map(item -> {

            Ticket ticket = new Ticket();

            ticket.setBookingId(bookingId);

            // NEW M3 FIELD
            ticket.setEventId(eventId);

            ticket.setAttendeeName(item.getAttendeeName());

            ticket.setTicketCode(item.getTicketCode());

            ticket.setStatus(TicketStatus.VALID);

            ticket.setIssuedAt(now);

            ticket.setMetadata(item.getMetadata());

            return ticket;

        }).toList();

        ticketRepository.saveAll(tickets);

        cacheEviction.evictPattern("S4-F1");
        cacheEviction.evictPattern("S4-F5");
        cacheEviction.evictPattern("S4-F6");
        cacheEviction.evictPattern("S4-F9");
        cacheEviction.evictPattern("S4-F10");

        notifyObservers(
                "TICKETS_BATCH_ISSUED",
                Map.of(
                        "bookingId", bookingId,
                        "count", tickets.size()
                )
        );

        return tickets.size();
    }



    // ─── [S4-F1] Get Latest Ticket by Booking ────────────────────────────────────

    @Cacheable(cacheNames = "ticket-service::S4-F1", key = "#bookingId")
    public Ticket getLatestTicketByBooking(Long bookingId) {

        try {
            bookingServiceClient.getBooking(bookingId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }

        return ticketRepository
                .findTopByBookingIdOrderByIssuedAtDesc(bookingId)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No tickets found for this booking"));
    }

    // ─── [S4-F2] Issue Ticket (write) ────────────────────────────────────────────

    public Ticket issueTicket(
            Long bookingId,
            String attendeeName,
            String ticketCode,
            Map<String, Object> metadata
    ) {

        // validate booking + retrieve booking data
        BookingDTO booking;

        try {

            booking = bookingServiceClient.getBooking(bookingId);

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Booking not found"
            );
        }

        // duplicate ticket code check
        if (ticketRepository.existsByTicketCode(ticketCode)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket code already exists"
            );
        }

        // create ticket
        Ticket ticket = new Ticket();

        ticket.setBookingId(bookingId);

        // NEW M3 FIELD
        ticket.setEventId(booking.eventId());

        ticket.setAttendeeName(attendeeName);
        ticket.setTicketCode(ticketCode);

        ticket.setMetadata(metadata);

        ticket.setIssuedAt(LocalDateTime.now());

        ticket.setStatus(TicketStatus.VALID);

        Ticket saved = ticketRepository.save(ticket);

        cacheEviction.evictPattern("S4-F1");
        cacheEviction.evictPattern("S4-F9");
        cacheEviction.evictPattern("S4-F10");

        notifyObservers(
                "TICKET_ISSUED",
                Map.of(
                        "ticketId", saved.getId(),
                        "bookingId", bookingId
                )
        );

        return saved;
    }



    // ─── [S4-F3] Find Nearby Tickets (location DTO)
    // ───────────────────────────────

//    @Cacheable(cacheNames = "ticket-service::S4-F3", key = "#lat + ':' + #lon + ':' + #radiusKm")
//    public List<NearbyTicketDTO> findNearbyTickets(double lat, double lon, double radiusKm) {
//
//        List<Object[]> results = ticketRepository.findNearbyTickets(lat, lon, radiusKm);
//
//        ObjectArrayDtoAdapter adapter = new ObjectArrayDtoAdapter();
//
//        return results.stream()
//                .map(adapter::adapt)
//                .collect(Collectors.toList());
//    }
    @Cacheable(
            cacheNames = "ticket-service::S4-F3",
            key = "#lat + ':' + #lon + ':' + #radiusKm"
    )
    public List<NearbyTicketDTO> findNearbyTickets(
            double lat,
            double lon,
            double radiusKm
    ) {

        // local postgres only
        List<Ticket> tickets =
                ticketRepository.findByStatus(TicketStatus.VALID);

        List<NearbyTicketDTO> results = new ArrayList<>();


        // request-scoped cache
        Map<Long, VenueCoordsDTO> venueCache = new HashMap<>();

        for (Ticket ticket : tickets) {

            try {

                // booking-service
                BookingDTO booking =
                        bookingServiceClient.getBooking(ticket.getBookingId());


                Long eventId = booking.eventId();
                EventDTO event = eventServiceClient.getEvent(eventId);

                VenueCoordsDTO venue;

                // avoid repeated feign calls
                if (venueCache.containsKey(eventId)) {

                    venue = venueCache.get(eventId);

                } else {

                    // event-service
                    venue =
                            eventServiceClient.getEventVenueCoords(eventId);


                    venueCache.put(eventId, venue);
                }

                double eventLat = venue.venueLat();
                double eventLon = venue.venueLon();

                // Euclidean distance × 111
                double distanceKm =
                        Math.sqrt(
                                Math.pow(eventLat - lat, 2) +
                                        Math.pow(eventLon - lon, 2)
                        ) * 111;

                if (distanceKm <= radiusKm) {

                    NearbyTicketDTO dto = NearbyTicketDTO.builder()
                            .ticketId(ticket.getId())
                            .attendeeName(ticket.getAttendeeName())
                            .bookingId(ticket.getBookingId())
                            .eventName(event.name())
                            .eventLat(eventLat)
                            .eventLon(eventLon)
                            .distanceKm(distanceKm)
                            .build();


                    results.add(dto);
                }

            } catch (Exception ignored) {

            }
        }

        results.sort((a, b) ->
                Double.compare(a.getDistanceKm(), b.getDistanceKm()));

        return results;
    }

    // ─── [S4-F7] Purge Expired Tickets (write — invalidates) ─────────────────────
    @Transactional
    public int purgeExpiredTickets(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        int count = ticketRepository.purgeExpiredAndCancelledTickets(cutoff);
        cacheEviction.evictPattern("S4-F1");
        cacheEviction.evictPattern("S4-F6");
        cacheEviction.evictPattern("S4-F9");
        cacheEviction.evictPattern("S4-F10");
        notifyObservers("TICKETS_PURGED", Map.of("olderThanDays", olderThanDays, "purgedCount", count));
        return count;
    }

    // ─── [S4-F8] Event Attendance Summary (relationship DTO) ─────────────────────

    @Cacheable(cacheNames = "ticket-service::S4-F8", key = "#eventId")
    public EventAttendanceSummaryDTO getSummary(Long eventId) {

        List<Ticket> tickets =
                ticketRepository.findByEventId(eventId);

        if (tickets.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No tickets found for event: " + eventId
            );
        }

        long totalTickets = tickets.size();

        long usedTickets = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.USED)
                .count();

        long validTickets = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.VALID)
                .count();

        double attendanceRate =
                totalTickets == 0
                        ? 0.0
                        : ((double) usedTickets / totalTickets) * 100;

        LocalDateTime lastCheckIn = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.USED)
                .map(Ticket::getIssuedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return EventAttendanceSummaryDTO.builder()
                .eventId(eventId)
                .totalTickets(totalTickets)
                .usedTickets(usedTickets)
                .validTickets(validTickets)
                .lastCheckIn(lastCheckIn)
                .build();
    }

    // ─── [S4-F9] Unused Upcoming Tickets (combined)
    // ───────────────────────────────

//    @Cacheable(cacheNames = "ticket-service::S4-F9", key = "'all'")
//    public List<UnusedTicketDTO> getUnusedUpcomingTickets() {
//
//        // Fetch the raw data from PostgreSQL
//        List<Map<String, Object>> results = ticketRepository.findUnusedUpcomingTicketsNative();
//
//        // Instantiate the correct structural adapter
//        PostgresRowAdapter adapter = new PostgresRowAdapter();
//
//        // Stream and adapt the results
//        return results.stream()
//                .map(adapter::adapt)
//                .toList();
//    }
    @Cacheable(cacheNames = "ticket-service::S4-F9", key = "'all'")
    public List<UnusedTicketDTO> getUnusedUpcomingTickets() {

        List<Ticket> tickets =
                ticketRepository.findByStatus(TicketStatus.VALID);

        List<UnusedTicketDTO> results = new ArrayList<>();

        Map<Long, EventDTO> eventCache = new HashMap<>();

        for (Ticket ticket : tickets) {

            try {

                Long eventId = ticket.getEventId();

                EventDTO event;

                if (eventCache.containsKey(eventId)) {

                    event = eventCache.get(eventId);

                } else {

                    event = eventServiceClient.getEvent(eventId);

                    eventCache.put(eventId, event);
                }

                if ("UPCOMING".equalsIgnoreCase(event.status())) {

                    UnusedTicketDTO dto = UnusedTicketDTO.builder()
                            .ticketId(ticket.getId())
                            .attendeeName(ticket.getAttendeeName())
                            .ticketCode(ticket.getTicketCode())
                            .bookingId(ticket.getBookingId())
                            .eventName(event.name())
                            .eventDate(event.eventDate())
                            .build();

                    results.add(dto);
                }

            } catch (Exception ignored) {

            }
        }

        return results;
    }

    // ─── [S4-F6] Ticket History (date range report)
    // ───────────────────────────────

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

    // ─── [S4-F5] Filter Tickets by Metadata (JSONB)
    // ───────────────────────────────

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

    // ─── [S4-F10] Ticket Analytics (aggregated report)
    // Caches the result in Redis. The key combines startDate and endDate.
    // Note: TTL configuration (10 mins) is typically handled in your
    // RedisCacheManager config.
    @Cacheable(value = "ticket-service::S4-F10", key = "#startDate.toString() + '_' + #endDate.toString()")
    public TicketAnalyticsDTO getAnalytics(LocalDate startDate, LocalDate endDate) {

        // 1. Validate date range - throws 400 if startDate is after endDate[cite: 1]
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }

        // 2. Expand window to fully-closed range [startDateT00:00:00,
        // endDateT23:59:59.999][cite: 1]
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 3. Fetch data
        List<Ticket> tickets = ticketRepository.findAllByIssuedAtBetween(startDateTime, endDateTime);

        // 4. Aggregate ticket counts by status[cite: 1]
        Map<String, Long> ticketsByStatus = tickets.stream()
                .collect(Collectors.groupingBy(
                        ticket -> ticket.getStatus().name(),
                        Collectors.counting()));

        // Extract specific counts (default to 0 if not present)[cite: 1]
        long usedCount = ticketsByStatus.getOrDefault("USED", 0L);
        long validCount = ticketsByStatus.getOrDefault("VALID", 0L);
        long expiredCount = ticketsByStatus.getOrDefault("EXPIRED", 0L);
        long cancelledCount = ticketsByStatus.getOrDefault("CANCELLED", 0L);

        // 5. Calculate totals and attendance rate[cite: 1]
        long totalIssued = tickets.size();
        double attendanceRate = totalIssued == 0 ? 0.0 : (double) usedCount / totalIssued;

        // 6. Build and return DTO using the Builder Pattern[cite: 1]
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
