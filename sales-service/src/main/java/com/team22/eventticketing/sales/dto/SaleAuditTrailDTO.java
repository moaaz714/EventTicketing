package com.team22.eventticketing.sales.dto;

import java.util.List;

public record SaleAuditTrailDTO(
        Long saleId,
        List<AuditEventDTO> events
) {}
