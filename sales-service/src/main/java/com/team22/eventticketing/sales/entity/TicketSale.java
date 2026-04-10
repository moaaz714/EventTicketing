package com.team22.eventticketing.sales.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team22.eventticketing.sales.entity.enums.PaymentMethod;
import com.team22.eventticketing.sales.entity.enums.SaleStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a ticket sale transaction.
 *
 * Table: ticket_sales
 *
 * bookingId and userId are plain Long FK references — not JPA-managed relationships
 * because Booking and User live in different services.
 *
 * JSONB: transactionDetails stores gateway response, card info, receipt URL, etc.
 *
 * Relationship: One TicketSale → Many SalePromotions.
 * TicketSale is the inverse side (mappedBy = "ticketSale").
 * SalePromotion is the owner side (holds FK ticket_sale_id).
 */
@Entity
@Table(name = "ticket_sales")
public class TicketSale {

    // ─── Primary Key ──────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Cross-Service FK References (plain Long — not JPA-managed) ──────────────

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Long userId;

    // ─── Core Fields ──────────────────────────────────────────────────────────────

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus status;

    // ─── JSONB Column ─────────────────────────────────────────────────────────────

    /**
     * Stores flexible transaction metadata as JSONB.
     *
     * Example keys:
     *   - gatewayResponse  (String)  e.g. "approved" or "declined"
     *   - cardLastFour     (String)  e.g. "4242"
     *   - receiptUrl       (String)  URL to receipt
     *   - failureReason    (String)  reason if FAILED
     *   - bookingReference (String)  e.g. "BK-2026-0042"
     *   - retryAttempt     (Integer) number of retry attempts
     *   - refundReason     (String)  set on refund (S5-F2)
     *   - refundedAt       (String)  ISO timestamp set on refund (S5-F2)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> transactionDetails = new HashMap<>();

    // ─── Audit ───────────────────────────────────────────────────────────────────

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // ─── Relationships ────────────────────────────────────────────────────────────

    /**
     * Inverse side of the TicketSale ↔ SalePromotion bidirectional relationship.
     * SalePromotion is the owner side (holds FK ticket_sale_id).
     * @JsonIgnore prevents circular serialisation.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "ticketSale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalePromotion> salePromotions = new ArrayList<>();

    // ─── Constructors ─────────────────────────────────────────────────────────────

    public TicketSale() {}

    public TicketSale(Long bookingId, Long userId, Double amount, PaymentMethod method, SaleStatus status) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
        this.method = method;
        this.status = status;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public SaleStatus getStatus() { return status; }
    public void setStatus(SaleStatus status) { this.status = status; }

    public Map<String, Object> getTransactionDetails() { return transactionDetails; }
    public void setTransactionDetails(Map<String, Object> transactionDetails) { this.transactionDetails = transactionDetails; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<SalePromotion> getSalePromotions() { return salePromotions; }
    public void setSalePromotions(List<SalePromotion> salePromotions) { this.salePromotions = salePromotions; }
}
