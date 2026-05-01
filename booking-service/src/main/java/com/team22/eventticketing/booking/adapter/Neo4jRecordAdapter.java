//package com.team22.eventticketing.booking.adapter;
//
//import com.team22.eventticketing.booking.dto.BookingDetailsDTO;
//import org.neo4j.driver.Record;
//
//import java.util.Collections;
//
//public class Neo4jRecordAdapter {
//
//    public BookingDetailsDTO adaptToBookingDetailsDTO(Record record) {
//
//        Long bookingId = record.get("bookingId").asLong();
//        Long userId = record.get("userId").asLong();
//        Long eventId = record.get("eventId").asLong();
//
//        String status = record.get("status").asString();
//        Double totalAmount = record.get("totalAmount").asDouble();
//
//        int totalItems = record.get("totalItems").asInt();
//        long confirmedItems = record.get("confirmedItems").asLong();
//
//        return new BookingDetailsDTO(
//                bookingId,
//                userId,
//                eventId,
//                status,
//                totalAmount,
//                Collections.emptyMap(),
//                Collections.emptyList(),
//                totalItems,
//                confirmedItems
//        );
//    }
//}

package com.team22.eventticketing.booking.adapter;

import com.team22.eventticketing.booking.dto.BookingDetailsDTO;
import org.neo4j.driver.Record;

import java.util.Collections;

public class Neo4jRecordAdapter {

    public BookingDetailsDTO adaptToBookingDetailsDTO(Record record) {

        Long bookingId = record.get("bookingId").asLong();
        Long userId = record.get("userId").asLong();
        Long eventId = record.get("eventId").asLong();

        String status = record.get("status").asString();
        Double totalAmount = record.get("totalAmount").asDouble();

        int totalItems = record.get("totalItems").asInt();
        long confirmedItems = record.get("confirmedItems").asLong();

        return BookingDetailsDTO.builder()
                .bookingId(bookingId)
                .userId(userId)
                .eventId(eventId)
                .status(status)
                .totalAmount(totalAmount)
                .metadata(Collections.emptyMap())
                .items(Collections.emptyList())
                .totalItems(totalItems)
                .confirmedItems(confirmedItems)
                .build();
    }

    public BookingDetailsDTO adapt(Record record) {
        return adaptToBookingDetailsDTO(record);
    }
}