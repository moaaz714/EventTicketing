package com.team22.eventticketing.event.service;

import com.team22.eventticketing.event.dto.EventRevenueDTO;
import com.team22.eventticketing.event.dto.EventSessionAlertDTO;
import com.team22.eventticketing.event.dto.TopEventDTO;
import com.team22.eventticketing.event.dto.UpdateEventStatusRequest;
import com.team22.eventticketing.event.model.Event;
import com.team22.eventticketing.event.model.EventCategory;
import com.team22.eventticketing.event.model.EventSearchDocument;
import com.team22.eventticketing.event.model.EventSession;
import com.team22.eventticketing.event.model.EventStatus;
import com.team22.eventticketing.event.observer.EntityObserver;
import com.team22.eventticketing.event.repository.EventRepository;
import com.team22.eventticketing.event.repository.EventSearchRepository;
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
    private final EventSearchRepository eventSearchRepository;
    private final EventSessionRepository eventSessionRepository;
    private final CacheEvictionService cacheEviction;
    private final List<EntityObserver> observers = new ArrayList<>();

    public EventService(EventRepository eventRepository,
                        EventSearchRepository eventSearchRepository,
                        EventSessionRepository eventSessionRepository,
                        CacheEvictionService cacheEviction) {
        this.eventRepository = eventRepository;
        this.eventSearchRepository = eventSearchRepository;
        this.eventSessionRepository = eventSessionRepository;
        this.cacheEviction = cacheEviction;
    }

    public void register(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String action, Object payload) {
        for (EntityObserver o : observers) {
            o.onEvent(action, payload);
        }
    }

