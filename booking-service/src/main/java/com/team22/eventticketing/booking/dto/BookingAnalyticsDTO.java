package com.team22.eventticketing.booking.dto;

public class BookingAnalyticsDTO {
    private long totalBookings;
    private long completedBookings;
    private long cancelledBookings;
    private double totalRevenue;
    private double averageBookingAmount;
    private double completionRate;

    public BookingAnalyticsDTO(long totalBookings, long completedBookings,
                               long cancelledBookings, double totalRevenue,
                               double averageBookingAmount, double completionRate) {
        this.totalBookings = totalBookings;
        this.completedBookings = completedBookings;
        this.cancelledBookings = cancelledBookings;
        this.totalRevenue = totalRevenue;
        this.averageBookingAmount = averageBookingAmount;
        this.completionRate = completionRate;
    }

    public long getTotalBookings() { return totalBookings; }
    public long getCompletedBookings() { return completedBookings; }
    public long getCancelledBookings() { return cancelledBookings; }
    public double getTotalRevenue() { return totalRevenue; }
    public double getAverageBookingAmount() { return averageBookingAmount; }
    public double getCompletionRate() { return completionRate; }
}