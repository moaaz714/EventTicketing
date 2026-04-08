package com.team22.eventticketing.sales.repository;

import com.team22.eventticketing.sales.entity.SalePromotion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalePromotionRepository extends JpaRepository<SalePromotion, Long> {
}