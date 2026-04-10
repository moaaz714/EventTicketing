package com.team22.eventticketing.sales.repository;

import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.entity.enums.SaleStatus;
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
    // BASIC DATE FILTERS (HEAD kept)
    // =========================

    // Get all ticket sales created between two dates
    List<TicketSale> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Get ticket sales of a specific status between two dates
    List<TicketSale> findByStatusAndCreatedAtBetween(SaleStatus status, LocalDateTime start, LocalDateTime end);

    // =========================
    // S5-F1 (MAIN FEATURE - advanced filtering)
    // =========================

    @Query(value = """
            SELECT * FROM ticket_sales
            WHERE (:status IS NULL OR status = :status)
              AND created_at BETWEEN :startDate AND :endDate
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<TicketSale> findByStatusAndDateRange(
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // =========================
    // S5-F3 (USER SUMMARY)
    // =========================

    @Query(value = "SELECT COUNT(*) FROM users WHERE id = :userId", nativeQuery = true)
    Long countUserById(@Param("userId") Long userId);

    @Query(value = """
            SELECT method, COUNT(*) as count, SUM(amount) as total
            FROM ticket_sales
            WHERE user_id = :userId AND status = 'COMPLETED'
            GROUP BY method
            """, nativeQuery = true)
    List<Object[]> findSalesSummaryByUserId(@Param("userId") Long userId);

    // =========================
    // S5-F4 (BOOKING)
    // =========================

    Optional<TicketSale> findByBookingIdAndStatus(Long bookingId, SaleStatus status);

    Optional<TicketSale> findByBookingId(Long bookingId);

    // =========================
    // S5-F6 (REVENUE REPORT)
    // =========================

    @Query(value = """
            SELECT
              COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN amount ELSE 0 END), 0) as totalRevenue,
              COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as totalTransactions,
              COALESCE(SUM(CASE WHEN status = 'REFUNDED' THEN amount ELSE 0 END), 0) as refundedAmount,
              COUNT(CASE WHEN status = 'REFUNDED' THEN 1 END) as refundCount
            FROM ticket_sales
            WHERE created_at BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    Object[] getRevenueReport(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}