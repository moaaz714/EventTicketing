//package com.team22.eventticketing.ticket.adapter;
//
//import com.datastax.oss.driver.api.core.cql.Row;
//import com.team22.eventticketing.ticket.model.dto.NearbyTicketDTO;
//
//public class CassandraRowAdapter {
//
//    public NearbyTicketDTO adaptToNearbyTicketDTO(Row row) {
//
//        return NearbyTicketDTO.builder()
//                .ticketId(row.getLong("ticket_id"))
//                .attendeeName(row.getString("attendee_name"))
//                .bookingId(row.getLong("booking_id"))
//                .eventName(row.getString("event_name"))
//                .eventLat(row.getDouble("event_lat"))
//                .eventLon(row.getDouble("event_lon"))
//                .distanceKm(row.getDouble("distance_km"))
//                .build();
//    }
//    public NearbyTicketDTO adapt(Row row) {
//        return adaptToNearbyTicketDTO(row);
//    }
//}
package com.team22.eventticketing.ticket.adapter;

import com.team22.eventticketing.ticket.model.dto.EventAttendanceSummaryDTO;
import java.util.Map;

public class CassandraRowAdapter {

    public EventAttendanceSummaryDTO adapt(Map<String, Object> row) {

        return EventAttendanceSummaryDTO.builder()
                .eventId(((Number) row.get("eventId")).longValue())
                .totalTickets(((Number) row.get("total")).longValue())
                .usedTickets(((Number) row.get("used")).longValue())
                .validTickets(((Number) row.get("valid")).longValue())
                .lastCheckIn((java.time.LocalDateTime) row.get("last_checkin"))
                .build();
    }
}