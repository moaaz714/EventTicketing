package com.team22.eventticketing.booking.service;

import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.model.BookingStatus;
import com.team22.eventticketing.booking.repository.BookingItemRepository;
import com.team22.eventticketing.booking.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;

    // We use the updated constructor from the main branch that includes both repositories
    public BookingService(BookingRepository bookingRepository, BookingItemRepository bookingItemRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
    }

    // ==========================================
    // YOUR CODE (S3-F1)
    // ==========================================

    public List<Booking> searchBookings(BookingStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        if (status != null) {
            return bookingRepository.findByStatusAndBookingDateBetweenOrderByBookingDateDesc(status, startDate, endDate);
        } else {
            return bookingRepository.findByBookingDateBetweenOrderByBookingDateDesc(startDate, endDate);
        }
    }

    // ==========================================
    // TEAMMATES' CODE (Main Branch)
    // ==========================================

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
}