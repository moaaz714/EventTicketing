// UserRegisteredEvent.java
package events;
public record UserRegisteredEvent(Long userId, String email, String role) {}