// TicketStatusChangedEvent.java
package events;
public record TicketStatusChangedEvent(Long ticketId, Long bookingId, String newStatus) {}