package com.team22.eventticketing.ticket.controller;

import com.team22.eventticketing.ticket.model.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.dto.BatchTicketRequestDTO;
import com.team22.eventticketing.ticket.model.dto.NearbyTicketDTO;
import com.team22.eventticketing.ticket.model.dto.TicketAnalyticsDTO;
import com.team22.eventticketing.ticket.model.dto.TicketScanDTO;
import com.team22.eventticketing.ticket.model.dto.TicketScanRequestDTO;
import com.team22.eventticketing.ticket.service.TicketScanService;
import com.team22.eventticketing.ticket.model.dto.TicketScanRequestDTO;
import com.team22.eventticketing.ticket.service.TicketScanService;


import org.springframework.web.bind.annotation.*;
import com.team22.eventticketing.ticket.service.TicketService;
import com.team22.eventticketing.ticket.observer.MongoEventLogger;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;


@RestController
@RequestMapping("/api/tickets")
public class ticketController {

    private final TicketService ticketService;
    private final MongoEventLogger eventLogger;
    private final TicketScanService ticketScanService;
    public ticketController(TicketService ticketService, MongoEventLogger eventLogger, TicketScanService ticketScanService) {
        this.ticketService = ticketService;
        this.eventLogger = eventLogger;
        this.ticketScanService = ticketScanService;
    }

    // [S4-F1]
    @GetMapping("/booking/{bookingId}/latest")
    public Ticket getLatestTicket(@PathVariable Long bookingId) {
        return ticketService.getLatestTicketByBooking(bookingId);
    }

    @PostMapping("/booking/{bookingId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Ticket issueTicket(
            @PathVariable Long bookingId,
            @RequestBody Map<String, Object> request) {

        String attendeeName = (String) request.get("attendeeName");
        String ticketCode = (String) request.get("ticketCode");
        Map<String, Object> metadata = (Map<String, Object>) request.get("metadata");

        return ticketService.issueTicket(
                bookingId,
                attendeeName,
                ticketCode,
                metadata);
    }

    @GetMapping("/nearby")
    public List<NearbyTicketDTO> getNearbyTickets(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam double radiusKm) {
        return ticketService.findNearbyTickets(lat, lon, radiusKm);
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
    public Map<String, Object> purgeTickets(@RequestParam("olderThanDays") int olderThanDays) {

        int count = ticketService.purgeExpiredTickets(olderThanDays);

        Map<String, Object> response = new HashMap<>();
        response.put("deletedCount", count);
        return response;
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

    // ==================== S4-F5: Filter Tickets by Metadata ====================

    @GetMapping("/metadata/search")
    public List<Ticket> filterTicketsByMetadata(
            @RequestParam String key,
            @RequestParam String operator,
            @RequestParam String value) {
        return ticketService.filterByMetadata(key, operator, value);
    }

    // ==================== S4-F6: Get Tickets in Date Range ====================

    @GetMapping("/history")
    public List<Ticket> getTicketHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status) {
        return ticketService.getTicketHistory(startDate, endDate, status);
    }

    // S4-F10
    @GetMapping("/analytics")
    public ResponseEntity<TicketAnalyticsDTO> getTicketAnalyticsDashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // STEP 4: Trigger the Observer Pattern for Observability logging.
        // Doing this HERE ensures it runs on every invocation, bypassing the Redis
        // cache[cite: 1].
        Map<String, Object> payload = new HashMap<>();
        payload.put("startDate", startDate);
        payload.put("endDate", endDate);

        // Log the purely observability action to MongoDB via the Observer chain[cite:
        // 1]
        eventLogger.onEvent("ANALYTICS_VIEWED", payload);

        // STEP 5: Call the cached Service method and return a 200 OK[cite: 1]
        TicketAnalyticsDTO response = ticketService.getAnalytics(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    // S4-F11
    @PostMapping("/{id}/scan")
    public ResponseEntity<?> scanTicket(
        @PathVariable Long id,
        @RequestHeader("Authorization") String authHeader,
        @RequestBody TicketScanRequestDTO request) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");
        ticketScanService.recordScan(id, authHeader.substring(7), request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Scan recorded");
    }

    // S4-F12
    @GetMapping("/{id}/scans")
    public ResponseEntity<List<TicketScanDTO>> getScanHistory(
        @PathVariable Long id,
        @RequestParam(required = false) Instant startTime,
        @RequestParam(required = false) Instant endTime) {
            return ResponseEntity.ok(ticketScanService.getScanHistory(id, startTime, endTime));
        }
}
