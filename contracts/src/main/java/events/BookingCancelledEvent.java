// BookingCancelledEvent.java
package events;
public record BookingCancelledEvent(Long bookingId, Long userId, Long eventId, String reason) {}