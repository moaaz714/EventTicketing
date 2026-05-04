package com.team22.eventticketing.ticket.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("ticket_scan_events")
public class TicketScanEvent {

    @PrimaryKey
    private TicketScanEventKey key;

    private String scanType;
    private String attendeeName;
    private String gate;
    private String section;
    private String seatNumber;
    private String notes;

    public TicketScanEvent() {}

    public TicketScanEvent(TicketScanEventKey key,
                           String scanType,
                           String attendeeName,
                           String gate,
                           String section,
                           String seatNumber,
                           String notes) {
        this.key = key;
        this.scanType = scanType;
        this.attendeeName = attendeeName;
        this.gate = gate;
        this.section = section;
        this.seatNumber = seatNumber;
        this.notes = notes;
    }

    public TicketScanEventKey getKey() { return key; }
    public void setKey(TicketScanEventKey key) { this.key = key; }

    public String getScanType() { return scanType; }
    public void setScanType(String scanType) { this.scanType = scanType; }

    public String getAttendeeName() { return attendeeName; }
    public void setAttendeeName(String attendeeName) { this.attendeeName = attendeeName; }

    public String getGate() { return gate; }
    public void setGate(String gate) { this.gate = gate; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}