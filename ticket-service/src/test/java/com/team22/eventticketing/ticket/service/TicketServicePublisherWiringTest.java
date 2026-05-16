package com.team22.eventticketing.ticket.service;

import com.team22.eventticketing.ticket.messaging.TicketEventPublisher;
import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketStatus;
import com.team22.eventticketing.ticket.model.dto.BatchTicketRequestDTO;
import com.team22.eventticketing.ticket.model.dto.TicketItemDTO;
import com.team22.eventticketing.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServicePublisherWiringTest {

    @Mock TicketRepository ticketRepository;
    @Mock CacheEvictionService cacheEviction;
    @Mock TicketEventPublisher publisher;

    private TicketService service;

    @BeforeEach
    void setUp() {
        service = new TicketService(ticketRepository, cacheEviction, publisher);
    }

    @Test
    void issueTicket_publishesTicketIssued() {
        when(ticketRepository.existsBookingById(7L)).thenReturn(true);
        when(ticketRepository.existsByTicketCode("TIX-001")).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(101L);
            return t;
        });

        Ticket saved = service.issueTicket(7L, "Ahmed", "TIX-001", Map.of("section", "VIP"));

        assertThat(saved.getId()).isEqualTo(101L);
        verify(publisher).publishTicketIssued(101L, 7L, "Ahmed", "TIX-001");
        verify(publisher, times(1)).publishTicketIssued(anyLong(), anyLong(), any(), any());
    }

    @Test
    void deleteTicket_publishesTicketCancelled() {
        Ticket existing = ticket(101L, 7L, TicketStatus.VALID);
        when(ticketRepository.findById(101L)).thenReturn(Optional.of(existing));

        service.deleteTicket(101L);

        verify(ticketRepository).delete(existing);
        verify(publisher).publishTicketCancelled(101L, 7L);
    }

    @Test
    void updateTicket_statusChangedToUsed_publishesStatusChanged_notCancelled() {
        Ticket existing = ticket(101L, 7L, TicketStatus.VALID);
        when(ticketRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket update = new Ticket();
        update.setStatus(TicketStatus.USED);
        service.updateTicket(101L, update);

        verify(publisher).publishTicketStatusChanged(101L, 7L, "USED");
        verify(publisher, never()).publishTicketCancelled(anyLong(), anyLong());
    }

    @Test
    void updateTicket_statusChangedToCancelled_publishesTicketCancelled_notStatusChanged() {
        Ticket existing = ticket(101L, 7L, TicketStatus.VALID);
        when(ticketRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket update = new Ticket();
        update.setStatus(TicketStatus.CANCELLED);
        service.updateTicket(101L, update);

        verify(publisher).publishTicketCancelled(101L, 7L);
        verify(publisher, never()).publishTicketStatusChanged(anyLong(), anyLong(), any());
    }

    @Test
    void updateTicket_noStatusChange_doesNotPublish() {
        Ticket existing = ticket(101L, 7L, TicketStatus.VALID);
        when(ticketRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket update = new Ticket();
        update.setAttendeeName("Renamed");
        // no status set
        service.updateTicket(101L, update);

        verifyNoInteractions(publisher);
    }

    @Test
    void batchIssueTickets_publishesTicketIssuedPerTicket() {
        when(ticketRepository.bookingExistsById(7L)).thenReturn(true);
        when(ticketRepository.existsByTicketCode("T1")).thenReturn(false);
        when(ticketRepository.existsByTicketCode("T2")).thenReturn(false);
        when(ticketRepository.saveAll(anyIterable())).thenAnswer(inv -> {
            Iterable<Ticket> tickets = inv.getArgument(0);
            long nextId = 1000L;
            java.util.List<Ticket> result = new java.util.ArrayList<>();
            for (Ticket t : tickets) {
                t.setId(nextId++);
                result.add(t);
            }
            return result;
        });

        BatchTicketRequestDTO request = new BatchTicketRequestDTO();
        request.setBookingId(7L);
        request.setTickets(List.of(
                item("A", "T1"),
                item("B", "T2")));

        int count = service.batchIssueTickets(request);

        assertThat(count).isEqualTo(2);
        verify(publisher).publishTicketIssued(1000L, 7L, "A", "T1");
        verify(publisher).publishTicketIssued(1001L, 7L, "B", "T2");
        verify(publisher, times(2)).publishTicketIssued(anyLong(), anyLong(), any(), any());
    }

    private static Ticket ticket(long id, long bookingId, TicketStatus status) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setBookingId(bookingId);
        t.setStatus(status);
        t.setTicketCode("TIX-" + id);
        t.setAttendeeName("Attendee " + id);
        t.setIssuedAt(LocalDateTime.now());
        return t;
    }

    private static TicketItemDTO item(String attendee, String code) {
        TicketItemDTO i = new TicketItemDTO();
        i.setAttendeeName(attendee);
        i.setTicketCode(code);
        i.setMetadata(Map.of());
        return i;
    }
}
