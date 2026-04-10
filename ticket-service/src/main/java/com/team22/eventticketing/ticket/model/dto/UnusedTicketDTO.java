package com.team22.eventticketing.ticket.model.dto;

import java.time.LocalDateTime;

public class UnusedTicketDTO {
    private Long ticketId;
    private String attendeeName;
    private String ticketCode;
    private Long bookingId;
    private String eventName;
    private LocalDateTime eventDate;

    public UnusedTicketDTO(Long ticketId, String attendeeName, String ticketCode,
            Long bookingId, String eventName, LocalDateTime eventDate) {
        this.ticketId = ticketId;
        this.attendeeName = attendeeName;
        this.ticketCode = ticketCode;
        this.bookingId = bookingId;
        this.eventName = eventName;
        this.eventDate = eventDate;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public String getAttendeeName() {
        return attendeeName;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public String getEventName() {
        return eventName;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

}