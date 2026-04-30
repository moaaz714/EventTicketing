package com.team22.eventticketing.sales.service;

import com.team22.eventticketing.sales.dto.RevenueReportDTO;
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
import java.util.List;
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

    // ─── S5-F4: Process booking payment (From HEAD) ──────────────────────────────
    @Transactional
    public TicketSale processBookingPayment(Long bookingId, PaymentMethod method, String cardLastFour) {
        if (ticketSaleRepository.existsByBookingIdAndStatus(bookingId, SaleStatus.COMPLETED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "already paid");
        }

        Optional<TicketSale> pendingSale = ticketSaleRepository.findByBookingIdAndStatus(bookingId, SaleStatus.PENDING);
        TicketSale ticketSale = pendingSale.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        ticketSale.setStatus(SaleStatus.COMPLETED);

        Map<String, Object> transactionDetails = new HashMap<>();
        transactionDetails.put("method", method.name());
        if (cardLastFour != null) transactionDetails.put("cardLastFour", cardLastFour);
        transactionDetails.put("gatewayResponse", "approved");
        transactionDetails.put("paidAt", LocalDateTime.now().toString());

        ticketSale.setTransactionDetails(transactionDetails);
        return ticketSaleRepository.save(ticketSale);
    }

    // ─── S5-F6: Revenue Report (From origin/main) ────────────────────────────────
    public RevenueReportDTO getRevenueReport(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }

        List<TicketSale> completed = ticketSaleRepository.findByStatusAndCreatedAtBetween(
                SaleStatus.COMPLETED, startDate, endDate);
        double totalRevenue = completed.stream().mapToDouble(TicketSale::getAmount).sum();
        int totalTransactions = completed.size();
        double averageSale = totalTransactions == 0 ? 0 : totalRevenue / totalTransactions;

        List<TicketSale> refunded = ticketSaleRepository.findByStatusAndCreatedAtBetween(
                SaleStatus.REFUNDED, startDate, endDate);
        double refundedAmount = refunded.stream().mapToDouble(TicketSale::getAmount).sum();
        int refundCount = refunded.size();

        return RevenueReportDTO.builder()
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .averageSale(averageSale)
                .refundedAmount(refundedAmount)
                .refundCount(refundCount)
                .build();
    }

    // ─── S5-F5: Apply Promotion (Merged) ─────────────────────────────────────────
    @Transactional
    public TicketSale applyPromotionToSale(Long saleId, Long promotionId) {
        TicketSale ticketSale = ticketSaleRepository.findById(saleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket sale not found"));

        if (ticketSale.getStatus() != SaleStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cannot apply promotion to a completed/cancelled sale");
        }

        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found"));

        if (!promotion.getActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion is inactive");
        }
        if (promotion.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion has expired");
        }
        if (promotion.getCurrentUses() >= promotion.getMaxUses()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion usage limit reached");
        }

        boolean alreadyApplied = salePromotionRepository.existsByTicketSaleIdAndPromotionId(saleId, promotionId);
        if (alreadyApplied) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "promotion already applied");
        }

        SalePromotion salePromo = createSalePromotion(ticketSale, promotion);
        salePromo.setAppliedAt(LocalDateTime.now());

        promotion.setCurrentUses(promotion.getCurrentUses() + 1);

        salePromotionRepository.save(salePromo);
        promotionRepository.save(promotion);
        ticketSale.getSalePromotions().add(salePromo);

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

        if (discount > sale.getAmount()) discount = sale.getAmount();

        SalePromotion salePromo = new SalePromotion();
        salePromo.setTicketSale(sale);
        salePromo.setPromotion(promotion);
        salePromo.setDiscountApplied(discount);
        return salePromo;
    }
}