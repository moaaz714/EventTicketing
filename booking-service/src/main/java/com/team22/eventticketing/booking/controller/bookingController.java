package com.team22.eventticketing.booking.controller;

import com.team22.eventticketing.booking.dto.*;
import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.model.BookingStatus;
import com.team22.eventticketing.booking.security.AuthContext;
import com.team22.eventticketing.booking.service.BookingService;
import com.team22.eventticketing.booking.repository.BookingRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    private void assertOwnerOrAdmin(Long bookingUserId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;
        Long callerId = (Long) auth.getDetails();
        if (!callerId.equals(bookingUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
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
        Booking booking = bookingService.findById(id);
        assertOwnerOrAdmin(booking.getUserId());
        return ResponseEntity.ok(booking);
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
        assertOwnerOrAdmin(bookingService.findById(id).getUserId());
        return ResponseEntity.ok(bookingService.update(id, patch));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assertOwnerOrAdmin(bookingService.findById(id).getUserId());
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── S3-F4: Complete Booking ──────────────────────────────────────────────────
    @PutMapping("/{id}/complete")
    public ResponseEntity<Booking> completeBooking(@PathVariable Long id) {
        assertOwnerOrAdmin(bookingService.findById(id).getUserId());
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

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        assertOwnerOrAdmin(bookingService.findById(id).getUserId());
        bookingService.cancelBooking(id);
        return ResponseEntity.ok().build();
    }

    // ─── Add Items to Booking ─────────────────────────────────────────────────────
    @PostMapping("/{bookingId}/items")
    public ResponseEntity<Booking> addItemsToBooking(
            @PathVariable Long bookingId,
            @RequestBody List<BookingItemRequest> requests) {

        assertOwnerOrAdmin(bookingService.findById(bookingId).getUserId());
        Booking updatedBooking = bookingService.addItemsToBooking(bookingId, requests);
        return ResponseEntity.ok(updatedBooking);
    }

    // ─── Get Booking Details ──────────────────────────────────────────────────────
    @GetMapping("/{id}/details")
    public ResponseEntity<BookingDetailsDTO> getBookingDetails(@PathVariable Long id) {
        assertOwnerOrAdmin(bookingService.findById(id).getUserId());
        return ResponseEntity.ok(bookingService.getBookingDetails(id));
    }
    //-- End point for the second feature in the booking service in milestone 2
    @PostMapping("/{bookingId}/record-attendance")
    public ResponseEntity<?> recordAttendance(@PathVariable Long bookingId) {
        assertOwnerOrAdmin(bookingService.findById(bookingId).getUserId());
        bookingService.recordAttendance(bookingId);
        return ResponseEntity.ok("Attendance recorded");
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<EventRecommendationDTO>> getRecommendations(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {

        AuthContext ctx = (AuthContext) request.getAttribute("authContext");
        return ResponseEntity.ok(bookingService.getRecommendations(userId, limit, ctx));
    }
}