package com.team22.eventticketing.sales.service;

import com.team22.eventticketing.sales.dto.RevenueReportDTO;
import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.entity.enums.SaleStatus;
import com.team22.eventticketing.sales.repository.TicketSaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SalesService {

    private final TicketSaleRepository ticketSaleRepository;

    // Constructor injection of the repository
    public SalesService(TicketSaleRepository ticketSaleRepository) {
        this.ticketSaleRepository = ticketSaleRepository;
    }

    public RevenueReportDTO getRevenueReport(LocalDateTime startDate, LocalDateTime endDate) {

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }

        // Completed sales
        List<TicketSale> completed = ticketSaleRepository.findByStatusAndCreatedAtBetween(
                SaleStatus.COMPLETED, startDate, endDate);
        double totalRevenue = completed.stream().mapToDouble(TicketSale::getAmount).sum();
        int totalTransactions = completed.size();
        double averageSale = totalTransactions == 0 ? 0 : totalRevenue / totalTransactions;

        // Refunded sales
        List<TicketSale> refunded = ticketSaleRepository.findByStatusAndCreatedAtBetween(
                SaleStatus.REFUNDED, startDate, endDate);
        double refundedAmount = refunded.stream().mapToDouble(TicketSale::getAmount).sum();
        int refundCount = refunded.size();

        return new RevenueReportDTO(totalRevenue, totalTransactions, averageSale, refundedAmount, refundCount);
    }
}