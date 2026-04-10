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
import java.util.Map;

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

        boolean existsByTicketCode(String ticketCode);

        @Query(value = "SELECT COUNT(*) > 0 FROM bookings WHERE id = :bookingId", nativeQuery = true)
        boolean bookingExistsById(@Param("bookingId") Long bookingId);

        /**
         * [S4-F7] Purge Expired Tickets.
         * Deletes tickets older than a specific date if they are EXPIRED or CANCELLED.
         */
        @Modifying
        @Transactional
        @Query(value = "DELETE FROM tickets WHERE issued_at < :cutoff " +
                        "AND status IN ('EXPIRED', 'CANCELLED')", nativeQuery = true)
        int purgeExpiredAndCancelledTickets(@Param("cutoff") LocalDateTime cutoff);

        // [S4-F8] Update Ticket Status on Check-In.
        @Query(value = "SELECT " +
                        "COUNT(*) as total, " +
                        "COUNT(*) FILTER (WHERE t.status = 'USED') as used, " +
                        "COUNT(*) FILTER (WHERE t.status = 'VALID') as _valid, " +
                        "MAX(t.issued_at) FILTER (WHERE t.status = 'USED') as last_checkin " +
                        "FROM tickets t " +
                        "JOIN bookings b ON t.booking_id = b.id " +
                        "WHERE b.event_id = :eventId", nativeQuery = true)
        Map<String, Object> getAttendanceSummaryData(@Param("eventId") Long eventId);

        // [S4-F9] Find Unused Tickets for Upcoming Events.

        @Query(value = "SELECT t.id as ticketId, t.attendee_name as attendeeName, " +
                        "t.ticket_code as ticketCode, t.booking_id as bookingId, " +
                        "e.name as eventName, e.event_date as eventDate " +
                        "FROM tickets t " +
                        "JOIN bookings b ON t.booking_id = b.id " +
                        "JOIN events e ON b.event_id = e.id " +
                        "WHERE t.status = 'VALID' AND e.status = 'UPCOMING'", nativeQuery = true)
        List<Map<String, Object>> findUnusedUpcomingTicketsNative();

}