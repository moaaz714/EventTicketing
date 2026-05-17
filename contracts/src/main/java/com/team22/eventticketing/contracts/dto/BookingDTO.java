package com.team22.eventticketing.contracts.dto;
import java.math.BigDecimal;

public record BookingDTO(Long id, Long userId, Long eventId, String status, BigDecimal totalAmount) {}