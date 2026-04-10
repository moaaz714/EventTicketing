package com.team22.eventticketing.booking.repository;

import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Method 1: When status is provided
    List<Booking> findByStatusAndBookingDateBetweenOrderByBookingDateDesc(
            BookingStatus status, LocalDateTime startDate, LocalDateTime endDate);

    // Method 2: When status is NOT provided (fallback) [cite: 574, 580]
    List<Booking> findByBookingDateBetweenOrderByBookingDateDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    // Add this to your existing BookingRepository interface
    @org.springframework.data.jpa.repository.Query(value = "SELECT status FROM events WHERE id = :eventId", nativeQuery = true)
    String findEventStatusById(@org.springframework.data.repository.query.Param("eventId") Long eventId);


}