package com.team22.eventticketing.ticket.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Saga-state cache for the ticket.booking.saga-listener.
 *
 * Why: booking.placed carries the eventId, but tickets are issued later via the
 * synchronous S4-F2 / S4-F4 endpoints. The Ticket.eventId column needs that
 * value at issuance time, so we stash bookingId -> eventId here when the
 * booking.placed signal arrives and read it back during issuance.
 *
 * TTL bounds memory pressure for bookings that never issue tickets.
 */
@Component
public class BookingEventContext {

    private static final Logger log = LoggerFactory.getLogger(BookingEventContext.class);
    private static final String KEY_PREFIX = "ticket-service::saga::booking-event::";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    public BookingEventContext(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void rememberEventId(Long bookingId, Long eventId) {
        if (bookingId == null || eventId == null) return;
        try {
            redisTemplate.opsForValue().set(key(bookingId), eventId, TTL);
        } catch (Exception e) {
            log.warn("Saga eventId stash failed for bookingId {}: {}", bookingId, e.getMessage());
        }
    }

    public Optional<Long> lookupEventId(Long bookingId) {
        if (bookingId == null) return Optional.empty();
        try {
            Object value = redisTemplate.opsForValue().get(key(bookingId));
            if (value == null) return Optional.empty();
            if (value instanceof Number n) return Optional.of(n.longValue());
            return Optional.of(Long.parseLong(value.toString()));
        } catch (Exception e) {
            log.warn("Saga eventId lookup failed for bookingId {}: {}", bookingId, e.getMessage());
            return Optional.empty();
        }
    }

    private String key(Long bookingId) {
        return KEY_PREFIX + bookingId;
    }
}
