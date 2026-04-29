package com.team22.eventticketing.ticket.service;

import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketStatus;
import com.team22.eventticketing.ticket.model.dto.BatchTicketRequestDTO;
import com.team22.eventticketing.ticket.model.dto.EventAttendanceSummaryDTO;
import com.team22.eventticketing.ticket.model.dto.NearbyTicketDTO;
import com.team22.eventticketing.ticket.model.dto.TicketItemDTO;
import com.team22.eventticketing.ticket.model.dto.UnusedTicketDTO;
import com.team22.eventticketing.ticket.observer.EntityObserver;
import com.team22.eventticketing.ticket.repository.TicketRepository;

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
    private final List<EntityObserver> observers = new ArrayList<>();

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public void register(EntityObserver observer) { observers.add(observer); }
    public void unregister(EntityObserver observer) { observers.remove(observer); }

    private void notifyObservers(String action, Object payload) {
        for (EntityObserver o : observers) o.onEvent(action, payload);
    }

    // ==================== CRUD ====================

    public Ticket createTicket(Ticket ticket) {
        Ticket saved = ticketRepository.save(ticket);
        notifyObservers("TICKET_CREATED", Map.of("ticketId", saved.getId()));
        return saved;
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found with id: " + id));
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Ticket updateTicket(Long id, Ticket updated) {
        Ticket existing = getTicketById(id);
        existing.setBookingId(updated.getBookingId());
        existing.setAttendeeName(updated.getAttendeeName());
        existing.setTicketCode(updated.getTicketCode());
        existing.setStatus(updated.getStatus());
        existing.setIssuedAt(updated.getIssuedAt());
        existing.setMetadata(updated.getMetadata());
        Ticket saved = ticketRepository.save(existing);
        notifyObservers("TICKET_UPDATED", Map.of("ticketId", saved.getId()));
        return saved;
    }

    public void deleteTicket(Long id) {
        Ticket existing = getTicketById(id);
        ticketRepository.delete(existing);
        notifyObservers("TICKET_DELETED", Map.of("ticketId", id));
    }

    // ==================== S4-F4: Batch Ticket Issuance ====================

    @Transactional
    public int batchIssueTickets(BatchTicketRequestDTO request) {
        Long bookingId = request.getBookingId();
        List<TicketItemDTO> items = request.getTickets();

        if (!ticketRepository.bookingExistsById(bookingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found with id: " + bookingId);
        }

        Set<String> codesInBatch = new HashSet<>();
        for (TicketItemDTO item : items) {
            if (!codesInBatch.add(item.getTicketCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate ticket code in batch: " + item.getTicketCode());
            }
        }

        for (String code : codesInBatch) {
            if (ticketRepository.existsByTicketCode(code)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket code already exists: " + code);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<Ticket> tickets = items.stream().map(item -> new Ticket(
                bookingId,
                item.getAttendeeName(),
                item.getTicketCode(),
                TicketStatus.VALID,
                now,
                item.getMetadata()
        )).toList();

        ticketRepository.saveAll(tickets);
        notifyObservers("TICKETS_BATCH_ISSUED", Map.of("bookingId", bookingId, "count", tickets.size()));
        return tickets.size();
    }

    // Get ticket S4-F1
    public Ticket getLatestTicketByBooking(Long bookingId) {
        boolean bookingExists = ticketRepository.existsBookingById(bookingId);
        if (!bookingExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        return ticketRepository
                .findTopByBookingIdOrderByIssuedAtDesc(bookingId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "No tickets found for this booking"));
    }

    // Create ticket S4-F2
    public Ticket issueTicket(Long bookingId, String attendeeName, String ticketCode, Map<String, Object> metadata) {
        boolean exists = ticketRepository.existsBookingById(bookingId);
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }

        if (ticketRepository.existsByTicketCode(ticketCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket code already exists");
        }

        Ticket ticket = new Ticket();
        ticket.setBookingId(bookingId);
        ticket.setAttendeeName(attendeeName);
        ticket.setTicketCode(ticketCode);
        ticket.setMetadata(metadata);
        ticket.setIssuedAt(LocalDateTime.now());
        ticket.setStatus(TicketStatus.VALID);

        Ticket saved = ticketRepository.save(ticket);
        notifyObservers("TICKET_ISSUED", Map.of("ticketId", saved.getId(), "bookingId", bookingId));
        return saved;
    }

    // Find event according to distance of event S4-F3
    public List<NearbyTicketDTO> findNearbyTickets(double lat, double lon, double radiusKm) {
        List<Object[]> results = ticketRepository.findNearbyTickets(lat, lon, radiusKm);

        return results.stream().map(r ->
                new NearbyTicketDTO(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        ((Number) r[2]).longValue(),
                        (String) r[3],
                        r[4] != null ? ((Number) r[4]).doubleValue() : null,
                        r[5] != null ? ((Number) r[5]).doubleValue() : null,
                        ((Number) r[6]).doubleValue()
                )
        ).collect(Collectors.toList());
    }

    @Transactional
    public int purgeExpiredTickets(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        int count = ticketRepository.purgeExpiredAndCancelledTickets(cutoff);
        notifyObservers("TICKETS_PURGED", Map.of("olderThanDays", olderThanDays, "purgedCount", count));
        return count;
    }

    public EventAttendanceSummaryDTO getSummary(Long eventId) {
        Map<String, Object> data = ticketRepository.getAttendanceSummaryData(eventId);

        long total = ((Number) data.getOrDefault("total", 0)).longValue();

        if (total == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No tickets found for event: " + eventId);
        }

        long used = ((Number) data.getOrDefault("used", 0)).longValue();
        long valid = ((Number) data.getOrDefault("_valid", 0)).longValue();
        LocalDateTime last = (LocalDateTime) data.get("last_checkin");

        return new EventAttendanceSummaryDTO(eventId, total, used, valid, last);
    }

    public List<UnusedTicketDTO> getUnusedUpcomingTickets() {
        List<Map<String, Object>> results = ticketRepository.findUnusedUpcomingTicketsNative();

        return results.stream().map(row -> new UnusedTicketDTO(
                ((Number) row.get("ticketId")).longValue(),
                (String) row.get("attendeeName"),
                (String) row.get("ticketCode"),
                ((Number) row.get("bookingId")).longValue(),
                (String) row.get("eventName"),
                (LocalDateTime) row.get("eventDate"))).toList();
    }

    // ==================== S4-F6: Get Tickets in Date Range ====================

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

    // ==================== S4-F5: Filter Tickets by Metadata ====================

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
}
