package com.team22.eventticketing.ticket.service;

import com.team22.eventticketing.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
}