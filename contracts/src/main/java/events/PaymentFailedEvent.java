// PaymentFailedEvent.java
package events;
public record PaymentFailedEvent(Long saleId, Long bookingId, String reason) {}