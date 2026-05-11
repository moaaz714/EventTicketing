// EventStatusChangedEvent.java
package events;
public record EventStatusChangedEvent(Long eventId, String oldStatus, String newStatus) {}