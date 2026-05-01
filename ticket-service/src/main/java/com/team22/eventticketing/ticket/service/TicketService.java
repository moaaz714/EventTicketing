package com.team22.eventticketing.ticket.service;

import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketStatus;
import com.team22.eventticketing.ticket.model.dto.BatchTicketRequestDTO;
import com.team22.eventticketing.ticket.model.dto.EventAttendanceSummaryDTO;
import com.team22.eventticketing.ticket.model.dto.TicketItemDTO;
import com.team22.eventticketing.ticket.model.dto.UnusedTicketDTO;
import com.team22.eventticketing.ticket.repository.TicketRepository;
import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketStatus;
import com.team22.eventticketing.ticket.model.dto.NearbyTicketDTO;





import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    // ==================== CRUD ====================

    public Ticket createTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
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
        return ticketRepository.save(existing);
    }

    public void deleteTicket(Long id) {
        Ticket existing = getTicketById(id);
        ticketRepository.delete(existing);
    }

    // ==================== S4-F4: Batch Ticket Issuance ====================

    @Transactional
    public int batchIssueTickets(BatchTicketRequestDTO request) {
        Long bookingId = request.getBookingId();
        List<TicketItemDTO> items = request.getTickets();

        // 1. Verify booking exists
        if (!ticketRepository.bookingExistsById(bookingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found with id: " + bookingId);
        }

        // 2. Check for duplicate ticket codes within the batch
        Set<String> codesInBatch = new HashSet<>();
        for (TicketItemDTO item : items) {
            if (!codesInBatch.add(item.getTicketCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate ticket code in batch: " + item.getTicketCode());
            }
        }

        // 3. Check for duplicate ticket codes against the database
        for (String code : codesInBatch) {
            if (ticketRepository.existsByTicketCode(code)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket code already exists: " + code);
            }
        }

        // 4. Build and save all tickets
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
        return tickets.size();
    }

    //Get ticket S4-F1
    public Ticket getLatestTicketByBooking(Long bookingId) {

        //Check if booking exists using SQL
        boolean bookingExists = ticketRepository.existsBookingById(bookingId);

        if (!bookingExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }

        //Get the latest ticket
        return ticketRepository
                .findTopByBookingIdOrderByIssuedAtDesc(bookingId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "No tickets found for this booking"));
    }

    //Create ticket S4-F2
    public Ticket issueTicket(Long bookingId,
                              String attendeeName,
                              String ticketCode,
                              Map<String, Object> metadata) {

        boolean exists = ticketRepository.existsBookingById(bookingId);

        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }

        if (ticketRepository.existsByTicketCode(ticketCode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket code already exists"
            );
        }

        Ticket ticket = new Ticket();
        ticket.setBookingId(bookingId);
        ticket.setAttendeeName(attendeeName);
        ticket.setTicketCode(ticketCode);
        ticket.setMetadata(metadata);
        ticket.setIssuedAt(LocalDateTime.now());
        ticket.setStatus(TicketStatus.VALID);

        return ticketRepository.save(ticket);
    }

    //Find event according to distance of event S4-F3
    public List<NearbyTicketDTO> findNearbyTickets(double lat, double lon, double radiusKm) {

        List<Object[]> results = ticketRepository.findNearbyTickets(lat, lon, radiusKm);

        return results.stream().map(r ->
                 NearbyTicketDTO.builder()
                         .ticketId(((Number) r[0]).longValue())
                         .attendeeName((String) r[1])
                         .bookingId(((Number) r[2]).longValue())
                         .eventName((String) r[3])
                         .eventLat(r[4] != null ? ((Number) r[4]).doubleValue() : null)
                         .eventLon(r[5] != null ? ((Number) r[5]).doubleValue() : null)
                         .distanceKm(((Number) r[6]).doubleValue())
                         .build()
        ).collect(Collectors.toList());
    }



    @Transactional
    public int purgeExpiredTickets(int olderThanDays) {
        // Calculate cutoff: today minus n days.
        // Example: If olderThanDays is 30, cutoff is 30 days ago.
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);

        // Execute deletion of EXPIRED and CANCELLED tickets via repository.
        return ticketRepository.purgeExpiredAndCancelledTickets(cutoff);
    }

    public EventAttendanceSummaryDTO getSummary(Long eventId) {
        Map<String, Object> data = ticketRepository.getAttendanceSummaryData(eventId);

        long total = ((Number) data.getOrDefault("total", 0)).longValue();

        // Throw 404 directly using Spring's built-in exception
        if (total == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No tickets found for event: " + eventId);
        }

        // Handling potential nulls if no tickets exist for the event
        long used = ((Number) data.getOrDefault("used", 0)).longValue();
        long valid = ((Number) data.getOrDefault("_valid", 0)).longValue();
        LocalDateTime last = (LocalDateTime) data.get("last_checkin");

        return EventAttendanceSummaryDTO.builder()
                .eventId(eventId)
                .totalTickets(total)
                .usedTickets(used)
                .validTickets(valid)
                .lastCheckIn(last)
                .build();
    }

    public List<UnusedTicketDTO> getUnusedUpcomingTickets() {
        List<Map<String, Object>> results = ticketRepository.findUnusedUpcomingTicketsNative();

        return results.stream().map(row -> UnusedTicketDTO.builder()
                        .ticketId(((Number) row.get("ticketId")).longValue())
                        .attendeeName((String) row.get("attendeeName"))
                        .ticketCode((String) row.get("ticketCode"))
                        .bookingId(((Number) row.get("bookingId")).longValue())
                        .eventName((String) row.get("eventName"))
                        .eventDate((LocalDateTime) row.get("eventDate")).build()).toList();
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