package com.team22.eventticketing.sales.service;

import com.team22.eventticketing.sales.entity.Promotion;
import com.team22.eventticketing.sales.entity.SalePromotion;
import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.entity.enums.DiscountType;
import com.team22.eventticketing.sales.entity.enums.SaleStatus;
import com.team22.eventticketing.sales.entity.enums.PaymentMethod;
import com.team22.eventticketing.sales.repository.PromotionRepository;
import com.team22.eventticketing.sales.repository.SalePromotionRepository;
import com.team22.eventticketing.sales.repository.TicketSaleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SalesService {

    private final TicketSaleRepository ticketSaleRepository;
    private final PromotionRepository promotionRepository;
    private final SalePromotionRepository salePromotionRepository;

    public SalesService(TicketSaleRepository ticketSaleRepository,
                        PromotionRepository promotionRepository,
                        SalePromotionRepository salePromotionRepository) {
        this.ticketSaleRepository = ticketSaleRepository;
        this.promotionRepository = promotionRepository;
        this.salePromotionRepository = salePromotionRepository;
    }

    // ─── Process booking payment ────────────────────────────────────────────────
    @Transactional
    public TicketSale processBookingPayment(Long bookingId, PaymentMethod method, String cardLastFour) {

        // Check if a COMPLETED ticket sale already exists
        if (ticketSaleRepository.existsByBookingIdAndStatus(bookingId, SaleStatus.COMPLETED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "already paid");
        }

        // Fetch the PENDING ticket sale
        Optional<TicketSale> pendingSale = ticketSaleRepository.findByBookingIdAndStatus(bookingId, SaleStatus.PENDING);
        TicketSale ticketSale = pendingSale.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        // Update TicketSale status to COMPLETED
        ticketSale.setStatus(SaleStatus.COMPLETED);

        // Populate transactionDetails JSONB
        Map<String, Object> transactionDetails = new HashMap<>();
        transactionDetails.put("method", method.name());
        if (cardLastFour != null) transactionDetails.put("cardLastFour", cardLastFour);
        transactionDetails.put("gatewayResponse", "approved");
        transactionDetails.put("paidAt", LocalDateTime.now().toString());

        ticketSale.setTransactionDetails(transactionDetails);

        // Save and return
        return ticketSaleRepository.save(ticketSale);
    }

    // ─── Apply promotion to ticket sale ─────────────────────────────────────────
    @Transactional
    public TicketSale applyPromotionToSale(Long saleId, Long promotionId) {
        // 1. Find the ticket sale
        TicketSale ticketSale = ticketSaleRepository.findById(saleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket sale not found"));

        // 2. Validate status
        if (ticketSale.getStatus() != SaleStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cannot apply promotion to a completed/cancelled sale");
        }

        // 3. Find the promotion
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found"));

        // 4. Validate promotion
        if (!promotion.getActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion is inactive");
        }
        if (promotion.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion has expired");
        }
        if (promotion.getCurrentUses() >= promotion.getMaxUses()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion usage limit reached");
        }

        // 5. Check for duplicate
        boolean alreadyApplied = salePromotionRepository.existsByTicketSaleAndPromotion(ticketSale, promotion);
        if (alreadyApplied) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "promotion already applied");
        }

        // 6. Create SalePromotion
        SalePromotion salePromo = createSalePromotion(ticketSale, promotion);
        salePromo.setAppliedAt(LocalDateTime.now());

        // 7. Update promotion usage
        promotion.setCurrentUses(promotion.getCurrentUses() + 1);

        // 8. Save
        salePromotionRepository.save(salePromo);
        promotionRepository.save(promotion);

        return ticketSale;
    }

    // ─── Helper method to create SalePromotion and calculate discount ──────────
    private SalePromotion createSalePromotion(TicketSale sale, Promotion promotion) {
        double discount;
        if (promotion.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = sale.getAmount() * promotion.getDiscountValue() / 100.0;
        } else {
            discount = promotion.getDiscountValue();
        }

        // Cap discount so it never exceeds sale amount
        if (discount > sale.getAmount()) discount = sale.getAmount();

        SalePromotion salePromo = new SalePromotion();
        salePromo.setTicketSale(sale);
        salePromo.setPromotion(promotion);
        salePromo.setDiscountApplied(discount);

        return salePromo;
    }
}