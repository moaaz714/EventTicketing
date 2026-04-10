package com.team22.eventticketing.booking.dto;

import java.util.List;
import java.util.Map;

public class BookingDetailsDTO {
    private Long bookingId;
    private Long userId;
    private Long eventId;
    private String status;
    private Double totalAmount;
    private Map<String, Object> metadata;
    private List<BookingItemDTO> items;
    private int totalItems;
    private long confirmedItems;

    public BookingDetailsDTO(Long bookingId, Long userId, Long eventId,
                             String status, Double totalAmount,
                             Map<String, Object> metadata,
                             List<BookingItemDTO> items,
                             int totalItems, long confirmedItems) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.eventId = eventId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.metadata = metadata;
        this.items = items;
        this.totalItems = totalItems;
        this.confirmedItems = confirmedItems;
    }

    public Long getBookingId() { return bookingId; }
    public Long getUserId() { return userId; }
    public Long getEventId() { return eventId; }
    public String getStatus() { return status; }
    public Double getTotalAmount() { return totalAmount; }
    public Map<String, Object> getMetadata() { return metadata; }
    public List<BookingItemDTO> getItems() { return items; }
    public int getTotalItems() { return totalItems; }
    public long getConfirmedItems() { return confirmedItems; }
}