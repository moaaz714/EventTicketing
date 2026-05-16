package com.team22.eventticketing.ticket.messaging;

import com.team22.eventticketing.ticket.config.RabbitMQConfig;
import events.TicketCancelledEvent;
import events.TicketIssuedEvent;
import events.TicketStatusChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class TicketEventPublisher {

    public static final String ROUTING_TICKET_ISSUED          = "ticket.issued";
    public static final String ROUTING_TICKET_STATUS_CHANGED  = "ticket.status-changed";
    public static final String ROUTING_TICKET_CANCELLED       = "ticket.cancelled";

    private final RabbitTemplate rabbitTemplate;

    public TicketEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTicketIssued(Long ticketId, Long bookingId,
                                    String attendeeName, String ticketCode) {
        TicketIssuedEvent event = new TicketIssuedEvent(ticketId, bookingId, attendeeName, ticketCode);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TICKET_EVENTS_EXCHANGE,
                ROUTING_TICKET_ISSUED,
                event);
    }

    public void publishTicketStatusChanged(Long ticketId, Long bookingId, String newStatus) {
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(ticketId, bookingId, newStatus);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TICKET_EVENTS_EXCHANGE,
                ROUTING_TICKET_STATUS_CHANGED,
                event);
    }

    public void publishTicketCancelled(Long ticketId, Long bookingId) {
        TicketCancelledEvent event = new TicketCancelledEvent(ticketId, bookingId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TICKET_EVENTS_EXCHANGE,
                ROUTING_TICKET_CANCELLED,
                event);
    }
}
