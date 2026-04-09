package com.team22.eventticketing.booking.service;

import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.model.BookingStatus;
import com.team22.eventticketing.booking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> searchBookings(BookingStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        if (status != null) {
            return bookingRepository.findByStatusAndBookingDateBetweenOrderByBookingDateDesc(status, startDate, endDate);
        } else {
            return bookingRepository.findByBookingDateBetweenOrderByBookingDateDesc(startDate, endDate);
        }
    }

    @Transactional
    public Booking confirmBooking(Long bookingId, Long eventId) {
        // 1. Find booking -> throw 404 if not found
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        // 2. Validate booking status is PENDING -> throw 400 if not
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is not PENDING. Current status: " + booking.getStatus());
        }

        // 3. Verify event exists and is UPCOMING -> throw 404 or 400
        String eventStatus = bookingRepository.findEventStatusById(eventId);
        if (eventStatus == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event with ID " + eventId + " not found");
        }
        if (!"UPCOMING".equals(eventStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is not UPCOMING. Current status: " + eventStatus);
        }

        // 4. Update booking fields
        booking.setEventId(eventId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());

        // 5. Save and return
        return bookingRepository.save(booking);
    }
}