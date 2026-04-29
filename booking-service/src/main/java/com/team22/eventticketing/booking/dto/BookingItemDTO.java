package com.team22.eventticketing.booking.dto;

import java.util.List;
import java.util.Map;

public class BookingItemDTO {
    private Long id;
    private Integer eventOrder;
    private String sessionTitle;
    private Integer quantity;
    private Double unitPrice;
    private String status;
    private Map<String, Object> metadata;

//    public BookingItemDTO(Long id, Integer eventOrder, String sessionTitle,
//                          Integer quantity, Double unitPrice, String status,
//                          Map<String, Object> metadata) {
//        this.id = id;
//        this.eventOrder = eventOrder;
//        this.sessionTitle = sessionTitle;
//        this.quantity = quantity;
//        this.unitPrice = unitPrice;
//        this.status = status;
//        this.metadata = metadata;
//    }

    public BookingItemDTO(Builder builder){
        this.id = builder.id;
        this.eventOrder = builder.eventOrder;
        this.sessionTitle = builder.sessionTitle;
        this.quantity = builder.quantity;
        this.unitPrice = builder.unitPrice;
        this.status = builder.status;
        this.metadata = builder.metadata;

    }

    public static Builder builder(){return new Builder();}

    public static class Builder{
        private Long id;
        private Integer eventOrder;
        private String sessionTitle;
        private Integer quantity;
        private Double unitPrice;
        private String status;
        private Map<String, Object> metadata;

        public Builder id(Long id){this.id=id;return this;}
        public Builder eventOrder(Integer eventOrder){this.eventOrder=eventOrder;return this;}
        public Builder sessionTitle(String sessionTitle){this.sessionTitle=sessionTitle;return this;}
        public Builder quantity(Integer quantity){this.quantity=quantity;return this;}
        public Builder unitPrice(Double unitPrice){this.unitPrice=unitPrice;return this;}
        public Builder status(String status){this.status=status;return this;}
        public Builder metadata(Map<String, Object> metadata){this.metadata=metadata;return this;}



        public BookingItemDTO build() {
            return new BookingItemDTO(this);
        }

    }

    public Long getId() { return id; }
    public Integer getEventOrder() { return eventOrder; }
    public String getSessionTitle() { return sessionTitle; }
    public Integer getQuantity() { return quantity; }
    public Double getUnitPrice() { return unitPrice; }
    public String getStatus() { return status; }
    public Map<String, Object> getMetadata() { return metadata; }
}