package com.team22.eventticketing.sales.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.team22.eventticketing.sales.entity.DiscountType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a promotional discount campaign.
 *
 * Table: promotions
 *
 * discountType determines how the discount is calculated:
 *   - PERCENTAGE: discount = sale.amount * discountValue / 100
 *   - FIXED:      discount = discountValue (capped at sale amount)
 *
 * JSONB: metadata stores eligible categories, min booking amount, terms, etc.
 *
 * Relationship: One Promotion → Many SalePromotions.
 * Promotion is the inverse side (mappedBy = "promotion").
 * SalePromotion is the owner side (holds FK promotion_id).
 */
@Entity
@Table(name = "promotions")
public class Promotion {

    // ─── Primary Key ──────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Core Fields ──────────────────────────────────────────────────────────────

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private Double discountValue;

    @Column(nullable = false)
    private Integer maxUses;

    @Column(nullable = false)
    private Integer currentUses = 0;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean active = true;

    // ─── JSONB Column ─────────────────────────────────────────────────────────────

    /**
     * Stores flexible promotion metadata as JSONB.
     *
     * Example keys:
     *   - eligibleCategories    (List)   e.g. ["CONCERT", "FESTIVAL"]
     *   - minimumBookingAmount  (Double) minimum sale amount to qualify
     *   - termsAndConditions    (String) human-readable terms
     *   - applicableEventIds    (List)   specific event IDs this applies to
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    // ─── Relationships ────────────────────────────────────────────────────────────

    /**
     * Inverse side of the Promotion ↔ SalePromotion bidirectional relationship.
     * SalePromotion is the owner side (holds FK promotion_id).
     * @JsonIgnore prevents circular serialisation.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalePromotion> salePromotions = new ArrayList<>();

    // ─── Constructors ─────────────────────────────────────────────────────────────

    public Promotion() {}

    public Promotion(String code, DiscountType discountType, Double discountValue,
                     Integer maxUses, LocalDateTime expiryDate) {
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxUses = maxUses;
        this.expiryDate = expiryDate;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }

    public Double getDiscountValue() { return discountValue; }
    public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }

    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }

    public Integer getCurrentUses() { return currentUses; }
    public void setCurrentUses(Integer currentUses) { this.currentUses = currentUses; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public List<SalePromotion> getSalePromotions() { return salePromotions; }
    public void setSalePromotions(List<SalePromotion> salePromotions) { this.salePromotions = salePromotions; }
}
