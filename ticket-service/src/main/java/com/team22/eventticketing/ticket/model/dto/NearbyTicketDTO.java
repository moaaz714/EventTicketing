package com.team22.eventticketing.ticket.model.dto;

public class NearbyTicketDTO {

    private Long ticketId;
    private String attendeeName;
    private Long bookingId;
    private String eventName;
    private Double eventLat;
    private Double eventLon;
    private Double distanceKm;

    public NearbyTicketDTO(Long ticketId, String attendeeName, Long bookingId,
                           String eventName, Double eventLat, Double eventLon,
                           Double distanceKm) {
        this.ticketId = ticketId;
        this.attendeeName = attendeeName;
        this.bookingId = bookingId;
        this.eventName = eventName;
        this.eventLat = eventLat;
        this.eventLon = eventLon;
        this.distanceKm = distanceKm;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public String getAttendeeName() {
        return attendeeName;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public String getEventName() {
        return eventName;
    }

    public Double getEventLat() {
        return eventLat;
    }

    public Double getEventLon() {
        return eventLon;
    }
}