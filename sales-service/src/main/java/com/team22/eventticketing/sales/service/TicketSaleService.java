package com.team22.eventticketing.sales.service;

import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.repository.TicketSaleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
