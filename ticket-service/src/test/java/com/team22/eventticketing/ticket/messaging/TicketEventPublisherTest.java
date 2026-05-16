package com.team22.eventticketing.ticket.messaging;

import com.team22.eventticketing.ticket.config.RabbitMQConfig;
import events.TicketCancelledEvent;
import events.TicketIssuedEvent;
import events.TicketStatusChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class TicketEventPublisherTest {

    @Mock
    RabbitTemplate rabbitTemplate;

    @InjectMocks
    TicketEventPublisher publisher;

    @Test
    void publishTicketIssued_sendsToTicketEventsExchangeWithCorrectKeyAndPayload() {
        publisher.publishTicketIssued(42L, 7L, "Ahmed Hassan", "TIX-2026-001");

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.TICKET_EVENTS_EXCHANGE),
                eq("ticket.issued"),
                payloadCaptor.capture());

        Object payload = payloadCaptor.getValue();
        assertThat(payload).isInstanceOf(TicketIssuedEvent.class);
        TicketIssuedEvent event = (TicketIssuedEvent) payload;
        assertThat(event.ticketId()).isEqualTo(42L);
        assertThat(event.bookingId()).isEqualTo(7L);
        assertThat(event.attendeeName()).isEqualTo("Ahmed Hassan");
        assertThat(event.ticketCode()).isEqualTo("TIX-2026-001");

        verifyNoMoreInteractions(rabbitTemplate);
    }

    @Test
    void publishTicketStatusChanged_sendsToTicketEventsExchangeWithCorrectKeyAndPayload() {
        publisher.publishTicketStatusChanged(42L, 7L, "USED");

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.TICKET_EVENTS_EXCHANGE),
                eq("ticket.status-changed"),
                payloadCaptor.capture());

        Object payload = payloadCaptor.getValue();
        assertThat(payload).isInstanceOf(TicketStatusChangedEvent.class);
        TicketStatusChangedEvent event = (TicketStatusChangedEvent) payload;
        assertThat(event.ticketId()).isEqualTo(42L);
        assertThat(event.bookingId()).isEqualTo(7L);
        assertThat(event.newStatus()).isEqualTo("USED");

        verifyNoMoreInteractions(rabbitTemplate);
    }

    @Test
    void publishTicketCancelled_sendsToTicketEventsExchangeWithCorrectKeyAndPayload() {
        publisher.publishTicketCancelled(42L, 7L);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.TICKET_EVENTS_EXCHANGE),
                eq("ticket.cancelled"),
                payloadCaptor.capture());

        Object payload = payloadCaptor.getValue();
        assertThat(payload).isInstanceOf(TicketCancelledEvent.class);
        TicketCancelledEvent event = (TicketCancelledEvent) payload;
        assertThat(event.ticketId()).isEqualTo(42L);
        assertThat(event.bookingId()).isEqualTo(7L);

        verifyNoMoreInteractions(rabbitTemplate);
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
