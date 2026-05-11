// PaymentRefundedEvent.java
package events;
import java.math.BigDecimal;
public record PaymentRefundedEvent(Long saleId, Long bookingId, BigDecimal refundAmount) {}