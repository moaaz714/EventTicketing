// TicketCancelledEvent.java
package events;
public record TicketCancelledEvent(Long ticketId, Long bookingId) {}