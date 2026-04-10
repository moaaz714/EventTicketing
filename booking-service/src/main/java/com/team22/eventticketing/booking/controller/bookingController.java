package com.team22.eventticketing.booking.controller;

import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class bookingController {

    private final BookingService bookingService;

    public bookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // ─── Health ───────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public String statusCheck() {
        return "OK";
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
}