package com.team22.eventticketing.event.service;

import com.team22.eventticketing.event.dto.UpdateEventStatusRequest;
import com.team22.eventticketing.event.model.Event;
import com.team22.eventticketing.event.model.EventStatus;
import com.team22.eventticketing.event.model.EventSession;
import com.team22.eventticketing.event.repository.EventRepository;
import com.team22.eventticketing.event.repository.EventSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventSessionRepository eventSessionRepository;

    public EventService(EventRepository eventRepository, EventSessionRepository eventSessionRepository) {
        this.eventRepository = eventRepository;
        this.eventSessionRepository = eventSessionRepository;
    }

    // [S2-F4] Update Event Status (Teammate's Feature)
    @Transactional
    public void updateEventStatus(Long eventId, UpdateEventStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found with id: " + eventId));

        EventStatus newStatus = request.getStatus();

        if (newStatus == EventStatus.CANCELLED && eventRepository.existsActiveBookingsForEvent(eventId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel event because active bookings exist for this event");
        }

        event.setStatus(newStatus);
        eventRepository.save(event);
    }

    // [S2-F7] Rate an Event (Your Feature)
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

    // [S2-F8] Verify Event Session (Your Feature)
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

    // [S2-F9] Get Events with Unverified Sessions (Your Feature)
    public java.util.List<com.team22.eventticketing.event.dto.EventSessionAlertDTO> getUnverifiedSessionsReport() {

        java.util.List<Event> events = eventRepository.findEventsWithUnverifiedSessions();

        return events.stream().map(event -> {

            java.util.List<com.team22.eventticketing.event.model.EventSession> unverifiedSessions =
                    event.getEventSessions().stream()
                            .filter(session -> session.getVerified() != null && !session.getVerified())
                            .toList();

            return new com.team22.eventticketing.event.dto.EventSessionAlertDTO(
                    event.getId(),
                    event.getName(),
                    event.getStatus(),
                    unverifiedSessions,
                    unverifiedSessions.size()
            );
        }).toList();
    }

    public List<Event> filterEventsByDetail(String key, String value, String status) {

        if (key == null || key.isBlank() || value == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Key and value are required"
            );
        }

        if (status == null || status.isBlank()) {
            return eventRepository.findByDetailAttribute(key, value);
        }

        return eventRepository.findByDetailAttributeAndStatus(key, value, status);
    }
}