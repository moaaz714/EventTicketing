// PaymentInitiatedEvent.java
package events;
import java.math.BigDecimal;
public record PaymentInitiatedEvent(Long saleId, Long bookingId, BigDecimal amount) {}