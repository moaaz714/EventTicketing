package com.team22.eventticketing.ticket.adapter;

import com.datastax.oss.driver.api.core.cql.Row;
import com.team22.eventticketing.ticket.model.dto.NearbyTicketDTO;

public class CassandraRowAdapter {

    public NearbyTicketDTO adaptToNearbyTicketDTO(Row row) {

        return NearbyTicketDTO.builder()
                .ticketId(row.getLong("ticket_id"))
                .attendeeName(row.getString("attendee_name"))
                .bookingId(row.getLong("booking_id"))
                .eventName(row.getString("event_name"))
                .eventLat(row.getDouble("event_lat"))
                .eventLon(row.getDouble("event_lon"))
                .distanceKm(row.getDouble("distance_km"))
                .build();
    }
    public NearbyTicketDTO adapt(Row row) {
        return adaptToNearbyTicketDTO(row);
    }
}