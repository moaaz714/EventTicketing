// TicketIssuedEvent.java
package events;
public record TicketIssuedEvent(Long ticketId, Long bookingId, String attendeeName, String ticketCode) {}