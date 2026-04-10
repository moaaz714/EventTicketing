package com.team22.eventticketing.event.controller;

import com.team22.eventticketing.event.dto.EventRatingRequestDTO;
import com.team22.eventticketing.event.model.Event;
import com.team22.eventticketing.event.model.EventCategory;
import com.team22.eventticketing.event.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    // [S2-F7] Rate an Event
    @PostMapping("/{id}/rate")
    public ResponseEntity<Void> rateEvent(@PathVariable Long id, @RequestBody EventRatingRequestDTO request) {
        eventService.rateEvent(id, request.bookingId(), request.rating());
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
}