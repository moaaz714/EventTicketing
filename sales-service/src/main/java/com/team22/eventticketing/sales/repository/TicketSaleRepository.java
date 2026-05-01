package com.team22.eventticketing.sales.repository;

import com.team22.eventticketing.sales.entity.TicketSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketSaleRepository extends JpaRepository<TicketSale, Long> {

    // =========================
    // S5-F8 (Sale Details with Promotions)
    // =========================
    @Query("SELECT ts FROM TicketSale ts LEFT JOIN FETCH ts.salePromotions sp LEFT JOIN FETCH sp.promotion WHERE ts.id = :id")
    Optional<TicketSale> findByIdWithPromotions(@Param("id") Long id);

    // =========================
    // S5-F4 (Booking Payment Flow)
    // =========================
    Optional<TicketSale> findByBookingId(Long bookingId);

    List<TicketSale> findAllByBookingId(Long bookingId);

    @Query(value = "SELECT status FROM bookings WHERE id = :bookingId", nativeQuery = true)
    String findBookingStatus(@Param("bookingId") Long bookingId);

    @Query(value = "SELECT COUNT(*) > 0 FROM ticket_sales WHERE booking_id = :bookingId AND status::text = 'COMPLETED'", nativeQuery = true)
    boolean existsByBookingIdAndStatusCompleted(@Param("bookingId") Long bookingId);

    @Query(value = "SELECT * FROM ticket_sales WHERE booking_id = :bookingId AND status::text = 'PENDING' LIMIT 1", nativeQuery = true)
    Optional<TicketSale> findByBookingIdAndStatusPending(@Param("bookingId") Long bookingId);

    // =========================
    // S5-F1 (Filter by status and date range)
    // =========================
    @Query(value = """
            SELECT * FROM ticket_sales
            WHERE (:status IS NULL OR status::text = :status)
              AND created_at BETWEEN :startDate AND :endDate
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<TicketSale> findByStatusAndDateRange(
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // =========================
    // S5-F6 (Filter by status and date range for revenue)
    // =========================
    @Query(value = """
            SELECT * FROM ticket_sales
            WHERE status::text = :status
            AND created_at BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    List<TicketSale> findByStatusAndCreatedAtBetween(
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // =========================
    // S5-F3 (User Summary)
    // =========================
    @Query(value = "SELECT COUNT(*) FROM users WHERE id = :userId", nativeQuery = true)
    Long countUserById(@Param("userId") Long userId);

    @Query(value = """
            SELECT method::text, COUNT(*) as count, SUM(amount) as total
            FROM ticket_sales
            WHERE user_id = :userId AND status::text = 'COMPLETED'
            GROUP BY method::text
            """, nativeQuery = true)
    List<Object[]> findSalesSummaryByUserId(@Param("userId") Long userId);

    // =========================
    // S5-F6 (Revenue report)
    // =========================
    @Query(value = """
            SELECT
              COALESCE(SUM(CASE WHEN status::text = 'COMPLETED' THEN amount ELSE 0 END), 0) as totalRevenue,
              COUNT(CASE WHEN status::text = 'COMPLETED' THEN 1 END) as totalTransactions,
              COALESCE(SUM(CASE WHEN status::text = 'REFUNDED' THEN amount ELSE 0 END), 0) as refundedAmount,
              COUNT(CASE WHEN status::text = 'REFUNDED' THEN 1 END) as refundCount
            FROM ticket_sales
            WHERE created_at BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    Object[] getRevenueReport(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}