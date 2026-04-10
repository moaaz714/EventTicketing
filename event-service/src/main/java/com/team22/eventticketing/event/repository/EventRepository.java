package com.team22.eventticketing.event.repository;


import com.team22.eventticketing.event.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query(value = """
            SELECT EXISTS (
                SELECT *
                FROM bookings b
                WHERE b.event_id = :eventId
                  AND b.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')
            )
            """, nativeQuery = true)
    boolean existsActiveBookingsForEvent(@Param("eventId") Long eventId);
    // Check if booking exists at all
    @Query(value = "SELECT COUNT(*) FROM bookings WHERE id = :bookingId", nativeQuery = true)
    long checkBookingExists(@Param("bookingId") Long bookingId);

    // Check if it is completed and matches the event
    @Query(value = "SELECT COUNT(*) FROM bookings b WHERE b.id = :bookingId AND b.event_id = :eventId AND b.status = 'COMPLETED'", nativeQuery = true)
    long countCompletedBookingForEvent(@Param("bookingId") Long bookingId, @Param("eventId") Long eventId);

    //  check if the user is an ADMIN
    @Query(value = "SELECT COUNT(*) FROM users u WHERE u.id = :userId AND u.role = 'ADMIN'", nativeQuery = true)
    long checkUserIsAdmin(@Param("userId") Long userId);

    //  Find events that have at least one unverified session
    @Query("SELECT DISTINCT e FROM Event e JOIN e.eventSessions s WHERE s.verified = false")
    java.util.List<Event> findEventsWithUnverifiedSessions();
}