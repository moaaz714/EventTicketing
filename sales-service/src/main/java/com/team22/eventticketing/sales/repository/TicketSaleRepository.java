package com.team22.eventticketing.sales.repository;

import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.entity.enums.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface TicketSaleRepository extends JpaRepository<TicketSale, Long> {

    // Find a ticket sale by bookingId
    Optional<TicketSale> findByBookingId(Long bookingId);

    // Find a ticket sale by bookingId and status
    Optional<TicketSale> findByBookingIdAndStatus(Long bookingId, SaleStatus status);

    // Check if a COMPLETED sale exists for a booking
    boolean existsByBookingIdAndStatus(Long bookingId, SaleStatus status);

    // Optional: get all sales for a booking (for reporting or verification)
    List<TicketSale> findAllByBookingId(Long bookingId);
}