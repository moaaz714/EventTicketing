package com.team22.eventticketing.event.service;

import com.team22.eventticketing.event.dto.EventRevenueDTO;
import com.team22.eventticketing.event.dto.EventSessionAlertDTO;
import com.team22.eventticketing.event.dto.TopEventDTO;
import com.team22.eventticketing.event.dto.UpdateEventStatusRequest;
import com.team22.eventticketing.event.model.Event;
import com.team22.eventticketing.event.model.EventCategory;
import com.team22.eventticketing.event.model.EventSession;
import com.team22.eventticketing.event.model.EventStatus;
import com.team22.eventticketing.event.repository.EventRepository;
import com.team22.eventticketing.event.repository.EventSessionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final EventRepository eventRepository;
    private final EventSessionRepository eventSessionRepository;
    private final CacheEvictionService cacheEviction;

    public EventService(EventRepository eventRepository,
                        EventSessionRepository eventSessionRepository,
                        CacheEvictionService cacheEviction) {
        this.eventRepository = eventRepository;
        this.eventSessionRepository = eventSessionRepository;
        this.cacheEviction = cacheEviction;
    }

    // ─── [S2-F1] Search Events ────────────────────────────────────────────────────

    @Cacheable(cacheNames = "event-service::S2-F1",
            key = "T(String).valueOf(#category) + ':' + T(String).valueOf(#startDate) + ':' + T(String).valueOf(#endDate)")
    public List<Event> searchEvents(EventCategory category, LocalDateTime startDate, LocalDateTime endDate) {
        if (category != null && startDate != null && endDate != null) {
            return eventRepository.findByCategoryAndEventDateBetweenOrderByEventDateAsc(category, startDate, endDate);
        }
        if (category != null && startDate != null) {
            return eventRepository.findByCategoryAndEventDateAfterOrderByEventDateAsc(category, startDate);
        }
        if (category != null && endDate != null) {
            return eventRepository.findByCategoryAndEventDateBeforeOrderByEventDateAsc(category, endDate);
        }
        if (category != null) {
            return eventRepository.findByCategoryOrderByEventDateAsc(category);
        }
        if (startDate != null && endDate != null) {
            return eventRepository.findByEventDateBetweenOrderByEventDateAsc(startDate, endDate);
        }
        if (startDate != null) {
            return eventRepository.findByEventDateAfterOrderByEventDateAsc(startDate);
        }
        if (endDate != null) {
            return eventRepository.findByEventDateBeforeOrderByEventDateAsc(endDate);
        }
        return eventRepository.findAllByOrderByEventDateAsc();
    }

    // ─── [S2-F5] Filter Events by Detail (JSONB) ─────────────────────────────────

    @Cacheable(cacheNames = "event-service::S2-F5",
            key = "#key + ':' + #value + ':' + T(String).valueOf(#status)")
    public List<Event> filterEventsByDetail(String key, String value, String status) {
        if (key == null || key.isBlank() || value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key and value are required");
        }
        if (status == null || status.isBlank()) {
            return eventRepository.findByDetailAttribute(key, value);
        }
        return eventRepository.findByDetailAttributeAndStatus(key, value, status);
    }

    // ─── [S2-F2] Update Event Details (write — invalidates) ──────────────────────

    public void updateEventDetails(Long id, Map<String, Object> newDetails) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Map<String, Object> existingDetails = event.getDetails();
        if (existingDetails == null) existingDetails = new HashMap<>();
        existingDetails.putAll(newDetails);
        event.setDetails(existingDetails);
        eventRepository.save(event);

        cacheEviction.evictEntityAndFeatures("event", id, "S2-F1", "S2-F3", "S2-F5", "S2-F6", "S2-F9", "S2-F10");
    }

    // ─── [S2-F4] Update Event Status (write — invalidates) ───────────────────────

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

        cacheEviction.evictEntityAndFeatures("event", eventId, "S2-F1", "S2-F5", "S2-F6", "S2-F9", "S2-F10");
    }

    // ─── [S2-F6] Top Rated Events Report ─────────────────────────────────────────

    @Cacheable(cacheNames = "event-service::S2-F6", key = "#limit")
    public List<TopEventDTO> getTopRatedEvents(int limit) {
        if (limit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be greater than 0");
        }

        List<Object[]> rows = eventRepository.findTopRatedEvents(limit);
        List<TopEventDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            Long eventId = ((Number) row[0]).longValue();
            String name = (String) row[1];
            Double rating = row[2] == null ? 0.0 : ((Number) row[2]).doubleValue();
            Long totalBookings = row[3] == null ? 0L : ((Number) row[3]).longValue();
            result.add(new TopEventDTO(eventId, name, rating, totalBookings));
        }

        return result;
    }

    // ─── [S2-F3] Get Event Revenue DTO ───────────────────────────────────────────

    @Cacheable(cacheNames = "event-service::S2-F3",
            key = "#eventId + ':' + T(String).valueOf(#start) + ':' + T(String).valueOf(#end)")
    public EventRevenueDTO getEventRevenue(Long eventId, LocalDateTime start, LocalDateTime end) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        String url = "http://localhost:8081/api/bookings/event/" + eventId +
                "/revenue?startDate=" + start.toLocalDate() +
                "&endDate=" + end.toLocalDate();

        Object[][] response = restTemplate.getForObject(url, Object[][].class);
        Object[] stats = response[0];

        long totalBookings = ((Number) stats[0]).longValue();
        double totalRevenue = ((Number) stats[1]).doubleValue();
        double avg = ((Number) stats[2]).doubleValue();

        return new EventRevenueDTO(eventId, event.getName(), totalBookings, totalRevenue, avg);
    }

    // ─── [S2-F7] Rate an Event (write — invalidates) ──────────────────────────────

    @Transactional
    public void rateEvent(Long eventId, Long bookingId, Double newRating) {
        if (newRating < 1.0 || newRating > 5.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (eventRepository.checkBookingExists(bookingId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }

        if (eventRepository.countCompletedBookingForEvent(bookingId, eventId) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is not completed or does not belong to this event");
        }

        int currentCount = event.getTotalRatings();
        double currentTotalScore = event.getRating() * currentCount;
        int newCount = currentCount + 1;
        double updatedAverage = (currentTotalScore + newRating) / newCount;

        event.setTotalRatings(newCount);
        event.setRating(updatedAverage);
        eventRepository.save(event);

        cacheEviction.evictEntityAndFeatures("event", eventId, "S2-F3", "S2-F6");
    }

    // ─── [S2-F8] Verify Event Session (write — invalidates) ───────────────────────

    @Transactional
    public Event verifyEventSession(Long eventId, Long sessionId, Long verifiedByUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getEvent().getId().equals(eventId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session does not belong to this event");
        }

        if (session.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot verify a session that has already started");
        }

        if (eventRepository.checkUserIsAdmin(verifiedByUserId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not authorized (Must be ADMIN)");
        }

        session.setVerified(true);
        if (session.getMetadata() == null) session.setMetadata(new HashMap<>());
        session.getMetadata().put("verifiedAt", LocalDateTime.now().toString());
        session.getMetadata().put("verifiedBy", verifiedByUserId);
        eventSessionRepository.save(session);

        cacheEviction.evictEntityAndFeatures("event-session", sessionId, "S2-F9");

        return event;
    }

    // ─── [S2-F9] Get Events with Unverified Sessions ─────────────────────────────

    @Cacheable(cacheNames = "event-service::S2-F9", key = "'all'")
    public List<EventSessionAlertDTO> getUnverifiedSessionsReport() {
        List<Event> events = eventRepository.findEventsWithUnverifiedSessions();

        return events.stream().map(event -> {
            List<EventSession> unverifiedSessions =
                    event.getEventSessions().stream()
                            .filter(session -> session.getVerified() != null && !session.getVerified())
                            .toList();

            return new EventSessionAlertDTO(
                    event.getId(),
                    event.getName(),
                    event.getStatus(),
                    unverifiedSessions,
                    unverifiedSessions.size()
            );
        }).toList();
    }

    // ─── CRUD: Event ──────────────────────────────────────────────────────────────

    public Event createEvent(Event event) {
        Event saved = eventRepository.save(event);
        // New entity — no detail to evict; invalidate search/filter results
        cacheEviction.evictPattern("S2-F1");
        cacheEviction.evictPattern("S2-F10"); // ES index may be stale (§4.5 auto-index retrofit)
        return saved;
    }

    @Cacheable(cacheNames = "event-service::event", key = "#id")
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found with id: " + id));
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll(); // list — NOT cached
    }

    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
        cacheEviction.evictEntityAndFeatures("event", id, "S2-F1", "S2-F3", "S2-F5", "S2-F6", "S2-F9", "S2-F10");
    }

    // ─── CRUD: EventSession ───────────────────────────────────────────────────────

    public EventSession createEventSession(Long eventId, EventSession session) {
        Event event = getEventById(eventId);
        session.setEvent(event);
        return eventSessionRepository.save(session);
    }

    @Cacheable(cacheNames = "event-service::event-session", key = "#sessionId")
    public EventSession getEventSessionById(Long sessionId) {
        return eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found with id: " + sessionId));
    }

    public void deleteEventSession(Long sessionId) {
        if (!eventSessionRepository.existsById(sessionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found with id: " + sessionId);
        }
        eventSessionRepository.deleteById(sessionId);
        cacheEviction.evictEntityAndFeatures("event-session", sessionId, "S2-F9");
    }
}
