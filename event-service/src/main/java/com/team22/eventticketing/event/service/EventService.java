package com.team22.eventticketing.event.service;

import com.team22.eventticketing.event.dto.EventRevenueDTO;
import com.team22.eventticketing.event.model.Event;
import com.team22.eventticketing.event.model.EventCategory;
import com.team22.eventticketing.event.model.EventSession;
import com.team22.eventticketing.event.repository.EventRepository;
import com.team22.eventticketing.event.repository.EventSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team22.eventticketing.booking.model.Booking;
import com.team22.eventticketing.booking.model.BookingStatus;
import com.team22.eventticketing.booking.repository.BookingRepository;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class EventService {

    private final RestTemplate restTemplate = new RestTemplate();
    private  final EventRepository eventRepository;
    private final EventSessionRepository eventSessionRepository;

    public EventService(EventRepository eventRepository, EventSessionRepository eventSessionRepository) {
        this.eventRepository = eventRepository;
        this.eventSessionRepository = eventSessionRepository;

    }
    public List<Event> searchEvents(EventCategory category,
                                    LocalDateTime startDate,
                                    LocalDateTime endDate) {

        // Case 1: category + both dates
        if (category != null && startDate != null && endDate != null) {
            return eventRepository.findByCategoryAndEventDateBetweenOrderByEventDateAsc(
                    category, startDate, endDate);
        }

        // Case 2: category + only startDate
        if (category != null && startDate != null) {
            return eventRepository.findByCategoryAndEventDateAfterOrderByEventDateAsc(
                    category, startDate);
        }

        // Case 3: category + only endDate
        if (category != null && endDate != null) {
            return eventRepository.findByCategoryAndEventDateBeforeOrderByEventDateAsc(
                    category, endDate);
        }

        // Case 4: only category
        if (category != null) {
            return eventRepository.findByCategoryOrderByEventDateAsc(category);
        }

        // Case 5: only date range
        if (startDate != null && endDate != null) {
            return eventRepository.findByEventDateBetweenOrderByEventDateAsc(
                    startDate, endDate);
        }

        // Case 6: only startDate
        if (startDate != null) {
            return eventRepository.findByEventDateAfterOrderByEventDateAsc(startDate);
        }

        // Case 7: only endDate
        if (endDate != null) {
            return eventRepository.findByEventDateBeforeOrderByEventDateAsc(endDate);
        }

        // Case 8: no filters
        return eventRepository.findAllByOrderByEventDateAsc();
    }


    public void updateEventDetails(Long id, Map<String, Object> newDetails) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Map<String, Object> existingDetails = event.getDetails();

        if (existingDetails == null) {
            existingDetails = new HashMap<>();
        }

        existingDetails.putAll(newDetails);

        event.setDetails(existingDetails);

        eventRepository.save(event);
    }


    public EventRevenueDTO getEventRevenue(Long eventId,
                                           LocalDateTime start,
                                           LocalDateTime end) {

        // 1. Check event exists
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));

        // 2. Call booking-service
        String url = "http://localhost:8081/api/bookings/event/" + eventId +
                "/revenue?startDate=" + start.toLocalDate() +
                "&endDate=" + end.toLocalDate();

        Map response = restTemplate.getForObject(url, Map.class);

        long totalBookings = ((Number) response.get("totalBookings")).longValue();
        double totalRevenue = ((Number) response.get("totalRevenue")).doubleValue();

        double avg = totalBookings == 0 ? 0 : totalRevenue / totalBookings;

        return new EventRevenueDTO(
                eventId,
                event.getName(),
                totalBookings,
                totalRevenue,
                avg
        );
    }


    @Transactional
    public void rateEvent(Long eventId, Long bookingId, Double newRating) {
        // 1. Check rating bounds (400)
        if (newRating < 1.0 || newRating > 5.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        // 2. Find Event (404)
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        // 3. Check if booking exists (404)
        if (eventRepository.checkBookingExists(bookingId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }

        // 4. Verify booking matches event and is COMPLETED (400)
        if (eventRepository.countCompletedBookingForEvent(bookingId, eventId) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is not completed or does not belong to this event");
        }

        // 5. Recalculate running average
        int currentCount = event.getTotalRatings();
        double currentTotalScore = event.getRating() * currentCount;

        int newCount = currentCount + 1;
        double updatedAverage = (currentTotalScore + newRating) / newCount;

        // 6. Update and Save
        event.setTotalRatings(newCount);
        event.setRating(updatedAverage);
        eventRepository.save(event);
    }

    // [S2-F8] Verify Event Session
    @Transactional
    public Event verifyEventSession(Long eventId, Long sessionId, Long verifiedByUserId) {

        // 1. Find Event (404)
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        // 2. Find Session (404)
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        // 3. Verify session belongs to the specific event (400)
        if (!session.getEvent().getId().equals(eventId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session does not belong to this event");
        }

        // 4. Check if startTime is in the future (400)
        if (session.getStartTime().isBefore(java.time.LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot verify a session that has already started");
        }

        // 5. Cross-Service Check: Is the user an ADMIN? (403)
        if (eventRepository.checkUserIsAdmin(verifiedByUserId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not authorized (Must be ADMIN)");
        }

        // 6. Update the session properties
        session.setVerified(true);
        if (session.getMetadata() == null) {
            session.setMetadata(new java.util.HashMap<>());
        }
        session.getMetadata().put("verifiedAt", java.time.LocalDateTime.now().toString());
        session.getMetadata().put("verifiedBy", verifiedByUserId);

        // Save session
        eventSessionRepository.save(session);

        // Return the updated event
        return event;
    }
}