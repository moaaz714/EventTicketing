package com.team22.eventticketing.sales.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record AuditEventDTO(
        String action,
        LocalDateTime timestamp,
        String method,
        Double amount,
        Map<String, Object> details
) {}
