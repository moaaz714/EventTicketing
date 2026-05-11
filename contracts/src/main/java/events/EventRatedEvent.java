// EventRatedEvent.java
package events;
public record EventRatedEvent(Long eventId, Long bookingId, Double rating, Long userId) {}