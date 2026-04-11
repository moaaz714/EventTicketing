package com.team22.eventticketing.booking.controller;

import com.team22.eventticketing.booking.dto.BookingAnalyticsDTO;
import com.team22.eventticketing.booking.dto.BookingCostEstimateDTO;
import com.team22.eventticketing.booking.dto.BookingDetailsDTO;
import com.team22.eventticketing.booking.dto.BookingItemRequest;
import com.team22.eventticketing.booking.dto.CostEstimateRequest;
import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.model.BookingStatus;
import com.team22.eventticketing.booking.service.BookingService;
import com.team22.eventticketing.booking.repository.BookingRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings") // Requirement 4.a
public class bookingController {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    // Fixed constructor for dependency injection
    public bookingController(BookingService bookingService, BookingRepository bookingRepository) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/health")
    public String statusCheck(){
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

    @GetMapping("/event/{eventId}/revenue")
    public ResponseEntity<?> getEventRevenue(
            @PathVariable Long eventId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end   = LocalDateTime.parse(endDate + "T23:59:59");

        Object[] result = bookingRepository.getEventRevenueStats(eventId, start, end);

        return ResponseEntity.ok(result);
    }
}