package com.team22.eventticketing.sales.dto;

public class RevenueReportDTO {
    private double totalRevenue;
    private int totalTransactions;
    private double averageSale;
    private double refundedAmount;
    private int refundCount;

    public RevenueReportDTO(double totalRevenue, int totalTransactions, double averageSale,
                            double refundedAmount, int refundCount) {
        this.totalRevenue = totalRevenue;
        this.totalTransactions = totalTransactions;
        this.averageSale = averageSale;
        this.refundedAmount = refundedAmount;
        this.refundCount = refundCount;
    }

    // Getters & setters
    public double getTotalRevenue() { return totalRevenue; }
    public int getTotalTransactions() { return totalTransactions; }
    public double getAverageSale() { return averageSale; }
    public double getRefundedAmount() { return refundedAmount; }
    public int getRefundCount() { return refundCount; }
}