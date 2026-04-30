package com.team22.eventticketing.sales.service;

import com.team22.eventticketing.sales.dto.PromotionUsageDTO;
import com.team22.eventticketing.sales.entity.Promotion;
import com.team22.eventticketing.sales.entity.enums.DiscountType;
import com.team22.eventticketing.sales.repository.PromotionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final CacheEvictionService cacheEviction;

    public PromotionService(PromotionRepository promotionRepository,
                            CacheEvictionService cacheEviction) {
        this.promotionRepository = promotionRepository;
        this.cacheEviction = cacheEviction;
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    public Promotion create(Promotion promotion) {
        return promotionRepository.save(promotion);
    }

    // ─── [CRUD] Read by ID ────────────────────────────────────────────────────────

    @Cacheable(cacheNames = "sales-service::promotion", key = "#id")
    public Promotion findById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Promotion not found with id: " + id));
    }

    // ─── Read all (NOT cached) ────────────────────────────────────────────────────

    public List<Promotion> findAll() {
        return promotionRepository.findAll();
    }

    // ─── [CRUD] Update ────────────────────────────────────────────────────────────

    public Promotion update(Long id, Promotion patch) {
        Promotion existing = findById(id);

        if (patch.getCode() != null)          existing.setCode(patch.getCode());
        if (patch.getDiscountType() != null)  existing.setDiscountType(patch.getDiscountType());
        if (patch.getDiscountValue() != null) existing.setDiscountValue(patch.getDiscountValue());
        if (patch.getMaxUses() != null)       existing.setMaxUses(patch.getMaxUses());
        if (patch.getCurrentUses() != null)   existing.setCurrentUses(patch.getCurrentUses());
        if (patch.getExpiryDate() != null)    existing.setExpiryDate(patch.getExpiryDate());
        if (patch.getActive() != null)        existing.setActive(patch.getActive());
        if (patch.getMetadata() != null)      existing.setMetadata(patch.getMetadata());

        Promotion saved = promotionRepository.save(existing);
        cacheEviction.evictEntityAndFeatures("promotion", id, "S5-F9");
        return saved;
    }

    // ─── [S5-F9] Top Used Promotions Report (combined) ───────────────────────────

    @Cacheable(cacheNames = "sales-service::S5-F9", key = "#limit")
    public List<PromotionUsageDTO> getTopUsedPromotions(int limit) {
        List<Object[]> rows = promotionRepository.findTopUsedPromotions(limit);
        return rows.stream().map(row -> {
            Long promotionId       = ((Number) row[0]).longValue();
            String code            = (String) row[1];
            DiscountType discType  = DiscountType.valueOf((String) row[2]);
            Double discountValue   = ((Number) row[3]).doubleValue();
            int timesUsed          = ((Number) row[4]).intValue();
            Double totalDiscount   = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;
            Boolean active         = (Boolean) row[6];
            LocalDateTime expiry   = ((Timestamp) row[7]).toLocalDateTime();
            boolean expired        = expiry.isBefore(LocalDateTime.now());

            return new PromotionUsageDTO(promotionId, code, discType, discountValue,
                    timesUsed, totalDiscount, active, expired);
        }).toList();
    }

    // ─── [CRUD] Delete ────────────────────────────────────────────────────────────

    public void delete(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Promotion not found with id: " + id);
        }
        promotionRepository.deleteById(id);
        cacheEviction.evictEntityAndFeatures("promotion", id, "S5-F9");
    }
}
