package com.team22.eventticketing.event.repository;
import com.team22.eventticketing.event.dto.TopEventDTO;


import com.team22.eventticketing.event.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.List;

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