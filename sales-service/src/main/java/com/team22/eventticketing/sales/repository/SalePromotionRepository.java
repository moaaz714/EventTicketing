package com.team22.eventticketing.sales.repository;

import com.team22.eventticketing.sales.entity.SalePromotion;
import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalePromotionRepository extends JpaRepository<SalePromotion, Long> {

    boolean existsByTicketSaleAndPromotion(TicketSale sale, Promotion promo);
}