// BookingPlacedEvent.java
package events;
public record BookingPlacedEvent(Long bookingId, Long userId, Long eventId) {}