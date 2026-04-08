package com.team22.eventticketing.event.repository;


import com.team22.eventticketing.event.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}