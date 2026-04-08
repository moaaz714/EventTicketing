package com.team22.eventticketing.sales.repository;

import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.entity.enums.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketSaleRepository extends JpaRepository<TicketSale, Long> {

    // Get all ticket sales created between two dates
    List<TicketSale> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Get ticket sales of a specific status between two dates
    List<TicketSale> findByStatusAndCreatedAtBetween(SaleStatus status, LocalDateTime start, LocalDateTime end);
}