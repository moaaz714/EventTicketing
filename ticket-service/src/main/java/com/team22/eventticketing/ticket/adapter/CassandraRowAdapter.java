package com.team22.eventticketing.ticket.adapter;

import com.datastax.oss.driver.api.core.cql.Row;
import com.team22.eventticketing.ticket.model.dto.EventAttendanceSummaryDTO;
import java.util.Map;
import com.team22.eventticketing.ticket.model.dto.TicketScanDTO;
import com.team22.eventticketing.ticket.model.TicketScanEvent;

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

    public TicketScanDTO adapt(TicketScanEvent event) {
        return TicketScanDTO.builder().timestamp(event.getKey().getTimestamp())
                .scanType(event.getScanType())
                .attendeeName(event.getAttendeeName())
                .gate(event.getGate())
                .section(event.getSection())
                .seatNumber(event.getSeatNumber())
                .notes(event.getNotes())
                .build();
    }
}