package com.team22.eventticketing.ticket.service;

import com.team22.eventticketing.ticket.model.dto.TicketScanRequestDTO;
import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketScanEvent;
import com.team22.eventticketing.ticket.model.TicketScanEventKey;
import com.team22.eventticketing.ticket.observer.MongoEventLogger;
import com.team22.eventticketing.ticket.repository.TicketRepository;
import com.team22.eventticketing.ticket.repository.TicketScanEventRepository;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class TicketScanService {

    private final TicketRepository ticketRepository;
    private final TicketScanEventRepository cassandraRepo;
    private final JwtService jwtService;
    private final MongoEventLogger mongoEventLogger;

    public TicketScanService(TicketRepository ticketRepository,
                             TicketScanEventRepository cassandraRepo,
                             JwtService jwtService,
                             MongoEventLogger mongoEventLogger) {
        this.ticketRepository = ticketRepository;
        this.cassandraRepo = cassandraRepo;
        this.jwtService = jwtService;
        this.mongoEventLogger = mongoEventLogger;
    }

    public void recordScan(Long ticketId, String token, TicketScanRequestDTO request) {

        //  1. Validate JWT
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

        //  3. Create Cassandra entity
        TicketScanEventKey key = new TicketScanEventKey(
                ticketId,
                Instant.now()
        );

        TicketScanEvent event = new TicketScanEvent();
        event.setKey(key);
        event.setScanType(request.getScanType());
        event.setAttendeeName(ticket.getAttendeeName());
        event.setGate(request.getGate());
        event.setSection(request.getSection());
        event.setSeatNumber(request.getSeatNumber());
        event.setNotes(request.getNotes());

        //  4. Save to Cassandra
        cassandraRepo.save(event);

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
}