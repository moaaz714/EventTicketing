package com.team22.eventticketing.ticket.service;

import com.team22.eventticketing.contracts.dto.BookingDTO;
import com.team22.eventticketing.contracts.events.TicketIssuedEvent;
import com.team22.eventticketing.contracts.feign.BookingServiceClient;
import com.team22.eventticketing.contracts.feign.EventServiceClient;
import com.team22.eventticketing.ticket.messaging.TicketEventPublisher;
import com.team22.eventticketing.ticket.model.BookingEventMapping;
import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketStatus;
import com.team22.eventticketing.contracts.dto.EventTicketSummaryDTO;
import com.team22.eventticketing.ticket.repository.BookingEventMappingRepository;
import com.team22.eventticketing.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceUnitTest {

    @Mock TicketRepository ticketRepository;
    @Mock CacheEvictionService cacheEviction;
    @Mock BookingServiceClient bookingServiceClient;
    @Mock EventServiceClient eventServiceClient;
    @Mock TicketEventPublisher ticketEventPublisher;
    @Mock BookingEventMappingRepository bookingEventMappingRepository;

    @InjectMocks TicketService ticketService;

    private static BookingDTO bookingDto(Long bookingId, Long eventId) {
        return new BookingDTO(bookingId, 10L, eventId, "CHECKED_IN", BigDecimal.valueOf(500.00));
    }

    @Test
    void f2_issueTicket_callsFeignThenSavesAndPublishesTicketIssued() {
        Long bookingId = 1L;
        Long eventId = 99L;

        when(bookingServiceClient.getBooking(bookingId)).thenReturn(bookingDto(bookingId, eventId));
        when(bookingEventMappingRepository.findById(bookingId))
                .thenReturn(Optional.of(new BookingEventMapping(bookingId, eventId, java.time.LocalDateTime.now())));
        when(ticketRepository.existsByTicketCode("TKT-001")).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(42L);
            return t;
        });

        Ticket saved = ticketService.issueTicket(bookingId, "Alice", "TKT-001", Map.of());

        verify(bookingServiceClient, times(1)).getBooking(bookingId);
        verify(ticketRepository).save(any(Ticket.class));
        verify(ticketEventPublisher).publishTicketIssued(any(TicketIssuedEvent.class));
        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(saved.getEventId()).isEqualTo(eventId);
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.VALID);
    }

    @Test
    void f2_issueTicket_feignThrows_mapsTo404AndDoesNotPublish() {
        Long bookingId = 999L;
        when(bookingServiceClient.getBooking(bookingId)).thenThrow(new RuntimeException("404 Not Found"));

        assertThatThrownBy(() -> ticketService.issueTicket(bookingId, "Alice", "TKT-X", Map.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Booking not found");

        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(ticketEventPublisher, never()).publishTicketIssued(any());
    }

    @Test
    void f8_getEventTicketSummary_readsLocallyAndReturnsCounts() {
        Long eventId = 5L;
        Ticket t1 = new Ticket();
        t1.setStatus(TicketStatus.USED);
        Ticket t2 = new Ticket();
        t2.setStatus(TicketStatus.USED);
        Ticket t3 = new Ticket();
        t3.setStatus(TicketStatus.VALID);

        when(ticketRepository.findByEventId(eventId)).thenReturn(List.of(t1, t2, t3));

        EventTicketSummaryDTO summary = ticketService.getEventTicketSummary(eventId);

        verify(ticketRepository, times(1)).findByEventId(eventId);
        verify(bookingServiceClient, never()).getBooking(anyLong());
        verify(eventServiceClient, never()).getEvent(anyLong());
        assertThat(summary.totalTicketsSold()).isEqualTo(3L);
        assertThat(summary.usedCount()).isEqualTo(2L);
    }

    @Test
    void f8_getEventTicketSummary_noTickets_throws404() {
        Long eventId = 404L;
        when(ticketRepository.findByEventId(eventId)).thenReturn(List.of());

        assertThatThrownBy(() -> ticketService.getEventTicketSummary(eventId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No tickets found");
    }
}
