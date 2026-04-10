package com.team22.eventticketing.booking.repository;

import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
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

    // S3-F3: Get Average Session Capacity for an Event
    @org.springframework.data.jpa.repository.Query(value = "SELECT AVG(capacity) FROM event_sessions WHERE event_id = :eventId", nativeQuery = true)
    Double getAverageSessionCapacity(@org.springframework.data.repository.query.Param("eventId") Long eventId);

    // S3-F3: Get Active Bookings Count for Demand Multiplier
    @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM bookings WHERE event_id = :eventId AND status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')", nativeQuery = true)
    Long countActiveBookingsForEvent(@org.springframework.data.repository.query.Param("eventId") Long eventId);

    List<Booking> findByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, BookingStatus status);
    // S3-F4: load booking with items for calculation
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.bookingItems WHERE b.id = :id")
    Optional<Booking> findByIdWithItems(@Param("id") Long id);

    // S3-F4: insert a PENDING ticket sale into shared ticket_sales table
    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO ticket_sales (booking_id, user_id, amount, method, status, created_at, transaction_details)
    VALUES (:bookingId, :userId, :amount, 'CREDIT_CARD', 'PENDING', NOW(), '{}')
    """, nativeQuery = true)
    void insertTicketSale(@Param("bookingId") Long bookingId,
                          @Param("userId") Long userId,
                          @Param("amount") Double amount);
    // S3-F5: filter bookings by JSONB metadata key-value pair
    @Query(value = "SELECT * FROM bookings WHERE metadata->>:key = :value",
            nativeQuery = true)
    List<Booking> findByMetadataField(@Param("key") String key,
                                      @Param("value") String value);
    // S3-F6: count all bookings in date range
    @Query(value = """
    SELECT COUNT(*) FROM bookings
    WHERE booking_date BETWEEN :start AND :end
    """, nativeQuery = true)
    long countInRange(@Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    // S3-F6: count completed bookings in date range
    @Query(value = """
    SELECT COUNT(*) FROM bookings
    WHERE booking_date BETWEEN :start AND :end AND status = 'COMPLETED'
    """, nativeQuery = true)
    long countCompletedInRange(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    // S3-F6: count cancelled bookings in date range
    @Query(value = """
    SELECT COUNT(*) FROM bookings
    WHERE booking_date BETWEEN :start AND :end AND status = 'CANCELLED'
    """, nativeQuery = true)
    long countCancelledInRange(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    // S3-F6: sum revenue from completed bookings in date range
    @Query(value = """
    SELECT COALESCE(SUM(total_amount), 0) FROM bookings
    WHERE booking_date BETWEEN :start AND :end AND status = 'COMPLETED'
    """, nativeQuery = true)
    double sumRevenueInRange(@Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);
    @Modifying
    @Transactional
    @Query(value = """
    UPDATE tickets SET status = 'CANCELLED'
    WHERE booking_id = :bookingId AND status = 'VALID'
    """, nativeQuery = true)
    void cancelValidTicketsForBooking(@Param("bookingId") Long bookingId);
    @Query(value = """
    SELECT COALESCE(MAX(event_order), 0) FROM booking_items
    WHERE booking_id = :bookingId
    """, nativeQuery = true)
    int findMaxEventOrderByBookingId(@Param("bookingId") Long bookingId);


}