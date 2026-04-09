package com.team22.eventticketing.ticket.controller;

import org.springframework.web.bind.annotation.*;

import com.team22.eventticketing.ticket.service.TicketService;

@RestController
@RequestMapping("/api/tickets") // Requirement 4.a [cite: 98]
public class ticketController {

    private final TicketService ticketService;

    public ticketController(TicketService ticketService) {
        this.ticketService = ticketService;
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
}
