package com.team22.eventticketing.sales.controller;

import com.team22.eventticketing.sales.dto.UserSaleSummaryDTO;
import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.service.TicketSaleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class salesController {

    private final TicketSaleService ticketSaleService;

    public salesController(TicketSaleService ticketSaleService) {
        this.ticketSaleService = ticketSaleService;
    }

    @GetMapping("/health")
    public String statusCheck() {
        return "OK";
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────────

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

    // ─── [S5-F2] Process Refund ───────────────────────────────────────────────────

    @PutMapping("/{id}/refund")
    public TicketSale refund(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ticketSaleService.refund(id, body.get("reason"));
    }

    // ─── [S5-F3] User Ticket Sale Summary ────────────────────────────────────────

    @GetMapping("/user/{userId}/summary")
    public UserSaleSummaryDTO getUserSummary(@PathVariable Long userId) {
        return ticketSaleService.getUserSummary(userId);
    }

    // ─── [S5-F1] Search by optional status and date range ────────────────────────

    @GetMapping("/search")
    public List<TicketSale> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ticketSaleService.search(status, startDate, endDate);
    }
}
