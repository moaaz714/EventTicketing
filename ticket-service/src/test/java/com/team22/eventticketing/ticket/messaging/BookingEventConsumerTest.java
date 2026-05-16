package com.team22.eventticketing.ticket.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketStatus;
import com.team22.eventticketing.ticket.repository.TicketRepository;
import events.BookingCancelledEvent;
import events.BookingCompletedEvent;
import events.BookingPlacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
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
class BookingEventConsumerTest {

    @Mock TicketRepository ticketRepository;
    @Mock TicketEventPublisher publisher;

    private BookingEventConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new BookingEventConsumer(ticketRepository, publisher, objectMapper);
    }

    @Test
    void bookingCancelled_cancelsValidTicketsAndPublishesTicketCancelledForEach() throws Exception {
        Ticket t1 = ticket(101L, 7L, TicketStatus.VALID);
        Ticket t2 = ticket(102L, 7L, TicketStatus.VALID);
        when(ticketRepository.findByBookingIdAndStatus(7L, TicketStatus.VALID))
                .thenReturn(List.of(t1, t2));

        BookingCancelledEvent event = new BookingCancelledEvent(7L, 1L, 5L, "user requested");
        consumer.handleBookingEvent(message(objectMapper.writeValueAsBytes(event)), "booking.cancelled");

        assertThat(t1.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(t2.getStatus()).isEqualTo(TicketStatus.CANCELLED);

        verify(ticketRepository).saveAll(anyIterable());

        verify(publisher).publishTicketCancelled(101L, 7L);
        verify(publisher).publishTicketCancelled(102L, 7L);
        verify(publisher, times(2)).publishTicketCancelled(anyLong(), anyLong());
    }

    @Test
    void bookingCancelled_noValidTickets_doesNothing() throws Exception {
        when(ticketRepository.findByBookingIdAndStatus(7L, TicketStatus.VALID))
                .thenReturn(List.of());

        BookingCancelledEvent event = new BookingCancelledEvent(7L, 1L, 5L, "user requested");
        consumer.handleBookingEvent(message(objectMapper.writeValueAsBytes(event)), "booking.cancelled");

        verify(ticketRepository, never()).saveAll(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void bookingCancelled_alreadyCancelledTickets_areNotInResultSet_idempotent() throws Exception {
        // Repository only returns currently-VALID tickets, so already-CANCELLED ones are skipped.
        // Simulating a redelivery where the first delivery already cancelled everything.
        when(ticketRepository.findByBookingIdAndStatus(7L, TicketStatus.VALID))
                .thenReturn(List.of());

        BookingCancelledEvent event = new BookingCancelledEvent(7L, 1L, 5L, "duplicate delivery");
        consumer.handleBookingEvent(message(objectMapper.writeValueAsBytes(event)), "booking.cancelled");

        verify(ticketRepository, never()).saveAll(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void bookingPlaced_doesNotMutateOrPublish() throws Exception {
        BookingPlacedEvent event = new BookingPlacedEvent(7L, 1L, 5L);
        consumer.handleBookingEvent(message(objectMapper.writeValueAsBytes(event)), "booking.placed");

        verifyNoInteractions(publisher);
        verify(ticketRepository, never()).saveAll(any());
        verify(ticketRepository, never()).findByBookingIdAndStatus(anyLong(), any());
    }

    @Test
    void bookingCompleted_doesNotMutateOrPublish() throws Exception {
        BookingCompletedEvent event = new BookingCompletedEvent(7L, 1L, 5L, new BigDecimal("100.00"));
        consumer.handleBookingEvent(message(objectMapper.writeValueAsBytes(event)), "booking.completed");

        verifyNoInteractions(publisher);
        verify(ticketRepository, never()).saveAll(any());
        verify(ticketRepository, never()).findByBookingIdAndStatus(anyLong(), any());
    }

    @Test
    void unknownRoutingKey_isIgnored_noError() throws Exception {
        consumer.handleBookingEvent(message("{}".getBytes()), "booking.something-else");

        verifyNoInteractions(publisher);
        verify(ticketRepository, never()).saveAll(any());
        verify(ticketRepository, never()).findByBookingIdAndStatus(anyLong(), any());
    }

    private static Ticket ticket(long id, long bookingId, TicketStatus status) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setBookingId(bookingId);
        t.setStatus(status);
        t.setTicketCode("TIX-" + id);
        t.setAttendeeName("Attendee " + id);
        return t;
    }

    private static Message message(byte[] body) {
        return new Message(body, new MessageProperties());
    }
}
