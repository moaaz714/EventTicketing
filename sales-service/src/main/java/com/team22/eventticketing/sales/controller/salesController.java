package com.team22.eventticketing.sales.controller;

import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.service.SalesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class salesController {

    private final SalesService salesService;

    public salesController(SalesService salesService) {
        this.salesService = salesService;
    }

    // ─── Health check ─────────────────────────────
    @GetMapping("/health")
    public String statusCheck() {
        return "OK";
    }

    // ─── Process booking payment (S5-F4) ─────────
    @PostMapping("/processPayment")
    public ResponseEntity<TicketSale> processPayment(
            @RequestParam Long bookingId,
            @RequestParam String method,
            @RequestParam(required = false) String cardLastFour
    ) {
        com.team22.eventticketing.sales.entity.enums.PaymentMethod paymentMethod =
                com.team22.eventticketing.sales.entity.enums.PaymentMethod.valueOf(method.toUpperCase());

        TicketSale sale = salesService.processBookingPayment(bookingId, paymentMethod, cardLastFour);

        return new ResponseEntity<>(sale, HttpStatus.OK);
    }

    // ─── Apply promotion to ticket sale (S5-F5) ──
    @PostMapping("/{saleId}/promotions/{promotionId}")
    public ResponseEntity<TicketSale> applyPromotion(
            @PathVariable Long saleId,
            @PathVariable Long promotionId
    ) {
        TicketSale updatedSale = salesService.applyPromotionToSale(saleId, promotionId);
        return new ResponseEntity<>(updatedSale, HttpStatus.OK);
    }
}