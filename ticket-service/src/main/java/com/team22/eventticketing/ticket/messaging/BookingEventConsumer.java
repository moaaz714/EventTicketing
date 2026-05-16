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

/**
 * Saga listener for booking.* signals, bound to {@code ticket.booking.saga-listener}.
 *
 * Routing semantics:
 *   booking.placed     -> capture eventId for the booking (no ticket mutation; tickets don't exist yet).
 *   booking.completed  -> log + emit a ticket.status-changed audit signal per existing ticket. The
 *                         ticket status is NOT mutated; the audit signal advertises the current status
 *                         downstream so analytics / reporting can correlate the booking lifecycle.
 *   booking.cancelled  -> state-guarded cascade: VALID tickets transition to CANCELLED.
 */
@Component
public class BookingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingEventConsumer.class);

    private final TicketRepository ticketRepository;
    private final TicketEventPublisher ticketEventPublisher;
    private final BookingEventContext bookingEventContext;
    private final ObjectMapper objectMapper;

    public BookingEventConsumer(TicketRepository ticketRepository,
                                TicketEventPublisher ticketEventPublisher,
                                BookingEventContext bookingEventContext,
                                ObjectMapper objectMapper) {
        this.ticketRepository = ticketRepository;
        this.ticketEventPublisher = ticketEventPublisher;
        this.bookingEventContext = bookingEventContext;
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
        // Stash bookingId -> eventId so subsequent S4-F2 / S4-F4 issuance can backfill the
        // Ticket.eventId column without re-querying the booking service.
        bookingEventContext.rememberEventId(event.bookingId(), event.eventId());
        log.debug("booking.placed captured: bookingId={} userId={} eventId={}",
                event.bookingId(), event.userId(), event.eventId());
    }

    private void handleCompleted(BookingCompletedEvent event) {
        log.debug("booking.completed received: bookingId={} userId={} amount={}",
                event.bookingId(), event.userId(), event.totalAmount());

        // Audit-only fan-out: emit ticket.status-changed for each existing ticket with its CURRENT
        // status. We do NOT mutate ticket status — ticket lifecycle is owned by S4-F2 issuance and
        // the booking.cancelled / event.status-changed cascades.
        List<Ticket> tickets = ticketRepository.findByBookingId(event.bookingId());
        if (tickets.isEmpty()) {
            log.debug("booking.completed for bookingId {} - no tickets to audit", event.bookingId());
            return;
        }
        for (Ticket t : tickets) {
            TicketStatus status = t.getStatus();
            if (status == null) continue;
            ticketEventPublisher.publishTicketStatusChanged(t.getId(), t.getBookingId(), status.name());
        }
        log.info("Emitted {} ticket.status-changed audit signal(s) for booking.completed bookingId {}",
                tickets.size(), event.bookingId());
    }

    private void handleCancelled(BookingCancelledEvent event) {
        // State-guarded cascade: only currently-VALID tickets are cancelled. Idempotent — already-
        // CANCELLED tickets are skipped because they don't match VALID.
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
