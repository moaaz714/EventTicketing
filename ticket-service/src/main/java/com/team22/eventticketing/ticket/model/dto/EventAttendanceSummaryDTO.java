package com.team22.eventticketing.ticket.model.dto;

import java.time.LocalDateTime;

public class EventAttendanceSummaryDTO {
    private Long eventId;
    private long totalTickets;
    private long usedTickets;
    private long validTickets;
    private double attendanceRate;
    private LocalDateTime lastCheckIn;

    public EventAttendanceSummaryDTO(Long eventId, long total, long used, long valid, LocalDateTime last) {
        this.eventId = eventId;
        this.totalTickets = total;
        this.usedTickets = used;
        this.validTickets = valid;
        this.lastCheckIn = last;
        // Calculate rate: (Used / Total) * 100
        this.attendanceRate = (total > 0) ? ((double) used / total) * 100 : 0.0;
    }

    public Long getEventId() {
        return eventId;
    }

    public long getTotalTickets() {
        return totalTickets;
    }

    public long getUsedTickets() {
        return usedTickets;
    }

    public long getValidTickets() {
        return validTickets;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public LocalDateTime getLastCheckIn() {
        return lastCheckIn;
    }

}