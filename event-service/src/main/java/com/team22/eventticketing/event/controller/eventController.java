package com.team22.eventticketing.event.controller;

import com.team22.eventticketing.event.dto.EventDashboardDTO;
import com.team22.eventticketing.event.dto.EventRatingRequestDTO;
import com.team22.eventticketing.event.dto.EventRevenueDTO;
import com.team22.eventticketing.event.dto.EventSessionAlertDTO;
import com.team22.eventticketing.event.dto.TopEventDTO;
import com.team22.eventticketing.event.dto.UpdateEventStatusRequest;
import com.team22.eventticketing.event.dto.VerifySessionRequestDTO;
import com.team22.eventticketing.event.model.Event;
import com.team22.eventticketing.event.model.EventCategory;
import com.team22.eventticketing.event.model.EventSearchDocument;
import com.team22.eventticketing.event.model.EventSession;
import com.team22.eventticketing.event.service.EventFullTextSearchService;
import com.team22.eventticketing.event.service.EventService;

import dto.AvgCapacityDTO;
import dto.VenueCoordsDTO;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class eventController {

    private static final Logger log = LoggerFactory.getLogger(eventController.class);

    private final EventService eventService;
    private final EventFullTextSearchService eventFullTextSearchService;

    public eventController(EventService eventService,
                           EventFullTextSearchService eventFullTextSearchService) {
        this.eventService = eventService;
        this.eventFullTextSearchService = eventFullTextSearchService;
    }

    private void logEntry(String method, String path) {
        log.info("Received {} {}", method, path);
    }

    private void logExit(int status, String method, String path) {
        log.info("Returning {} for {} {}", status, method, path);
    }

    @GetMapping("/health")
    public String statusCheck() {
        String method = "GET";
        String path = "/api/events/health";
        logEntry(method, path);
        logExit(HttpStatus.OK.value(), method, path);
        return "OK";
    }

    // [S2-F4] Update Event Status
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateEventStatus(
            @PathVariable Long id,
            @RequestBody UpdateEventStatusRequest request
    ) {
        String method = "PUT";
        String path = "/api/events/" + id + "/status";
        logEntry(method, path);

        eventService.updateEventStatus(id, request);

        logExit(HttpStatus.OK.value(), method, path);
        return ResponseEntity.ok().build();
    }

    // [S2-F5] Filter Events by Detail
    @GetMapping("/details/search")
    public ResponseEntity<List<Event>> filterEvents(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) String status
    ) {
        String method = "GET";
        String path = "/api/events/details/search";
        logEntry(method, path);

        ResponseEntity<List<Event>> response = ResponseEntity.ok(eventService.filterEventsByDetail(key, value, status));

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    // [S2-F7] Rate an Event
    @PostMapping("/{id}/rate")
    public ResponseEntity<Void> rateEvent(@PathVariable Long id, @RequestBody EventRatingRequestDTO request) {
        String method = "POST";
        String path = "/api/events/" + id + "/rate";
        logEntry(method, path);

        eventService.rateEvent(id, request.bookingId(), request.rating());

        logExit(HttpStatus.OK.value(), method, path);
        return ResponseEntity.ok().build();
    }

    // [S2-F2] Update Event Details (JSONB Merge)
    @PutMapping("/{id}/details")
    public ResponseEntity<Event> updateEventDetails(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        String method = "PUT";
        String path = "/api/events/" + id + "/details";
        logEntry(method, path);

        Event updatedEvent = eventService.updateEventDetails(id, request);
        ResponseEntity<Event> response = ResponseEntity.ok(updatedEvent);

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    // [S2-F8] Verify Event Session
    @PutMapping("/{eventId}/sessions/{sessionId}/verify")
    public ResponseEntity<Event> verifySession(
            @PathVariable Long eventId,
            @PathVariable Long sessionId,
            @RequestBody VerifySessionRequestDTO request
    ) {
        String method = "PUT";
        String path = "/api/events/" + eventId + "/sessions/" + sessionId + "/verify";
        logEntry(method, path);

        Event updatedEvent = eventService.verifyEventSession(eventId, sessionId, request.verifiedBy());
        ResponseEntity<Event> response = ResponseEntity.ok(updatedEvent);

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    // [S2-F10] Full-Text Event Search
    @GetMapping("/search/full-text")
    public ResponseEntity<List<EventSearchDocument>> fullTextSearchEvents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating
    ) {
        String method = "GET";
        String path = "/api/events/search/full-text";
        logEntry(method, path);

        LocalDateTime start = null;
        LocalDateTime end = null;

        if (startDate != null && !startDate.isBlank()) {
            start = LocalDateTime.parse(startDate + "T00:00:00");
        }

        if (endDate != null && !endDate.isBlank()) {
            end = LocalDateTime.parse(endDate + "T23:59:59");
        }

        ResponseEntity<List<EventSearchDocument>> response = ResponseEntity.ok(
                eventFullTextSearchService.fullTextSearch(
                        query,
                        category,
                        venue,
                        status,
                        start,
                        end,
                        minRating,
                        maxRating
                )
        );

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    // [S2-F1] Search Events
    @GetMapping("/search")
    public ResponseEntity<List<Event>> searchEvents(
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        String method = "GET";
        String path = "/api/events/search";
        logEntry(method, path);

        LocalDateTime start = null;
        LocalDateTime end = null;

        if (startDate != null && !startDate.isBlank()) {
            start = LocalDateTime.parse(startDate + "T00:00:00");
        }

        if (endDate != null && !endDate.isBlank()) {
            end = LocalDateTime.parse(endDate + "T23:59:59");
        }

        ResponseEntity<List<Event>> response = ResponseEntity.ok(eventService.searchEvents(category, start, end));

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    // [S2-F3] Get Event Revenue
    @GetMapping("/{id}/revenue")
    public ResponseEntity<EventRevenueDTO> getRevenue(
            @PathVariable Long id,
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        String method = "GET";
        String path = "/api/events/" + id + "/revenue";
        logEntry(method, path);

        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");

        ResponseEntity<EventRevenueDTO> response = ResponseEntity.ok(eventService.getEventRevenue(id, start, end));

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    // [S2-F9] Get Events with Unverified Sessions Report
    @GetMapping("/sessions/unverified")
    public ResponseEntity<List<EventSessionAlertDTO>> getUnverifiedSessions() {
        String method = "GET";
        String path = "/api/events/sessions/unverified";
        logEntry(method, path);

        ResponseEntity<List<EventSessionAlertDTO>> response = ResponseEntity.ok(eventService.getUnverifiedSessionsReport());

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    // [S2-F6] Top Rated Events
    @GetMapping("/reports/top-rated")
    public ResponseEntity<List<TopEventDTO>> getTopRatedEvents(@RequestParam int limit) {
        String method = "GET";
        String path = "/api/events/reports/top-rated";
        logEntry(method, path);

        ResponseEntity<List<TopEventDTO>> response = ResponseEntity.ok(eventService.getTopRatedEvents(limit));

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    // CRUD ENDPOINTS: EVENT

    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        String method = "POST";
        String path = "/api/events";
        logEntry(method, path);

        Event createdEvent = eventService.createEvent(event);
        ResponseEntity<Event> response = new ResponseEntity<>(createdEvent, HttpStatus.CREATED);

        logExit(HttpStatus.CREATED.value(), method, path);
        return response;
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        String method = "GET";
        String path = "/api/events";
        logEntry(method, path);

        ResponseEntity<List<Event>> response = ResponseEntity.ok(eventService.getAllEvents());

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        String method = "GET";
        String path = "/api/events/" + id;
        logEntry(method, path);

        ResponseEntity<Event> response = ResponseEntity.ok(eventService.getEventById(id));

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody Event updatedEvent) {
        String method = "PUT";
        String path = "/api/events/" + id;
        logEntry(method, path);

        Event savedEvent = eventService.updateEvent(id, updatedEvent);
        ResponseEntity<Event> response = ResponseEntity.ok(savedEvent);

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        String method = "DELETE";
        String path = "/api/events/" + id;
        logEntry(method, path);

        eventService.deleteEvent(id);

        logExit(HttpStatus.NO_CONTENT.value(), method, path);
        return ResponseEntity.noContent().build();
    }

    // [M3 S2-READ-DB] Average session capacity for Feign consumers
    @GetMapping("/{id}/sessions/avg-capacity")
    public ResponseEntity<AvgCapacityDTO> getAverageSessionCapacity(@PathVariable Long id) {
        String method = "GET";
        String path = "/api/events/" + id + "/sessions/avg-capacity";
        logEntry(method, path);

        ResponseEntity<AvgCapacityDTO> response = ResponseEntity.ok(eventService.getAverageSessionCapacity(id));

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    // [M3 S2-READ-DB] Venue coordinates for Feign consumers
    @GetMapping("/{id}/venue-coords")
    public ResponseEntity<VenueCoordsDTO> getVenueCoordinates(@PathVariable Long id) {
        String method = "GET";
        String path = "/api/events/" + id + "/venue-coords";
        logEntry(method, path);

        ResponseEntity<VenueCoordsDTO> response = ResponseEntity.ok(eventService.getVenueCoordinates(id));

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    // CRUD ENDPOINTS: EVENT SESSION

    @PostMapping("/{eventId}/sessions")
    public ResponseEntity<EventSession> createEventSession(
            @PathVariable Long eventId,
            @RequestBody EventSession session
    ) {
        String method = "POST";
        String path = "/api/events/" + eventId + "/sessions";
        logEntry(method, path);

        EventSession createdSession = eventService.createEventSession(eventId, session);
        ResponseEntity<EventSession> response = new ResponseEntity<>(createdSession, HttpStatus.CREATED);

        logExit(HttpStatus.CREATED.value(), method, path);
        return response;
    }

    @GetMapping("/{eventId}/sessions")
    public ResponseEntity<List<EventSession>> getSessionsForEvent(@PathVariable Long eventId) {
        String method = "GET";
        String path = "/api/events/" + eventId + "/sessions";
        logEntry(method, path);

        ResponseEntity<List<EventSession>> response = ResponseEntity.ok(eventService.getSessionsForEvent(eventId));

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    @GetMapping("/{eventId}/sessions/{sessionId}")
    public ResponseEntity<EventSession> getEventSessionById(
            @PathVariable Long eventId,
            @PathVariable Long sessionId
    ) {
        String method = "GET";
        String path = "/api/events/" + eventId + "/sessions/" + sessionId;
        logEntry(method, path);

        ResponseEntity<EventSession> response = ResponseEntity.ok(eventService.getEventSessionById(eventId, sessionId));

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    @PutMapping("/{eventId}/sessions/{sessionId}")
    public ResponseEntity<EventSession> updateEventSession(
            @PathVariable Long eventId,
            @PathVariable Long sessionId,
            @RequestBody EventSession updatedSession
    ) {
        String method = "PUT";
        String path = "/api/events/" + eventId + "/sessions/" + sessionId;
        logEntry(method, path);

        ResponseEntity<EventSession> response = ResponseEntity.ok(eventService.updateEventSession(eventId, sessionId, updatedSession));

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }

    @DeleteMapping("/{eventId}/sessions/{sessionId}")
    public ResponseEntity<Void> deleteEventSession(
            @PathVariable Long eventId,
            @PathVariable Long sessionId
    ) {
        String method = "DELETE";
        String path = "/api/events/" + eventId + "/sessions/" + sessionId;
        logEntry(method, path);

        eventService.deleteEventSession(eventId, sessionId);

        logExit(HttpStatus.NO_CONTENT.value(), method, path);
        return ResponseEntity.noContent().build();
    }

    @PostConstruct
    public void init() {
        log.info("EVENT CONTROLLER LOADED");
    }

    // [S2-F11] Index Event for Search
    @PostMapping("/{id}/index")
    public ResponseEntity<Void> indexEventExplicitly(@PathVariable Long id) {
        String method = "POST";
        String path = "/api/events/" + id + "/index";
        logEntry(method, path);

        eventService.explicitIndexEvent(id);

        logExit(HttpStatus.OK.value(), method, path);
        return ResponseEntity.ok().build();
    }

    // [S2-F12] Event Performance Dashboard
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<EventDashboardDTO> getDashboard(@PathVariable Long id) {
        String method = "GET";
        String path = "/api/events/" + id + "/dashboard";
        logEntry(method, path);

        ResponseEntity<EventDashboardDTO> response = ResponseEntity.ok(eventService.getEventDashboard(id));

        logExit(HttpStatus.OK.value(), method, path);
        return response;
    }
}