package com.team22.eventticketing.booking.controller;


import com.team22.eventticketing.booking.dto.BookingCostEstimateDTO;
import com.team22.eventticketing.booking.dto.CostEstimateRequest;
import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.model.BookingStatus;
import com.team22.eventticketing.booking.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.List;

import com.team22.eventticketing.booking.dto.BookingAnalyticsDTO;
import java.time.LocalDateTime;
import java.util.List;
import com.team22.eventticketing.booking.dto.BookingItemRequest;
import com.team22.eventticketing.booking.dto.BookingDetailsDTO;
@RestController
@RequestMapping("/api/bookings")
public class bookingController {
    private final BookingService bookingService;

    public bookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    private final BookingService bookingService;

    public bookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // ─── Health ───────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public String statusCheck() {
        return "OK";
    }

    @GetMapping("/search")
    public ResponseEntity<List<Booking>> searchBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<Booking> results = bookingService.searchBookings(status, startDate, endDate);
        return ResponseEntity.ok(results);
    }

    @PutMapping("/{bookingId}/confirm")
    public ResponseEntity<Booking> confirmBooking(
            @PathVariable Long bookingId,
            @RequestParam Long eventId) {

        Booking confirmedBooking = bookingService.confirmBooking(bookingId, eventId);
        return ResponseEntity.ok(confirmedBooking);
    }



    @PostMapping("/estimate")
    public ResponseEntity<BookingCostEstimateDTO> getBookingCostEstimate(@RequestBody CostEstimateRequest request) {
        BookingCostEstimateDTO estimate = bookingService.estimateCost(
                request.getEventId(),
                request.getTicketCount(),
                request.getTicketTier()
        );
        return ResponseEntity.ok(estimate);
    }
}
}
}
    // ─── Create ──────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Booking> create(@RequestBody Booking booking) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(booking));
    }

    // ─── Read by ID ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<Booking> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.findById(id));
    }

    // ─── Read all (optionally filtered by userId) ─────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Booking>> findAll(
            @RequestParam(required = false) Long userId) {
        if (userId != null) {
            return ResponseEntity.ok(bookingService.findByUserId(userId));
        }
        return ResponseEntity.ok(bookingService.findAll());
    }

    // ─── Update ───────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<Booking> update(
            @PathVariable Long id,
            @RequestBody Booking patch) {
        return ResponseEntity.ok(bookingService.update(id, patch));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }
    // ─── S3-F4: Complete Booking ──────────────────────────────────────────────────
    @PutMapping("/{id}/complete")
    public ResponseEntity<Booking> completeBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.completeBooking(id));
    }
    // ─── S3-F5: Filter by Metadata ────────────────────────────────────────────────
    @GetMapping("/metadata/search")
    public ResponseEntity<List<Booking>> searchByMetadata(
            @RequestParam String key,
            @RequestParam String value) {
        return ResponseEntity.ok(bookingService.filterByMetadata(key, value));
    }
    // ─── S3-F6: Booking Analytics ─────────────────────────────────────────────────
    @GetMapping("/analytics")
    public ResponseEntity<BookingAnalyticsDTO> getAnalytics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end   = LocalDateTime.parse(endDate + "T23:59:59");
        return ResponseEntity.ok(bookingService.getAnalytics(start, end));
    }
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/{bookingId}/items")
    public ResponseEntity<Booking> addItems(
            @PathVariable Long bookingId,
            @RequestBody List<BookingItemRequest> items) {
        return ResponseEntity.ok(bookingService.addItemsToBooking(bookingId, items));
    }

    @GetMapping("/{bookingId}/details")
    public ResponseEntity<BookingDetailsDTO> getBookingDetails(
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBookingDetails(bookingId));
    }
}
