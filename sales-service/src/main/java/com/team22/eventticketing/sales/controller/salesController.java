package com.team22.eventticketing.sales.controller;

import com.team22.eventticketing.sales.dto.RevenueReportDTO;
import com.team22.eventticketing.sales.dto.UserSaleSummaryDTO;
import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.service.SalesService;
import com.team22.eventticketing.sales.service.TicketSaleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class salesController {

    private final SalesService salesService;
    private final TicketSaleService ticketSaleService;

    public salesController(SalesService salesService,
                           TicketSaleService ticketSaleService) {
        this.salesService = salesService;
        this.ticketSaleService = ticketSaleService;
    }

    // ─── Health Check ─────────────────────────────

    @GetMapping("/health")
    public String statusCheck() {
        return "OK";
    }

    // ─── S5-F6: Revenue Report ─────────────────────

    @GetMapping("/reports/revenue")
    public RevenueReportDTO getRevenueReport(
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        return salesService.getRevenueReport(startDateTime, endDateTime);
    }

    // ─── CRUD ─────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketSale create(@RequestBody TicketSale ticketSale) {
        return ticketSaleService.create(ticketSale);
    }

    @GetMapping("/{id}")
    public TicketSale getById(@PathVariable Long id) {
        return ticketSaleService.findById(id);
    }

    @GetMapping
    public List<TicketSale> getAll() {
        return ticketSaleService.findAll();
    }

    @PutMapping("/{id}")
    public TicketSale update(@PathVariable Long id, @RequestBody TicketSale ticketSale) {
        return ticketSaleService.update(id, ticketSale);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ticketSaleService.delete(id);
    }

    // ─── Refund ─────────────────────────────

    @PutMapping("/{id}/refund")
    public TicketSale refund(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ticketSaleService.refund(id, body.get("reason"));
    }

    // ─── User Summary ─────────────────────────────

    @GetMapping("/user/{userId}/summary")
    public UserSaleSummaryDTO getUserSummary(@PathVariable Long userId) {
        return ticketSaleService.getUserSummary(userId);
    }

    // ─── Search ─────────────────────────────

    @GetMapping("/search")
    public List<TicketSale> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return ticketSaleService.search(status, startDate, endDate);
    }
}