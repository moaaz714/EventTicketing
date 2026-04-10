package com.team22.eventticketing.event.controller;
import com.team22.eventticketing.event.dto.TopEventDTO;


import com.team22.eventticketing.event.dto.EventRatingRequestDTO;
import com.team22.eventticketing.event.dto.UpdateEventStatusRequest;
import com.team22.eventticketing.event.model.Event;
import com.team22.eventticketing.event.model.EventCategory;
import com.team22.eventticketing.event.service.EventService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.time.LocalDateTime;
import java.util.List;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class eventController {

    private final EventService eventService;

    public eventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/health")
    public String statusCheck() {
        return "OK";
    }

    // [S2-F4] Update Event Status
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateEventStatus(
            @PathVariable Long id,
            @RequestBody UpdateEventStatusRequest request
    ) {
        eventService.updateEventStatus(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/details/search")
    public ResponseEntity<List<Event>> filterEvents(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(eventService.filterEventsByDetail(key, value, status));
    }

    // [S2-F7] Rate an Event
    @PostMapping("/{id}/rate")
    public ResponseEntity<Void> rateEvent(@PathVariable Long id, @RequestBody EventRatingRequestDTO request) {
        eventService.rateEvent(id, request.bookingId(), request.rating());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/details")
    public ResponseEntity<Void> updateEventDetails(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        eventService.updateEventDetails(id, request);
        return ResponseEntity.ok().build();
    }
    // [S2-F8] Verify Event Session

    @PutMapping("/{eventId}/sessions/{sessionId}/verify")
    public ResponseEntity<Event> verifySession(
            @PathVariable Long eventId,
            @PathVariable Long sessionId,
            @RequestBody com.team22.eventticketing.event.dto.VerifySessionRequestDTO request) {

        Event updatedEvent = eventService.verifyEventSession(eventId, sessionId, request.verifiedBy());
        return ResponseEntity.ok(updatedEvent);
    }
    @GetMapping("/search")
    public ResponseEntity<List<Event>> searchEvents(
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {

        return ResponseEntity.ok(
                eventService.searchEvents(category, startDate, endDate)
        );


    }

    @GetMapping("/test")
    public String test() {
        return "CONTROLLER OK";
    }

    // [S2-F9] Get Events with Unverified Sessions Report
    @GetMapping("/sessions/unverified")
    public ResponseEntity<java.util.List<com.team22.eventticketing.event.dto.EventSessionAlertDTO>> getUnverifiedSessions() {
        return ResponseEntity.ok(eventService.getUnverifiedSessionsReport());
    }
    @GetMapping("/reports/top-rated")
    public ResponseEntity<List<TopEventDTO>> getTopRatedEvents(@RequestParam int limit) {
        return ResponseEntity.ok(eventService.getTopRatedEvents(limit));
    }


    @PostConstruct
    public void init() {
        System.out.println("EVENT CONTROLLER LOADED");
    }

//    @PutMapping("/{id}/details")
//    public ResponseEntity<Event> updateEventDetails(
//            @PathVariable Long id,
//            @RequestBody Map details) {
//        return ResponseEntity.ok(eventService.updateEventDetails(id, details));
//    }
}