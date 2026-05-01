package com.team22.eventticketing.ticket.adapter;

import com.team22.eventticketing.ticket.model.dto.*;
import org.bson.Document;

import java.time.LocalDateTime;

public class MongoDocumentAdapter {

    public EventAttendanceSummaryDTO adaptToEventAttendanceSummaryDTO(Document doc) {

        return EventAttendanceSummaryDTO.builder()
                .eventId(doc.getLong("eventId"))
                .totalTickets(doc.getLong("totalTickets"))
                .usedTickets(doc.getLong("usedTickets"))
                .validTickets(doc.getLong("validTickets"))
                .lastCheckIn(doc.get("lastCheckIn", LocalDateTime.class))
                .build();
    }
    public EventAttendanceSummaryDTO adapt(Document doc) {
        return adaptToEventAttendanceSummaryDTO(doc);
    }
    public UnusedTicketDTO adapt(Document doc) {
        return adaptToUnusedTicketDTO(doc);
    }


    public UnusedTicketDTO adaptToUnusedTicketDTO(Document doc) {

        return UnusedTicketDTO.builder()
                .ticketId(doc.getLong("ticketId"))
                .attendeeName(doc.getString("attendeeName"))
                .ticketCode(doc.getString("ticketCode"))
                .bookingId(doc.getLong("bookingId"))
                .eventName(doc.getString("eventName"))
                .eventDate(doc.get("eventDate", LocalDateTime.class))
                .build();
    }
}