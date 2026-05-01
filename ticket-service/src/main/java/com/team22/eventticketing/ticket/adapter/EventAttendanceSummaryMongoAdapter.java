package com.team22.eventticketing.ticket.adapter;

import com.team22.eventticketing.ticket.model.dto.EventAttendanceSummaryDTO;
import org.bson.Document;

import java.time.LocalDateTime;

public class EventAttendanceSummaryMongoAdapter {
    public EventAttendanceSummaryDTO adaptToEventAttendanceSummaryDTO(Document doc) {

        return EventAttendanceSummaryDTO.builder()
                .eventId(doc.getLong("eventId"))
                .totalTickets(doc.getLong("totalTickets"))
                .usedTickets(doc.getLong("usedTickets"))
                .validTickets(doc.getLong("validTickets"))
                .lastCheckIn(doc.get("lastCheckIn", LocalDateTime.class))
                .build();
    }
}
