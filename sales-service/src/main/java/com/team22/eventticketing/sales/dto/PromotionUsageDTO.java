package com.team22.eventticketing.sales.dto;

import static com.team22.eventticketing.sales.entity.Promotion.DiscountType;

public record PromotionUsageDTO(
    Long promotionId,
    String code,
    DiscountType discountType,
    Double discountValue,
    Integer timesUsed,
    Double totalDiscountGiven,
    Boolean active,
    Boolean expired
) {}
