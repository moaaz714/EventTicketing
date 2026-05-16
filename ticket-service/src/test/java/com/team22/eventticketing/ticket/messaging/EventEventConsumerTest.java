package com.team22.eventticketing.ticket.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketStatus;
import com.team22.eventticketing.ticket.repository.TicketRepository;
import events.EventStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.List;

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
class EventEventConsumerTest {

    @Mock TicketRepository ticketRepository;
    @Mock TicketEventPublisher publisher;

    private EventEventConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new EventEventConsumer(ticketRepository, publisher, objectMapper);
    }

    @Test
    void eventStatusChangedToCancelled_cancelsValidTicketsForThatEvent() throws Exception {
        Ticket t1 = ticket(101L, 7L, 5L, TicketStatus.VALID);
        Ticket t2 = ticket(102L, 8L, 5L, TicketStatus.VALID);
        when(ticketRepository.findByEventIdAndStatus(5L, TicketStatus.VALID))
                .thenReturn(List.of(t1, t2));

        EventStatusChangedEvent event = new EventStatusChangedEvent(5L, "UPCOMING", "CANCELLED");
        consumer.handleEventEvent(message(objectMapper.writeValueAsBytes(event)), "event.status-changed");

        assertThat(t1.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(t2.getStatus()).isEqualTo(TicketStatus.CANCELLED);

        verify(ticketRepository).saveAll(anyIterable());

        verify(publisher).publishTicketCancelled(101L, 7L);
        verify(publisher).publishTicketCancelled(102L, 8L);
        verify(publisher, times(2)).publishTicketCancelled(anyLong(), anyLong());
    }

    @Test
    void eventStatusChangedToCancelled_lowerCase_alsoTriggersCascade() throws Exception {
        Ticket t1 = ticket(101L, 7L, 5L, TicketStatus.VALID);
        when(ticketRepository.findByEventIdAndStatus(5L, TicketStatus.VALID))
                .thenReturn(List.of(t1));

        EventStatusChangedEvent event = new EventStatusChangedEvent(5L, "UPCOMING", "cancelled");
        consumer.handleEventEvent(message(objectMapper.writeValueAsBytes(event)), "event.status-changed");

        assertThat(t1.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(publisher).publishTicketCancelled(101L, 7L);
    }

    @Test
    void eventStatusChangedToNonCancelled_doesNothing() throws Exception {
        EventStatusChangedEvent event = new EventStatusChangedEvent(5L, "UPCOMING", "COMPLETED");
        consumer.handleEventEvent(message(objectMapper.writeValueAsBytes(event)), "event.status-changed");

        verify(ticketRepository, never()).findByEventIdAndStatus(anyLong(), any());
        verify(ticketRepository, never()).saveAll(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void eventStatusChangedToCancelled_noValidTickets_doesNothing_idempotent() throws Exception {
        when(ticketRepository.findByEventIdAndStatus(5L, TicketStatus.VALID))
                .thenReturn(List.of());

        EventStatusChangedEvent event = new EventStatusChangedEvent(5L, "UPCOMING", "CANCELLED");
        consumer.handleEventEvent(message(objectMapper.writeValueAsBytes(event)), "event.status-changed");

        verify(ticketRepository, never()).saveAll(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void unknownEventRoutingKey_isIgnored_noError() throws Exception {
        consumer.handleEventEvent(message("{}".getBytes()), "event.rated");

        verifyNoInteractions(publisher);
        verify(ticketRepository, never()).saveAll(any());
        verify(ticketRepository, never()).findByEventIdAndStatus(anyLong(), any());
    }

    private static Ticket ticket(long id, long bookingId, long eventId, TicketStatus status) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setBookingId(bookingId);
        t.setEventId(eventId);
        t.setStatus(status);
        t.setTicketCode("TIX-" + id);
        t.setAttendeeName("Attendee " + id);
        return t;
    }

    private static Message message(byte[] body) {
        return new Message(body, new MessageProperties());
    }
}
