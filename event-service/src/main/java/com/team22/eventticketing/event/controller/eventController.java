package com.team22.eventticketing.event.controller;


import com.team22.eventticketing.event.dto.UpdateEventStatusRequest;
import com.team22.eventticketing.event.model.Event;
import com.team22.eventticketing.event.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}