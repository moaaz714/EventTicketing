package com.team22.eventticketing.booking.repository;

import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

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
}