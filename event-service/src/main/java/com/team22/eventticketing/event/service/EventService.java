package com.team22.eventticketing.event.service;
import com.team22.eventticketing.event.dto.TopEventDTO;

import com.team22.eventticketing.event.dto.UpdateEventStatusRequest;
import com.team22.eventticketing.event.model.Event;
import com.team22.eventticketing.event.model.EventStatus;
import com.team22.eventticketing.event.repository.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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

    public List<Event> filterEventsByDetail(String key, String value) {

        if (key == null || key.isBlank() || value == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Key and value are required"
            );
        }

        return eventRepository.findByDetailAttribute(key, value);
    }
    public List<TopEventDTO> getTopRatedEvents(int limit) {
        if (limit <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Limit must be greater than 0"
            );
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
}