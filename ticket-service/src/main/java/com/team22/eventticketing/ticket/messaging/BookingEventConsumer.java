package com.team22.eventticketing.ticket.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team22.eventticketing.ticket.config.RabbitMQConfig;
import com.team22.eventticketing.ticket.model.Ticket;
import com.team22.eventticketing.ticket.model.TicketStatus;
import com.team22.eventticketing.ticket.repository.TicketRepository;
import events.BookingCancelledEvent;
import events.BookingCompletedEvent;
import events.BookingPlacedEvent;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingEventConsumer.class);

    private final TicketRepository ticketRepository;
    private final TicketEventPublisher ticketEventPublisher;
    private final ObjectMapper objectMapper;

    public BookingEventConsumer(TicketRepository ticketRepository,
                                TicketEventPublisher ticketEventPublisher,
                                ObjectMapper objectMapper) {
        this.ticketRepository = ticketRepository;
        this.ticketEventPublisher = ticketEventPublisher;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.BOOKING_LISTENER_QUEUE)
    @Transactional
    public void handleBookingEvent(Message message,
                                   @Header("amqp_receivedRoutingKey") String routingKey) {
        try {
            switch (routingKey) {
                case "booking.placed" -> handlePlaced(objectMapper.readValue(
                        message.getBody(), BookingPlacedEvent.class));
                case "booking.completed" -> handleCompleted(objectMapper.readValue(
                        message.getBody(), BookingCompletedEvent.class));
                case "booking.cancelled" -> handleCancelled(objectMapper.readValue(
                        message.getBody(), BookingCancelledEvent.class));
                default -> {
                    // any other booking.* routing key: ignore (still ack'd)
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to process " + routingKey + ": " + e.getMessage(), e);
        }
    }

    private void handlePlaced(BookingPlacedEvent event) {
        // Audit-only — no ticket mutation expected at booking placement.
        log.debug("booking.placed received: bookingId={} userId={} eventId={}",
                event.bookingId(), event.userId(), event.eventId());
    }

    private void handleCompleted(BookingCompletedEvent event) {
        // Audit-only — actual ticket issuance happens via the synchronous S4-F2 flow,
        // not via this consumer. Tracked here for observability.
        log.debug("booking.completed received: bookingId={} userId={} amount={}",
                event.bookingId(), event.userId(), event.totalAmount());
    }

    private void handleCancelled(BookingCancelledEvent event) {
        // State-guarded cascade: only currently-VALID tickets are cancelled.
        // Idempotent — already-CANCELLED tickets are skipped because they don't match VALID.
        List<Ticket> validTickets = ticketRepository.findByBookingIdAndStatus(
                event.bookingId(), TicketStatus.VALID);

        if (validTickets.isEmpty()) {
            log.debug("booking.cancelled for bookingId {} - no VALID tickets to cancel",
                    event.bookingId());
            return;
        }

        for (Ticket t : validTickets) {
            t.setStatus(TicketStatus.CANCELLED);
        }
        ticketRepository.saveAll(validTickets);

        for (Ticket t : validTickets) {
            ticketEventPublisher.publishTicketCancelled(t.getId(), t.getBookingId());
        }

        log.info("Cancelled {} ticket(s) cascading from booking.cancelled for bookingId {}",
                validTickets.size(), event.bookingId());
    }
}
