package com.team22.eventticketing.event.repository;


import com.team22.eventticketing.event.model.Event;
import com.team22.eventticketing.event.model.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

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


}