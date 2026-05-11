package dto;
import java.math.BigDecimal;

public record BookingItemDTO(String ticketTier, Integer quantity, BigDecimal unitPrice) {}