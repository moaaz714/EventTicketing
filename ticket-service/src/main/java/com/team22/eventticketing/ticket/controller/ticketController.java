package com.team22.eventticketing.ticket.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.dto.BatchTicketRequestDTO;
import com.team22.eventticketing.ticket.model.dto.EventAttendanceSummaryDTO;
import com.team22.eventticketing.ticket.model.dto.UnusedTicketDTO;
import com.team22.eventticketing.ticket.service.TicketService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class ticketController {

    private final TicketService ticketService;

    public ticketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/health")
    public String statusCheck() {
        return "OK";
    }

    // ==================== CRUD ====================

    @PostMapping
    public ResponseEntity<Ticket> createTicket(@RequestBody Ticket ticket) {
        Ticket created = ticketService.createTicket(ticket);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public Ticket getTicketById(@PathVariable Long id) {
        return ticketService.getTicketById(id);
    }

    @GetMapping
    public List<Ticket> getAllTickets() {
        return ticketService.getAllTickets();
    }

    @PutMapping("/{id}")
    public Ticket updateTicket(@PathVariable Long id, @RequestBody Ticket ticket) {
        return ticketService.updateTicket(id, ticket);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.ok().build();
    }

    // ==================== S4-F4: Batch Ticket Issuance ====================

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Integer>> batchIssueTickets(@RequestBody BatchTicketRequestDTO request) {
        int count = ticketService.batchIssueTickets(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("count", count));
    }

    // ==================== S4-F7: Purge Expired Tickets ====================

    @DeleteMapping("/purge")
    public int purgeTickets(@RequestParam("olderThanDays") int olderThanDays) {
        return ticketService.purgeExpiredTickets(olderThanDays);
    }

    // ==================== S4-F8: Event Attendance Summary ====================

    @GetMapping("/event/{eventId}/summary")
    public EventAttendanceSummaryDTO getEventAttendanceSummary(@PathVariable Long eventId) {
        return ticketService.getSummary(eventId);
    }

    // ==================== S4-F9: Unused Upcoming Tickets ====================

    @GetMapping("/unused-upcoming")
    public List<UnusedTicketDTO> getUnusedUpcomingTickets() {
        return ticketService.getUnusedUpcomingTickets();
    }
}
