package com.team22.eventticketing.sales.service;

import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.entity.enums.SaleStatus;
import com.team22.eventticketing.sales.repository.TicketSaleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team22.eventticketing.sales.entity.enums.PaymentMethod;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    @Transactional
    public TicketSale processBookingPayment(Long bookingId, PaymentMethod method, String cardLastFour) {

        // 1. Find sale by booking
        TicketSale sale = ticketSaleRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "TicketSale not found for booking: " + bookingId));

        // 2. Check if already COMPLETED
        if (sale.getStatus() == SaleStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "already paid");
        }

        // 3. Validate status (must be PENDING or allowed state)
        if (sale.getStatus() != SaleStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid booking status");
        }

        // 4. Update payment info
        sale.setMethod(method);
        sale.setStatus(SaleStatus.COMPLETED);

        // 5. Populate transaction details (JSONB)
        if (sale.getTransactionDetails() == null) {
            sale.setTransactionDetails(new java.util.HashMap<>());
        }

        sale.getTransactionDetails().put("paymentMethod", method.toString());
        sale.getTransactionDetails().put("cardLastFour", cardLastFour);
        sale.getTransactionDetails().put("paidAt", java.time.LocalDateTime.now().toString());

        // 6. Save
        return ticketSaleRepository.save(sale);
    }
}
