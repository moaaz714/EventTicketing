package com.team22.eventticketing.ticket.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import com.team22.eventticketing.ticket.adapter.CassandraRowAdapter;
import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketScanEvent;
import com.team22.eventticketing.ticket.model.TicketScanEventKey;
import com.team22.eventticketing.ticket.model.dto.TicketScanDTO;
import com.team22.eventticketing.ticket.model.dto.TicketScanRequestDTO;
import com.team22.eventticketing.ticket.observer.MongoEventLogger;
import com.team22.eventticketing.ticket.repository.TicketRepository;
import com.team22.eventticketing.ticket.repository.TicketScanEventRepository;
import io.jsonwebtoken.Claims;

@Service
public class TicketScanService {
    private final TicketRepository ticketRepository;
    private final TicketScanEventRepository cassandraRepository;
    private final JwtService jwtService;
    private MongoEventLogger mongoEventLogger;

    public TicketScanService(TicketRepository ticketRepository,
            TicketScanEventRepository cassandraRepo,
            JwtService jwtService,
            MongoEventLogger mongoEventLogger) {
        this.ticketRepository = ticketRepository;
        this.cassandraRepository = cassandraRepo;
        this.jwtService = jwtService;
        this.mongoEventLogger = mongoEventLogger;
    }

    // ─── [S4-F11] Record Ticket Scan (write) ─────────────────────────────────
    @CacheEvict(value = "ticket-service::S4-F10", allEntries = true)
    public void recordScan(Long ticketId, String token, TicketScanRequestDTO request) {

        // 1. Validate JWT
        Claims claims;
        try {
            claims = jwtService.extractClaims(token);
        } catch (Exception e) {
            throw new RuntimeException("Unauthorized");
        }

        // (optional role check)
        String role = (String) claims.get("role");
        if (role == null) {
            throw new RuntimeException("Unauthorized");
        }

        // 2. Fetch ticket from PostgreSQL
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // 3. Create Cassandra entity
        TicketScanEventKey key = new TicketScanEventKey(
                ticketId,
                Instant.now());

        TicketScanEvent event = new TicketScanEvent();
        event.setKey(key);
        event.setScanType(request.getScanType());
        event.setAttendeeName(ticket.getAttendeeName());
        event.setGate(request.getGate());
        event.setSection(request.getSection());
        event.setSeatNumber(request.getSeatNumber());
        event.setNotes(request.getNotes());

        // 4. Save to Cassandra
        cassandraRepository.save(event);

        // 5. Log to Mongo (TicketEvent)

        Map<String, Object> payload = new HashMap<>();
        payload.put("ticketId", ticketId);
        payload.put("scanType", request.getScanType());
        payload.put("gate", request.getGate());
        payload.put("section", request.getSection());
        payload.put("seatNumber", request.getSeatNumber());
        payload.put("notes", request.getNotes());

        mongoEventLogger.onEvent("TICKET_SCANNED", payload);
    }

    // ─── [S4-F12] Get Ticket Scan History (read) ───────────────────────────────
    @Cacheable(cacheNames = "ticket-service::S4-F12", key = "#ticketId + ':' + T(String).valueOf(#startTime)+ ':' + T(String).valueOf(#endTime)")
    public List<TicketScanDTO> getScanHistory(Long ticketId, Instant startTime, Instant endTime) {
        // Verify ticket in postgreSQL
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found: " + ticketId));
        // Query Cassandra based on which params are presented
        List<TicketScanEvent> events;
        if (startTime != null && endTime != null) {
            events = cassandraRepository.findByKeyTicketIdAndKeyTimestampBetween(ticketId, startTime, endTime);
        } else if (startTime != null) {
            events = cassandraRepository.findByKeyTicketIdAndKeyTimestampGreaterThanEqual(ticketId, startTime);
        } else if (endTime != null) {
            events = cassandraRepository.findByKeyTicketIdAndKeyTimestampLessThanEqual(ticketId, endTime);
        }

        else {
            events = cassandraRepository.findByKeyTicketId(ticketId);
        }

        // Adapt to DTOs
        CassandraRowAdapter adapter = new CassandraRowAdapter();
        return events.stream().map(adapter::adapt).collect(Collectors.toList());
    }

}
