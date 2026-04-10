package com.team22.eventticketing.booking.service;

import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.repository.BookingRepository;
import com.team22.eventticketing.booking.model.BookingStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import com.team22.eventticketing.booking.dto.BookingAnalyticsDTO;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    public Booking create(Booking booking) {
        if (booking.getBookingDate() == null) {
            booking.setBookingDate(LocalDateTime.now());
        }
        return bookingRepository.save(booking);
    }

    // ─── Read by ID ───────────────────────────────────────────────────────────────

    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found with id: " + id));
    }

    // ─── Read all ─────────────────────────────────────────────────────────────────

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    // ─── Read by userId ───────────────────────────────────────────────────────────

    public List<Booking> findByUserId(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    // ─── Update ───────────────────────────────────────────────────────────────────

    public Booking update(Long id, Booking patch) {
        Booking existing = findById(id);

        if (patch.getContactEmail() != null) existing.setContactEmail(patch.getContactEmail());
        if (patch.getStatus() != null)       existing.setStatus(patch.getStatus());
        if (patch.getTotalAmount() != null)  existing.setTotalAmount(patch.getTotalAmount());
        if (patch.getMetadata() != null)     existing.setMetadata(patch.getMetadata());
        if (patch.getConfirmedAt() != null)  existing.setConfirmedAt(patch.getConfirmedAt());
        if (patch.getEventId() != null)      existing.setEventId(patch.getEventId());

        return bookingRepository.save(existing);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────────

    public void delete(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Booking not found with id: " + id);
        }
        bookingRepository.deleteById(id);
    }
    // ─── S3-F4: Complete Booking ──────────────────────────────────────────────────
    @Transactional
    public Booking completeBooking(Long id) {
        Booking booking = bookingRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found with id: " + id));

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only CHECKED_IN bookings can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);

        if (booking.getTotalAmount() == null && booking.getBookingItems() != null) {
            double total = booking.getBookingItems().stream()
                    .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                    .sum();
            booking.setTotalAmount(total);
        }

        Booking saved = bookingRepository.save(booking);

        bookingRepository.insertTicketSale(
                saved.getId(),
                saved.getUserId(),
                saved.getTotalAmount() != null ? saved.getTotalAmount() : 0.0
        );

        return saved;
    }
    // ─── S3-F5: Filter Bookings by Metadata ──────────────────────────────────────
    public List<Booking> filterByMetadata(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Key and value must not be blank");
        }
        return bookingRepository.findByMetadataField(key, value);
    }
    // ─── S3-F6: Booking Analytics ─────────────────────────────────────────────────
    public BookingAnalyticsDTO getAnalytics(LocalDateTime start, LocalDateTime end) {
        long total     = bookingRepository.countInRange(start, end);
        long completed = bookingRepository.countCompletedInRange(start, end);
        long cancelled = bookingRepository.countCancelledInRange(start, end);
        double revenue = bookingRepository.sumRevenueInRange(start, end);
        double avg     = completed > 0 ? revenue / completed : 0.0;
        double rate    = total > 0 ? (completed * 100.0) / total : 0.0;

        return new BookingAnalyticsDTO(total, completed, cancelled, revenue, avg, rate);
    }
    @Transactional
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found with id: " + id));

        if (booking.getStatus() != BookingStatus.PENDING &&
                booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PENDING or CONFIRMED bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        bookingRepository.cancelValidTicketsForBooking(id);
    }
}