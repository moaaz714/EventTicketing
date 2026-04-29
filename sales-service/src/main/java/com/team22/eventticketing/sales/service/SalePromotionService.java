package com.team22.eventticketing.sales.service;

import com.team22.eventticketing.sales.entity.SalePromotion;
import com.team22.eventticketing.sales.repository.SalePromotionRepository;
import com.team22.eventticketing.sales.repository.TicketSaleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SalePromotionService {

    private final SalePromotionRepository salePromotionRepository;
    private final TicketSaleRepository ticketSaleRepository;

    public SalePromotionService(SalePromotionRepository salePromotionRepository,
                                TicketSaleRepository ticketSaleRepository) {
        this.salePromotionRepository = salePromotionRepository;
        this.ticketSaleRepository = ticketSaleRepository;
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    public SalePromotion create(SalePromotion salePromotion) {
        if (salePromotion.getTicketSale() != null && salePromotion.getTicketSale().getId() != null
                && salePromotion.getDiscountApplied() != null) {
            ticketSaleRepository.findById(salePromotion.getTicketSale().getId()).ifPresent(sale -> {
                if (salePromotion.getDiscountApplied() > sale.getAmount()) {
                    salePromotion.setDiscountApplied(sale.getAmount());
                }
            });
        }
        return salePromotionRepository.save(salePromotion);
    }

    // ─── Read by ID ───────────────────────────────────────────────────────────────

    public SalePromotion findById(Long id) {
        return salePromotionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "SalePromotion not found with id: " + id));
    }

    // ─── Read all ─────────────────────────────────────────────────────────────────

    public List<SalePromotion> findAll() {
        return salePromotionRepository.findAll();
    }

    // ─── Update ───────────────────────────────────────────────────────────────────

    public SalePromotion update(Long id, SalePromotion patch) {
        SalePromotion existing = findById(id);

        if (patch.getDiscountApplied() != null) existing.setDiscountApplied(patch.getDiscountApplied());
        if (patch.getAppliedAt() != null)       existing.setAppliedAt(patch.getAppliedAt());
        if (patch.getTicketSale() != null)      existing.setTicketSale(patch.getTicketSale());
        if (patch.getPromotion() != null)       existing.setPromotion(patch.getPromotion());

        return salePromotionRepository.save(existing);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────────

    public void delete(Long id) {
        if (!salePromotionRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "SalePromotion not found with id: " + id);
        }
        salePromotionRepository.deleteById(id);
    }
}
