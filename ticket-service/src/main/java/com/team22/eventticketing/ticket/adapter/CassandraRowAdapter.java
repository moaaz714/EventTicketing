package com.team22.eventticketing.ticket.adapter;

import com.datastax.oss.driver.api.core.cql.Row;
import com.team22.eventticketing.ticket.model.dto.EventAttendanceSummaryDTO;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class CassandraRowAdapter {

    public EventAttendanceSummaryDTO adapt(Row row) {

        return EventAttendanceSummaryDTO.builder()
                .eventId(row.getLong("event_id"))
                .totalTickets(row.getLong("total_tickets"))
                .usedTickets(row.getLong("used_tickets"))
                .validTickets(row.getLong("valid_tickets"))
                .lastCheckIn(
                        row.getInstant("last_check_in") != null
                                ? LocalDateTime.ofInstant(
                                row.getInstant("last_check_in"),
                                ZoneId.systemDefault()
                        )
                                : null
                )
                .build();
    }
}