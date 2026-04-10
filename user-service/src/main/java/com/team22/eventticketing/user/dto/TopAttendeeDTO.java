package com.team22.eventticketing.user.dto;

public class TopAttendeeDTO {

    private Long userId;
    private String name;
    private Double totalSpent;
    private Long bookingCount;

    public TopAttendeeDTO(Long userId, String name, Double totalSpent, Long bookingCount) {
        this.userId = userId;
        this.name = name;
        this.totalSpent = totalSpent;
        this.bookingCount = bookingCount;
    }

    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public Double getTotalSpent() { return totalSpent; }
    public Long getBookingCount() { return bookingCount; }
}
