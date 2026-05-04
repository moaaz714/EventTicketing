package com.team22.eventticketing.ticket.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors; 

import com.team22.eventticketing.ticket.adapter.CassandraRowAdapter;
import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketScanEvent;
import com.team22.eventticketing.ticket.model.TicketScanEventKey;
import com.team22.eventticketing.ticket.model.dto.TicketScanDTO;
import com.team22.eventticketing.ticket.model.dto.TicketScanRequestDTO;
import com.team22.eventticketing.ticket.repository.TicketRepository;
import com.team22.eventticketing.ticket.repository.TicketScanEventRepository;
import io.jsonwebtoken.Claims;



@Service
public class TicketScanService {
    private final TicketRepository ticketRepository;
    private final TicketScanEventRepository cassandraRepository;
    private final JwtService jwtService;

    public TicketScanService(TicketRepository ticketRepository, TicketScanEventRepository cassandraRepository, JwtService jwtService) {
        this.ticketRepository = ticketRepository;
        this.cassandraRepository = cassandraRepository;
        this.jwtService = jwtService;
    }


    // ─── [S4-F11] Record Ticket Scan (write) ─────────────────────────────────
     public void recordScan(Long ticketId, String token, TicketScanRequestDTO request) {
         Claims claims;
         try { claims = jwtService.extractClaims(token); }
         catch (Exception e) { throw new RuntimeException("Unauthorized"); }

         if (claims.get("role") == null) throw new RuntimeException("Unauthorized");

         Ticket ticket = ticketRepository.findById(ticketId)
                 .orElseThrow(() -> new RuntimeException("Ticket not found"));

         TicketScanEventKey key = new TicketScanEventKey(ticketId, Instant.now());
         TicketScanEvent event  = new TicketScanEvent(
                 key, request.getScanType(), ticket.getAttendeeName(),
                 request.getGate(), request.getSection(),
                 request.getSeatNumber(), request.getNotes());

         cassandraRepository.save(event);
     }

     // ─── [S4-F12] Get Ticket Scan History (read) ───────────────────────────────
     @Cacheable(cacheNames = "ticket-service::S4-F12", key = "#ticketId + ':' + T(String).valueOf(#startTime)+ ':' + T(String).valueOf(#endTime)")
     public List<TicketScanDTO> getScanHistory(Long ticketId, Instant startTime, Instant endTime) {
        // Verify ticket in postgreSQL
        ticketRepository.findById(ticketId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found: " + ticketId));
        // Query Cassandra based on which params are presented
        List<TicketScanEvent> events;
        if(startTime != null && endTime != null) { events = cassandraRepository.findByKeyTicketIdAndKeyTimestampBetween(ticketId, startTime, endTime);}
        else if (startTime != null) { events = cassandraRepository.findByKeyTicketIdAndKeyTimestampGreaterThanEqual(ticketId, startTime);}
        else if (endTime != null) { events = cassandraRepository.findByKeyTicketIdAndKeyTimestampLessThanEqual(ticketId, endTime); }
        else { events = cassandraRepository.findByKeyTicketId(ticketId); }

        // Adapt to DTOs
        CassandraRowAdapter adapter = new CassandraRowAdapter();
        return events.stream().map(adapter::adapt).collect(Collectors.toList());
     }


}
