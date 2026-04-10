package com.team22.eventticketing.booking.dto;

public class BookingCostEstimateDTO {
    private Double ticketCost;
    private Double serviceFee;
    private Double estimatedTotal;
    private Double demandMultiplier;

    public BookingCostEstimateDTO(Double ticketCost, Double serviceFee, Double estimatedTotal, Double demandMultiplier) {
        this.ticketCost = ticketCost;
        this.serviceFee = serviceFee;
        this.estimatedTotal = estimatedTotal;
        this.demandMultiplier = demandMultiplier;
    }

    // TODO: Add standard Getters and Setters


    public Double getDemandMultiplier() {
        return demandMultiplier;
    }

    public void setDemandMultiplier(Double demandMultiplier) {
        this.demandMultiplier = demandMultiplier;
    }

    public Double getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(Double estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }

    public Double getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(Double serviceFee) {
        this.serviceFee = serviceFee;
    }

    public Double getTicketCost() {
        return ticketCost;
    }

    public void setTicketCost(Double ticketCost) {
        this.ticketCost = ticketCost;
    }
}