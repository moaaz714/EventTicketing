package com.team22.eventticketing.sales.service;

import com.team22.eventticketing.sales.dto.AppliedPromotionDTO;
import com.team22.eventticketing.sales.dto.SaleDetailsDTO;
import com.team22.eventticketing.sales.dto.UserSaleSummaryDTO;
import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.entity.enums.PaymentMethod;
import com.team22.eventticketing.sales.entity.enums.SaleStatus;
import com.team22.eventticketing.sales.observer.EntityObserver;
import com.team22.eventticketing.sales.repository.TicketSaleRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class TicketSaleService {

    private final TicketSaleRepository ticketSaleRepository;
    private final CacheEvictionService cacheEviction;
    private final List<EntityObserver> observers = new ArrayList<>();

    public TicketSaleService(TicketSaleRepository ticketSaleRepository,
                             CacheEvictionService cacheEviction) {
        this.ticketSaleRepository = ticketSaleRepository;
        this.cacheEviction = cacheEviction;
    }

    public void register(EntityObserver observer) { observers.add(observer); }
    public void unregister(EntityObserver observer) { observers.remove(observer); }

    private void notifyObservers(String action, Object payload) {
        for (EntityObserver o : observers) o.onEvent(action, payload);
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    public TicketSale create(TicketSale ticketSale) {
        TicketSale saved = ticketSaleRepository.save(ticketSale);
        notifyObservers("SALE_CREATED", Map.of("saleId", saved.getId()));
        return saved;
    }

    // ─── [CRUD] Read by ID ────────────────────────────────────────────────────────

    @Cacheable(cacheNames = "sales-service::ticket-sale", key = "#id")
    public TicketSale findById(Long id) {
        return ticketSaleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "TicketSale not found with id: " + id));
    }

    // ─── Read all (NOT cached) ────────────────────────────────────────────────────

    public List<TicketSale> findAll() {
        return ticketSaleRepository.findAll();
    }

    // ─── [CRUD] Update ────────────────────────────────────────────────────────────

    public TicketSale update(Long id, TicketSale patch) {
        TicketSale existing = findById(id);

        if (patch.getBookingId() != null)           existing.setBookingId(patch.getBookingId());
        if (patch.getUserId() != null)              existing.setUserId(patch.getUserId());
        if (patch.getAmount() != null)              existing.setAmount(patch.getAmount());
        if (patch.getMethod() != null)              existing.setMethod(patch.getMethod());
        if (patch.getStatus() != null)              existing.setStatus(patch.getStatus());
        if (patch.getTransactionDetails() != null)  existing.setTransactionDetails(patch.getTransactionDetails());

        TicketSale saved = ticketSaleRepository.save(existing);
        cacheEviction.evictEntityAndFeatures("ticket-sale", id,
                "S5-F1", "S5-F3", "S5-F6", "S5-F8", "S5-F10", "S5-F11");
        notifyObservers("SALE_UPDATED", Map.of("saleId", saved.getId()));
        return saved;
    }

    // ─── [CRUD] Delete ────────────────────────────────────────────────────────────

    public void delete(Long id) {
        if (!ticketSaleRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "TicketSale not found with id: " + id);
        }
        ticketSaleRepository.deleteById(id);
        cacheEviction.evictEntityAndFeatures("ticket-sale", id,
                "S5-F1", "S5-F3", "S5-F6", "S5-F8", "S5-F10", "S5-F11");
        notifyObservers("SALE_DELETED", Map.of("saleId", id));
    }

    // ─── [S5-F2] Process Refund (write — invalidates) ────────────────────────────

    @Transactional
    public TicketSale refund(Long id, String reason) {
        TicketSale sale = findById(id);

        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only COMPLETED sales can be refunded");
        }

        sale.setStatus(SaleStatus.REFUNDED);
        sale.getTransactionDetails().put("refundReason", reason);
        sale.getTransactionDetails().put("refundedAt", LocalDateTime.now().toString());

        TicketSale saved = ticketSaleRepository.save(sale);
        cacheEviction.evictEntityAndFeatures("ticket-sale", id,
                "S5-F1", "S5-F3", "S5-F6", "S5-F8", "S5-F10", "S5-F11");
        notifyObservers("SALE_REFUNDED", Map.of("saleId", saved.getId(), "reason", reason != null ? reason : ""));
        return saved;
    }

    // ─── [S5-F3] User Ticket Sale Summary (DTO) ──────────────────────────────────

    @Cacheable(cacheNames = "sales-service::S5-F3", key = "#userId")
    public UserSaleSummaryDTO getUserSummary(Long userId) {
        if (ticketSaleRepository.countUserById(userId) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found with id: " + userId);
        }

        List<Object[]> rows = ticketSaleRepository.findSalesSummaryByUserId(userId);

        Map<String, Double> methodBreakdown = new HashMap<>();
        long totalSales = 0;
        double totalAmount = 0.0;

        for (Object[] row : rows) {
            String method = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double total = ((Number) row[2]).doubleValue();
            methodBreakdown.put(method, total);
            totalSales += count;
            totalAmount += total;
        }

        return UserSaleSummaryDTO.builder()
                .userId(userId)
                .totalSales(totalSales)
                .totalAmount(totalAmount)
                .methodBreakdown(methodBreakdown)
                .build();
    }

    // ─── [S5-F1] Search Sales ────────────────────────────────────────────────────

    @Cacheable(cacheNames = "sales-service::S5-F1",
            key = "T(String).valueOf(#status) + ':' + T(String).valueOf(#startDate) + ':' + T(String).valueOf(#endDate)")
    public List<TicketSale> search(String status, String startDate, String endDate) {
        LocalDateTime start = (startDate != null)
                ? LocalDate.parse(startDate).atStartOfDay()
                : LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        LocalDateTime end = (endDate != null)
                ? LocalDate.parse(endDate).atTime(23, 59, 59)
                : LocalDateTime.of(9999, 12, 31, 23, 59, 59);

        return ticketSaleRepository.findByStatusAndDateRange(status, start, end);
    }

    // ─── [S5-F7] Retry Failed Sale (write — invalidates) ─────────────────────────

    @Transactional
    public TicketSale retry(Long id) {
        TicketSale sale = findById(id);

        if (sale.getStatus() != SaleStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only FAILED sales can be retried");
        }

        sale.setStatus(SaleStatus.COMPLETED);

        Map<String, Object> details = sale.getTransactionDetails();
        if (details == null) {
            details = new HashMap<>();
            sale.setTransactionDetails(details);
        }

        int retryAttempt = details.containsKey("retryAttempt")
                ? ((Number) details.get("retryAttempt")).intValue() + 1
                : 1;
        details.put("retryAttempt", retryAttempt);
        details.put("gatewayResponse", "approved");

        TicketSale saved = ticketSaleRepository.save(sale);
        cacheEviction.evictEntityAndFeatures("ticket-sale", id,
                "S5-F1", "S5-F3", "S5-F6", "S5-F8", "S5-F10", "S5-F11");
        notifyObservers("SALE_RETRIED", Map.of("saleId", saved.getId()));
        return saved;
    }

    // ─── [S5-F8] Sale Details with Applied Promotions (relationship DTO) ──────────

    @Cacheable(cacheNames = "sales-service::S5-F8", key = "#saleId")
    @Transactional(readOnly = true)
    public SaleDetailsDTO getSaleDetails(Long saleId) {
        TicketSale sale = ticketSaleRepository.findByIdWithPromotions(saleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "TicketSale not found with id: " + saleId));

        List<AppliedPromotionDTO> appliedPromotions = sale.getSalePromotions().stream()
                .map(sp -> new AppliedPromotionDTO(
                        sp.getPromotion().getCode(),
                        sp.getPromotion().getDiscountType(),
                        sp.getDiscountApplied(),
                        sp.getAppliedAt()))
                .toList();

        double totalDiscount = appliedPromotions.stream()
                .mapToDouble(AppliedPromotionDTO::getDiscountApplied)
                .sum();

        //double finalAmount = sale.getAmount() - totalDiscount;
        double finalAmount = Math.max(0, sale.getAmount() - totalDiscount);

        return SaleDetailsDTO.builder()
                .saleId(sale.getId())
                .bookingId(sale.getBookingId())
                .userId(sale.getUserId())
                .originalAmount(sale.getAmount())
                .method(sale.getMethod())
                .status(sale.getStatus())
                .transactionDetails(sale.getTransactionDetails())
                .appliedPromotions(appliedPromotions)
                .totalDiscount(totalDiscount)
                .finalAmount(finalAmount)
                .build();
    }

    // ─── [S5-F4] Process Booking Payment (write — invalidates) ───────────────────

    @Transactional
    public TicketSale processBookingPayment(Long bookingId, PaymentMethod method, String cardLastFour) {
        String bookingStatus = ticketSaleRepository.findBookingStatus(bookingId);
        if (bookingStatus == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found: " + bookingId);
        }
        if (!"COMPLETED".equals(bookingStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is not COMPLETED");
        }

        TicketSale sale = ticketSaleRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "TicketSale not found for booking: " + bookingId));

        if (sale.getStatus() == SaleStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "already paid");
        }

        sale.setMethod(method);
        sale.setStatus(SaleStatus.COMPLETED);

        if (sale.getTransactionDetails() == null) sale.setTransactionDetails(new java.util.HashMap<>());
        sale.getTransactionDetails().put("paymentMethod", method.toString());
        sale.getTransactionDetails().put("cardLastFour", cardLastFour);
        sale.getTransactionDetails().put("paidAt", java.time.LocalDateTime.now().toString());

        TicketSale saved = ticketSaleRepository.save(sale);
        cacheEviction.evictEntityAndFeatures("ticket-sale", saved.getId(),
                "S5-F1", "S5-F3", "S5-F6", "S5-F10", "S5-F11");
        notifyObservers("PAYMENT_PROCESSED", Map.of("saleId", saved.getId(), "bookingId", bookingId,
                "method", method.name(), "amount", saved.getAmount() != null ? saved.getAmount() : 0.0));
        return saved;
    }
}
