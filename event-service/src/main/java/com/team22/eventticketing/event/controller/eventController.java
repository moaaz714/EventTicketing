package com.team22.eventticketing.event.controller;

import com.team22.eventticketing.event.dto.EventRatingRequestDTO;
import com.team22.eventticketing.event.dto.UpdateEventStatusRequest;
import com.team22.eventticketing.event.model.Event;
import com.team22.eventticketing.event.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // [S2-F4] Update Event Status (Teammate's Feature)
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateEventStatus(
            @PathVariable Long id,
            @RequestBody UpdateEventStatusRequest request
    ) {
        eventService.updateEventStatus(id, request);
        return ResponseEntity.ok().build();
    }

    // [S2-F7] Rate an Event (Your Feature)
    @PostMapping("/{id}/rate")
    public ResponseEntity<Void> rateEvent(@PathVariable Long id, @RequestBody EventRatingRequestDTO request) {
        eventService.rateEvent(id, request.bookingId(), request.rating());
        return ResponseEntity.ok().build();
    }

    // [S2-F8] Verify Event Session (Your Feature)
    @PutMapping("/{eventId}/sessions/{sessionId}/verify")
    public ResponseEntity<Event> verifySession(
            @PathVariable Long eventId,
            @PathVariable Long sessionId,
            @RequestBody com.team22.eventticketing.event.dto.VerifySessionRequestDTO request) {

        Event updatedEvent = eventService.verifyEventSession(eventId, sessionId, request.verifiedBy());
        return ResponseEntity.ok(updatedEvent);
    }

    // [S2-F9] Get Events with Unverified Sessions Report (Your Feature)
    @GetMapping("/sessions/unverified")
    public ResponseEntity<java.util.List<com.team22.eventticketing.event.dto.EventSessionAlertDTO>> getUnverifiedSessions() {
        return ResponseEntity.ok(eventService.getUnverifiedSessionsReport());
    }
}