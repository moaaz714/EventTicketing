package com.team22.eventticketing.sales.repository;

import com.team22.eventticketing.sales.entity.SalePromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalePromotionRepository extends JpaRepository<SalePromotion, Long> {

    List<SalePromotion> findByTicketSaleId(Long ticketSaleId);

    // [S5-F5] Duplicate check — has this promotion already been applied to this sale?
    boolean existsByTicketSaleIdAndPromotionId(Long ticketSaleId, Long promotionId);
}