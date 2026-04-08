package com.team22.eventticketing.sales.controller;

import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.entity.enums.PaymentMethod;
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

    @GetMapping("/health")
    public String statusCheck() {
        return "OK";
    }

    @PostMapping("/booking/{bookingId}")
    public ResponseEntity<TicketSale> payBooking(
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> body
    ) {
        // Extract payment method and optional cardLastFour
        PaymentMethod method = PaymentMethod.valueOf(body.get("method"));
        String cardLastFour = body.get("cardLastFour");

        TicketSale sale = salesService.processBookingPayment(bookingId, method, cardLastFour);

        return new ResponseEntity<>(sale, HttpStatus.CREATED);
    }
}