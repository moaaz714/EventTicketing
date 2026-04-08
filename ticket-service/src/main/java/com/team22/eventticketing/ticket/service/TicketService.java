package com.team22.eventticketing.ticket.service;

import com.team22.eventticketing.ticket.model.dto.EventAttendanceSummaryDTO;
import com.team22.eventticketing.ticket.repository.TicketRepository;
import com.team22.eventticketing.ticket.model.dto.UnusedTicketDTO;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
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
                ((java.sql.Timestamp) row.get("eventDate")).toLocalDateTime())).toList();
    }

}