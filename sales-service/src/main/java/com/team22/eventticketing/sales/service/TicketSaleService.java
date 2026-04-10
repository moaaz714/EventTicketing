package com.team22.eventticketing.sales.service;

import com.team22.eventticketing.sales.dto.UserSaleSummaryDTO;
import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.entity.enums.SaleStatus;
import com.team22.eventticketing.sales.repository.TicketSaleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TicketSaleService {

    private final TicketSaleRepository ticketSaleRepository;

    public TicketSaleService(TicketSaleRepository ticketSaleRepository) {
        this.ticketSaleRepository = ticketSaleRepository;
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    public TicketSale create(TicketSale ticketSale) {
        return ticketSaleRepository.save(ticketSale);
    }

    // ─── Read by ID ───────────────────────────────────────────────────────────────

    public TicketSale findById(Long id) {
        return ticketSaleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "TicketSale not found with id: " + id));
    }

    // ─── Read all ─────────────────────────────────────────────────────────────────

    public List<TicketSale> findAll() {
        return ticketSaleRepository.findAll();
    }

    // ─── Update ───────────────────────────────────────────────────────────────────

    public TicketSale update(Long id, TicketSale patch) {
        TicketSale existing = findById(id);

        if (patch.getBookingId() != null)           existing.setBookingId(patch.getBookingId());
        if (patch.getUserId() != null)              existing.setUserId(patch.getUserId());
        if (patch.getAmount() != null)              existing.setAmount(patch.getAmount());
        if (patch.getMethod() != null)              existing.setMethod(patch.getMethod());
        if (patch.getStatus() != null)              existing.setStatus(patch.getStatus());
        if (patch.getTransactionDetails() != null)  existing.setTransactionDetails(patch.getTransactionDetails());

        return ticketSaleRepository.save(existing);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────────

    public void delete(Long id) {
        if (!ticketSaleRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "TicketSale not found with id: " + id);
        }
        ticketSaleRepository.deleteById(id);
    }

    // ─── [S5-F2] Process Refund ───────────────────────────────────────────────────

    @Transactional
    public TicketSale refund(Long id, String reason) {
        TicketSale sale = findById(id);

        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only COMPLETED sales can be refunded");
        }

        sale.setStatus(SaleStatus.REFUNDED);
        sale.getTransactionDetails().put("refundReason", reason);
        sale.getTransactionDetails().put("refundedAt", LocalDateTime.now().toString());

        return ticketSaleRepository.save(sale);
    }

    // ─── [S5-F3] User Ticket Sale Summary ────────────────────────────────────────

    public UserSaleSummaryDTO getUserSummary(Long userId) {
        if (ticketSaleRepository.countUserById(userId) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found with id: " + userId);
        }

        List<Object[]> rows = ticketSaleRepository.findSalesSummaryByUserId(userId);

        Map<String, Double> methodBreakdown = new HashMap<>();
        long totalSales = 0;
        double totalAmount = 0.0;

        for (Object[] row : rows) {
            String method = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double total = ((Number) row[2]).doubleValue();

            methodBreakdown.put(method, total);
            totalSales += count;
            totalAmount += total;
        }

        return new UserSaleSummaryDTO(userId, totalSales, totalAmount, methodBreakdown);
    }

    // ─── [S5-F1] Search by optional status and date range ────────────────────────

    public List<TicketSale> search(String status, String startDate, String endDate) {
        LocalDateTime start = (startDate != null)
                ? LocalDate.parse(startDate).atStartOfDay()
                : LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        LocalDateTime end = (endDate != null)
                ? LocalDate.parse(endDate).atTime(23, 59, 59)
                : LocalDateTime.of(9999, 12, 31, 23, 59, 59);

        return ticketSaleRepository.findByStatusAndDateRange(status, start, end);
    }
}
