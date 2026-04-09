package com.team22.eventticketing.booking.service;

import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.repository.BookingRepository;
import com.team22.eventticketing.booking.model.BookingStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

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
}