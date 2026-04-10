package com.team22.eventticketing.event.dto;

public class TopEventDTO {

    private Long eventId;
    private String name;
    private Double rating;
    private Long totalBookings;

    public TopEventDTO() {
    }

    public TopEventDTO(Long eventId, String name, Double rating, Long totalBookings) {
        this.eventId = eventId;
        this.name = name;
        this.rating = rating;
        this.totalBookings = totalBookings;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(Long totalBookings) {
        this.totalBookings = totalBookings;
    }
}