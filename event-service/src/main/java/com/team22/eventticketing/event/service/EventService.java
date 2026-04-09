package com.team22.eventticketing.event.service;

import com.team22.eventticketing.event.dto.UpdateEventStatusRequest;
import com.team22.eventticketing.event.model.Event;
import com.team22.eventticketing.event.model.EventStatus;
import com.team22.eventticketing.event.repository.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public void updateEventStatus(Long eventId, UpdateEventStatusRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found with id: " + eventId
                ));

        if (request == null || request.getStatus() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status is required"
            );
        }

        EventStatus newStatus = request.getStatus();

        if (newStatus == EventStatus.CANCELLED &&
                eventRepository.existsActiveBookingsForEvent(eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot cancel event because active bookings exist for this event"
            );
        }

        event.setStatus(newStatus);
        eventRepository.save(event);
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