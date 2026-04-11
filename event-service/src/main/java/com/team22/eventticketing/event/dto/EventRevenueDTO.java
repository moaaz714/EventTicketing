package com.team22.eventticketing.event.dto;

public class EventRevenueDTO {

    private Long eventId;
    private String name;
    private long totalBookings;
    private double totalRevenue;
    private double averageBookingAmount;

    public EventRevenueDTO(Long eventId, String name,
                           long totalBookings,
                           double totalRevenue,
                           double averageBookingAmount) {
        this.eventId = eventId;
        this.name = name;
        this.totalBookings = totalBookings;
        this.totalRevenue = totalRevenue;
        this.averageBookingAmount = averageBookingAmount;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public double getAverageBookingAmount() {
        return averageBookingAmount;
    }
// getters
}