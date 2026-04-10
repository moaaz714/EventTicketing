package com.team22.eventticketing.booking.controller;

import com.team22.eventticketing.booking.dto.BookingAnalyticsDTO;
import com.team22.eventticketing.booking.dto.BookingDetailsDTO;
import com.team22.eventticketing.booking.dto.BookingItemRequest;
import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.model.BookingStatus;
import com.team22.eventticketing.booking.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings") // Requirement 4.a
public class BookingController {

    private final BookingService bookingService;

    // Added constructor for dependency injection
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
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

}