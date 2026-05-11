package dto;
import java.math.BigDecimal;

public record EventBookingRevenueDTO(long totalBookings, BigDecimal totalRevenue, BigDecimal averageBookingAmount) {}