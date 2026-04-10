package com.team22.eventticketing.sales.service;

import com.team22.eventticketing.sales.entity.SalePromotion;
import com.team22.eventticketing.sales.repository.SalePromotionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SalePromotionService {

    private final SalePromotionRepository salePromotionRepository;

    public SalePromotionService(SalePromotionRepository salePromotionRepository) {
        this.salePromotionRepository = salePromotionRepository;
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    public SalePromotion create(SalePromotion salePromotion) {
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
