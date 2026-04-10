package com.team22.eventticketing.booking.service;

import com.team22.eventticketing.booking.dto.BookingCostEstimateDTO;
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

    // Make sure to import com.teamXX.eventticketing.booking.dto.BookingCostEstimateDTO;

    public BookingCostEstimateDTO estimateCost(Long eventId, Integer ticketCount, String ticketTier) {
        // 1. Get Average Capacity
        Double avgCapacity = bookingRepository.getAverageSessionCapacity(eventId);
        if (avgCapacity == null) {
            avgCapacity = 0.0; // Fallback if the event has no sessions yet
        }

        // 2. Calculate Base Price and Tier Multiplier
        double basePrice = avgCapacity / 10.0;
        double tierMultiplier = "VIP".equalsIgnoreCase(ticketTier) ? 2.5 : 1.0;

        // 3. Calculate Ticket Cost
        double ticketCost = basePrice * tierMultiplier * ticketCount;

        // 4. Calculate Service Fee (15%)
        double serviceFee = ticketCost * 0.15;

        // 5. Determine Demand Multiplier
        Long activeBookings = bookingRepository.countActiveBookingsForEvent(eventId);
        double demandMultiplier = 1.0;
        if (activeBookings > 200) {
            demandMultiplier = 1.5;
        } else if (activeBookings > 50) {
            demandMultiplier = 1.25;
        }

        // 6. Calculate Estimated Total
        double estimatedTotal = (ticketCost + serviceFee) * demandMultiplier;

        // Return the mapped DTO
        return new BookingCostEstimateDTO(ticketCost, serviceFee, estimatedTotal, demandMultiplier);
    }


}