//    // ─── TEMP ONLY: Reindex PostgreSQL Events into Elasticsearch ─────────────────
//    // Use this only for local testing before S2-F11 exists.
//    // Do NOT commit/push this temporary method if it is not part of your assigned feature.
//
//    public int tempReindexEventsToElasticsearch() {
//        List<Event> events = eventRepository.findAll();
//        int indexedCount = 0;
//
//        String elasticsearchBaseUrl = "http://elasticsearch:9200";
//
//        try {
//            restTemplate.delete(elasticsearchBaseUrl + "/events");
//        } catch (Exception ignored) {
//            // Index may not exist. That is fine for this temporary helper.
//        }
//
//        Map<String, Object> mapping = new HashMap<>();
//        Map<String, Object> properties = new HashMap<>();
//
//        properties.put("id", Map.of("type", "keyword"));
//        properties.put("name", Map.of("type", "text"));
//        properties.put("description", Map.of("type", "text"));
//        properties.put("venue", Map.of("type", "text"));
//        properties.put("category", Map.of("type", "keyword"));
//        properties.put("status", Map.of("type", "keyword"));
//        properties.put("eventDate", Map.of("type", "date"));
//        properties.put("rating", Map.of("type", "double"));
//
//        mapping.put("mappings", Map.of("properties", properties));
//
//        restTemplate.put(elasticsearchBaseUrl + "/events", mapping);
//
//        for (Event event : events) {
//            if (event == null || event.getId() == null) {
//                continue;
//            }
//
//            Map<String, Object> document = new HashMap<>();
//
//            document.put("id", String.valueOf(event.getId()));
//            document.put("name", event.getName() == null ? "" : event.getName());
//
//            Map<String, Object> details = event.getDetails();
//            Object description = details == null ? null : details.get("description");
//            document.put("description", description == null ? "" : String.valueOf(description));
//
//            document.put("venue", event.getVenue() == null ? "" : event.getVenue());
//
//            if (event.getCategory() != null) {
//                document.put("category", event.getCategory().name());
//            }
//
//            if (event.getStatus() != null) {
//                document.put("status", event.getStatus().name());
//            }
//
//            if (event.getEventDate() != null) {
//                document.put("eventDate", event.getEventDate().toString());
//            }
//
//            if (event.getRating() != null) {
//                document.put("rating", event.getRating());
//            }
//
//            restTemplate.put(
//                    elasticsearchBaseUrl + "/events/_doc/" + event.getId(),
//                    document
//            );
//
//            indexedCount++;
//        }
//
//        cacheEviction.evictPattern("S2-F10");
//
//        return indexedCount;
//    }

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

    public Event updateEventDetails(Long id, Map<String, Object> newDetails) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        Map<String, Object> existingDetails = event.getDetails();
        if (existingDetails == null) {
            existingDetails = new HashMap<>();
        }

        existingDetails.putAll(newDetails);
        event.setDetails(existingDetails);

        Event saved = eventRepository.save(event);

        cacheEviction.evictEntityAndFeatures("event", id, "S2-F1", "S2-F3", "S2-F5", "S2-F6", "S2-F9", "S2-F10");
        notifyObservers("EVENT_DETAILS_UPDATED", Map.of("eventId", id));

        return saved;
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
        notifyObservers("EVENT_STATUS_UPDATED", Map.of("eventId", eventId, "status", newStatus.name()));
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

            result.add(TopEventDTO.builder()
                    .eventId(eventId)
                    .name(name)
                    .rating(rating)
                    .totalBookings(totalBookings)
                    .build());
        }

        return result;
    }

    // ─── [S2-F3] Get Event Revenue DTO ───────────────────────────────────────────

    @Cacheable(cacheNames = "event-service::S2-F3",
            key = "#eventId + ':' + T(String).valueOf(#start) + ':' + T(String).valueOf(#end)")
    public EventRevenueDTO getEventRevenue(Long eventId, LocalDateTime start, LocalDateTime end) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        List<Object[]> stats = eventRepository.getEventRevenueStats(eventId);

        if (stats != null && !stats.isEmpty() && stats.get(0) != null) {
            Object[] row = stats.get(0);
            long totalBookings = row[0] == null ? 0L : ((Number) row[0]).longValue();
            double totalRevenue = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            double avg = row[2] == null ? 0.0 : ((Number) row[2]).doubleValue();

            return EventRevenueDTO.builder()
                    .eventId(eventId)
                    .name(event.getName())
                    .totalBookings(totalBookings)
                    .totalRevenue(totalRevenue)
                    .averageBookingAmount(avg)
                    .build();
        }

        return EventRevenueDTO.builder()
                .eventId(eventId)
                .name(event.getName())
                .totalBookings(0L)
                .totalRevenue(0.0)
                .averageBookingAmount(0.0)
                .build();
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
        notifyObservers("EVENT_RATED", Map.of("eventId", eventId, "rating", newRating));
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

        if (session.getMetadata() == null) {
            session.setMetadata(new HashMap<>());
        }

        session.getMetadata().put("verifiedAt", LocalDateTime.now().toString());
        session.getMetadata().put("verifiedBy", verifiedByUserId);

        eventSessionRepository.save(session);

        notifyObservers("SESSION_VERIFIED", Map.of("eventId", eventId, "sessionId", sessionId));
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

            return EventSessionAlertDTO.builder()
                    .eventId(event.getId())
                    .eventName(event.getName())
                    .eventStatus(event.getStatus())
                    .unverifiedSessions(unverifiedSessions)
                    .unverifiedCount(unverifiedSessions.size())
                    .build();
        }).toList();
    }

    // ─── CRUD: Event ──────────────────────────────────────────────────────────────

    public Event createEvent(Event event) {
        Event saved = eventRepository.save(event);

        cacheEviction.evictPattern("S2-F1");
        cacheEviction.evictPattern("S2-F10");

        notifyObservers("EVENT_CREATED", Map.of("eventId", saved.getId()));

        return saved;
    }

    @Cacheable(cacheNames = "event-service::event", key = "#id")
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found with id: " + id));
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event updateEvent(Long id, Event updatedEvent) {
        Event existingEvent = getEventById(id);

        existingEvent.setName(updatedEvent.getName());
        existingEvent.setVenue(updatedEvent.getVenue());
        existingEvent.setEventDate(updatedEvent.getEventDate());
        existingEvent.setCategory(updatedEvent.getCategory());

        Event saved = eventRepository.save(existingEvent);

        cacheEviction.evictEntityAndFeatures("event", id, "S2-F1", "S2-F3", "S2-F5", "S2-F6", "S2-F9", "S2-F10");

        return saved;
    }

    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found with id: " + id);
        }

        eventRepository.deleteById(id);

        cacheEviction.evictEntityAndFeatures("event", id, "S2-F1", "S2-F3", "S2-F5", "S2-F6", "S2-F9", "S2-F10");
        notifyObservers("EVENT_DELETED", Map.of("eventId", id));
    }

    // ─── CRUD: EventSession ───────────────────────────────────────────────────────

    public EventSession createEventSession(Long eventId, EventSession session) {
        Event event = getEventById(eventId);

        session.setEvent(event);

        EventSession saved = eventSessionRepository.save(session);

        notifyObservers("SESSION_CREATED", Map.of("eventId", eventId, "sessionId", saved.getId()));

        return saved;
    }

    @Cacheable(cacheNames = "event-service::event-session", key = "#sessionId")
    public EventSession getEventSessionById(Long sessionId) {
        return eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found with id: " + sessionId));
    }

    public List<EventSession> getSessionsForEvent(Long eventId) {
        Event event = getEventById(eventId);
        return event.getEventSessions();
    }

    public EventSession getEventSessionById(Long eventId, Long sessionId) {
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getEvent().getId().equals(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session does not belong to this event");
        }

        return session;
    }

    public EventSession updateEventSession(Long eventId, Long sessionId, EventSession updatedSession) {
        EventSession existingSession = getEventSessionById(eventId, sessionId);

        existingSession.setTitle(updatedSession.getTitle());
        existingSession.setSpeaker(updatedSession.getSpeaker());
        existingSession.setStartTime(updatedSession.getStartTime());
        existingSession.setEndTime(updatedSession.getEndTime());
        existingSession.setCapacity(updatedSession.getCapacity());

        return eventSessionRepository.save(existingSession);
    }

    public void deleteEventSession(Long eventId, Long sessionId) {
        getEventSessionById(eventId, sessionId);

        eventSessionRepository.deleteById(sessionId);

        cacheEviction.evictEntityAndFeatures("event-session", sessionId, "S2-F9");
        notifyObservers("SESSION_DELETED", Map.of("sessionId", sessionId));
    }
}