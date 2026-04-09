package com.team22.eventticketing.ticket.controller;

import com.team22.eventticketing.ticket.model.Ticket;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import com.team22.eventticketing.ticket.model.dto.EventAttendanceSummaryDTO;
import com.team22.eventticketing.ticket.model.dto.UnusedTicketDTO;
import com.team22.eventticketing.ticket.service.TicketService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets") // Requirement 4.a [cite: 98]
public class ticketController {

    private final TicketService ticketService;

    public ticketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    //[S4-F1]
    @GetMapping("/booking/{bookingId}/latest")
    public Ticket getLatestTicket(@PathVariable Long bookingId) {
        return ticketService.getLatestTicketByBooking(bookingId);
    }
    @PostMapping("/booking/{bookingId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Ticket issueTicket(
            @PathVariable Long bookingId,
            @RequestBody Map<String, Object> request
    ) {

        String attendeeName = (String) request.get("attendeeName");
        String ticketCode = (String) request.get("ticketCode");
        Map<String, Object> metadata = (Map<String, Object>) request.get("metadata");

        return ticketService.issueTicket(
                bookingId,
                attendeeName,
                ticketCode,
                metadata
        );
    }


    @GetMapping("/health")
    public String statusCheck() {
        return "OK";
    }

    // [S4-F7] Purge Expired Tickets
    @DeleteMapping("/purge")
    public int purgeTickets(@RequestParam("olderThanDays") int olderThanDays) {
        // Call the service layer to handle business logic and DB operation
        int deletedCount = ticketService.purgeExpiredTickets(olderThanDays);

        // Return the count with a 200 OK status as per requirements
        return deletedCount;
    }

    // [S4-F8] Event Attendance Summary

    @GetMapping("/event/{eventId}/summary")
    public EventAttendanceSummaryDTO getEventAttendanceSummary(@PathVariable Long eventId) {
        // Delegate to service layer
        EventAttendanceSummaryDTO summary = ticketService.getSummary(eventId);

        // Return 200 OK with the DTO body
        return summary;
    }

    @GetMapping("/unused-upcoming")
    public List<UnusedTicketDTO> getUnusedUpcomingTickets() {
        return ticketService.getUnusedUpcomingTickets();
    }
}
