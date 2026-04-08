package com.team22.eventticketing.sales.repository;

import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.entity.enums.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketSaleRepository extends JpaRepository<TicketSale, Long> {

    Optional<TicketSale> findByBookingIdAndStatus(Long bookingId, SaleStatus status);

    boolean existsByBookingIdAndStatus(Long bookingId, SaleStatus status);
}