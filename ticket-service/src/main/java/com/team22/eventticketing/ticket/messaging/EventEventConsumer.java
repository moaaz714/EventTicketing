package com.team22.eventticketing.ticket.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team22.eventticketing.ticket.config.RabbitMQConfig;
import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketStatus;
import com.team22.eventticketing.ticket.repository.TicketRepository;
import events.EventStatusChangedEvent;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventEventConsumer.class);

    private final TicketRepository ticketRepository;
    private final TicketEventPublisher ticketEventPublisher;
    private final ObjectMapper objectMapper;

    public EventEventConsumer(TicketRepository ticketRepository,
                              TicketEventPublisher ticketEventPublisher,
                              ObjectMapper objectMapper) {
        this.ticketRepository = ticketRepository;
        this.ticketEventPublisher = ticketEventPublisher;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.EVENT_LISTENER_QUEUE)
    @Transactional
    public void handleEventEvent(Message message,
                                 @Header("amqp_receivedRoutingKey") String routingKey) {
        try {
            if ("event.status-changed".equals(routingKey)) {
                EventStatusChangedEvent event = objectMapper.readValue(
                        message.getBody(), EventStatusChangedEvent.class);
                handleStatusChanged(event);
            }
            // any other event.* routing key: ignore (still ack'd)
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to process " + routingKey + ": " + e.getMessage(), e);
        }
    }

    private void handleStatusChanged(EventStatusChangedEvent event) {
        if (!"CANCELLED".equalsIgnoreCase(event.newStatus())) {
            return; // only act on cancellations
        }

        // State-guarded cascade: only currently-VALID tickets are cancelled.
        // Idempotent — already-CANCELLED tickets are skipped because they don't match VALID.
        List<Ticket> validTickets = ticketRepository.findByEventIdAndStatus(
                event.eventId(), TicketStatus.VALID);

        if (validTickets.isEmpty()) {
            log.debug("event.status-changed CANCELLED for event {} - no VALID tickets to cancel",
                    event.eventId());
            return;
        }

        for (Ticket t : validTickets) {
            t.setStatus(TicketStatus.CANCELLED);
        }
        ticketRepository.saveAll(validTickets);

        for (Ticket t : validTickets) {
            ticketEventPublisher.publishTicketCancelled(t.getId(), t.getBookingId());
        }

        log.info("Cancelled {} ticket(s) cascading from event.status-changed event {} -> CANCELLED",
                validTickets.size(), event.eventId());
    }
}
