package com.team22.eventticketing.ticket.repository;

import com.team22.eventticketing.ticket.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findById(Long id);

    /**
     * Find all tickets associated with a specific booking.
     */
    List<Ticket> findByBookingId(Long bookingId);

    /**
     * Find a ticket by its unique generated code.
     */
    Optional<Ticket> findByTicketCode(String ticketCode);

    /**
     * [S4-F7] Purge Expired Tickets[cite: 761, 765].
     * Deletes tickets older than a specific date if they are EXPIRED or CANCELLED.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM tickets WHERE issued_at < :cutoff " +
            "AND status IN ('EXPIRED', 'CANCELLED')", nativeQuery = true)
    int purgeExpiredAndCancelledTickets(@Param("cutoff") LocalDateTime cutoff);

    /**
     * [S4-F9] Find Unused Tickets for Upcoming Events[cite: 782, 786].
     * Demonstrates cross-service access by joining tickets with the bookings
     * and events tables using native SQL[cite: 337, 338, 340].
     */
    @Query(value = "SELECT t.* FROM tickets t " +
            "JOIN bookings b ON t.booking_id = b.id " +
            "JOIN events e ON b.event_id = e.id " +
            "WHERE t.status = 'VALID' AND e.status = 'UPCOMING'", nativeQuery = true)
    List<Ticket> findUnusedTicketsForUpcomingEvents();

    // --- @Modifying + @Transactional for updates/deletes ---

}