package com.team22.eventticketing.contracts.dto;
import java.time.LocalDateTime;

public record EventDTO(Long id, String name, String status, LocalDateTime eventDate) {}