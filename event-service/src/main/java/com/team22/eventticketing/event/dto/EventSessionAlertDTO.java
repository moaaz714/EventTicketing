package com.team22.eventticketing.event.dto;

import com.team22.eventticketing.event.model.EventSession;
import com.team22.eventticketing.event.model.EventStatus;
import java.util.List;

public record EventSessionAlertDTO(
        Long eventId,
        String eventName,
        EventStatus eventStatus,
        List<EventSession> unverifiedSessions,
        Integer unverifiedCount
) {}