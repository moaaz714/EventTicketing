package com.team22.eventticketing.user.dto;

public class UserBookingSummaryDTO {

    private Long userId;
    private String name;
    private Long totalBookings;
    private Long completedBookings;
    private Long cancelledBookings;
    private Double totalSpent;
    private Double averageBookingAmount;

    public UserBookingSummaryDTO(Long userId, String name, Long totalBookings,
                                 Long completedBookings, Long cancelledBookings,
                                 Double totalSpent, Double averageBookingAmount) {
        this.userId = userId;
        this.name = name;
        this.totalBookings = totalBookings;
        this.completedBookings = completedBookings;
        this.cancelledBookings = cancelledBookings;
        this.totalSpent = totalSpent;
        this.averageBookingAmount = averageBookingAmount;
    }

    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public Long getTotalBookings() { return totalBookings; }
    public Long getCompletedBookings() { return completedBookings; }
    public Long getCancelledBookings() { return cancelledBookings; }
    public Double getTotalSpent() { return totalSpent; }
    public Double getAverageBookingAmount() { return averageBookingAmount; }
}