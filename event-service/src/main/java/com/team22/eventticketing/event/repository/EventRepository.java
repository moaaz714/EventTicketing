package com.team22.eventticketing.event.repository;
import com.team22.eventticketing.event.dto.TopEventDTO;



import com.team22.eventticketing.event.model.Event;
import com.team22.eventticketing.event.model.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import java.util.List;

import java.util.List;

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


    @Query(value = """
            SELECT * FROM events e
            WHERE e.details ->> :key = :value
            """, nativeQuery = true)
    List<Event> findByDetailAttribute(
            @Param("key") String key,
            @Param("value") String value
    );
    @Query(value = """
        SELECT * FROM events e
        WHERE e.details ->> :key = :value
          AND e.status = :status
        """, nativeQuery = true)
    List<Event> findByDetailAttributeAndStatus(
            @Param("key") String key,
            @Param("value") String value,
            @Param("status") String status
    );
    // Check if booking exists at all
    @Query(value = "SELECT COUNT(*) FROM bookings WHERE id = :bookingId", nativeQuery = true)
    long checkBookingExists(@Param("bookingId") Long bookingId);

    // Check if it is completed and matches the event
    @Query(value = "SELECT COUNT(*) FROM bookings b WHERE b.id = :bookingId AND b.event_id = :eventId AND b.status = 'COMPLETED'", nativeQuery = true)
    long countCompletedBookingForEvent(@Param("bookingId") Long bookingId, @Param("eventId") Long eventId);

    //  check if the user is an ADMIN
    @Query(value = "SELECT COUNT(*) FROM users u WHERE u.id = :userId AND u.role = 'ADMIN'", nativeQuery = true)
    long checkUserIsAdmin(@Param("userId") Long userId);

    // Dynamic search for events with optional filters
//    @Query("""
//        SELECT e FROM Event e
//        WHERE (:category IS NULL OR e.category = :category)
//        AND (:startDate IS NULL OR e.eventDate >= :startDate)
//        AND (:endDate IS NULL OR e.eventDate <= :endDate)
//        ORDER BY e.eventDate ASC
//        """)
//    List<Event> searchEvents(
//            @Param("category") EventCategory category,
//            @Param("startDate") LocalDateTime startDate,
//            @Param("endDate") LocalDateTime endDate
//    );

    List<Event> findByCategoryOrderByEventDateAsc(EventCategory category);

    List<Event> findByCategoryAndEventDateBetweenOrderByEventDateAsc(
            EventCategory category,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<Event> findByEventDateBetweenOrderByEventDateAsc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<Event> findAllByOrderByEventDateAsc();
    List<Event> findByCategoryAndEventDateAfterOrderByEventDateAsc(
            EventCategory category, LocalDateTime startDate);

    List<Event> findByCategoryAndEventDateBeforeOrderByEventDateAsc(
            EventCategory category, LocalDateTime endDate);

    List<Event> findByEventDateAfterOrderByEventDateAsc(LocalDateTime startDate);

    List<Event> findByEventDateBeforeOrderByEventDateAsc(LocalDateTime endDate);

    //  Find events that have at least one unverified session
    @Query("SELECT DISTINCT e FROM Event e JOIN e.eventSessions s WHERE s.verified = false")
    java.util.List<Event> findEventsWithUnverifiedSessions();
    @Query(value = """
        SELECT 
            e.id AS eventId,
            e.name AS name,
            e.rating AS rating,
            COUNT(b.id) AS totalBookings
        FROM events e
        LEFT JOIN bookings b ON b.event_id = e.id
        GROUP BY e.id, e.name, e.rating
        ORDER BY e.rating DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopRatedEvents(@Param("limit") int limit);
}