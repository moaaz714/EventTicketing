package com.team22.eventticketing.sales.service;

import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.observer.EntityObserver;
import com.team22.eventticketing.sales.repository.TicketSaleRepository;
import com.team22.eventticketing.sales.strategy.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RefundWindowPolicyService {

    private final TicketSaleRepository ticketSaleRepository;
    private final CacheEvictionService cacheEviction;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final RefundStrategySelector strategySelector = new RefundStrategySelector();

    public RefundWindowPolicyService(TicketSaleRepository ticketSaleRepository,
                                     CacheEvictionService cacheEviction) {
        this.ticketSaleRepository = ticketSaleRepository;
        this.cacheEviction = cacheEviction;
    }

    public void register(EntityObserver observer) { observers.add(observer); }
    public void unregister(EntityObserver observer) { observers.remove(observer); }

    private void notifyObservers(String action, Object payload) {
        for (EntityObserver o : observers) o.onEvent(action, payload);
    }

    @Transactional
    public TicketSale processRefundWithWindowPolicy(Long saleId, String reason) {

        // 1. Find the sale — 404 if not found
        TicketSale sale = ticketSaleRepository.findById(saleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "TicketSale not found with id: " + saleId));

        // 2. Must be COMPLETED — 400 otherwise
        if (sale.getStatus() != TicketSale.SaleStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only COMPLETED sales can be refunded");
        }

        // 3. Fetch eventDate via native SQL join — 400 if booking has no event
        LocalDateTime eventDate = ticketSaleRepository.findEventDateBySaleId(saleId);
        if (eventDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "booking has no associated event");
        }

        // 4. Compute hours until event and select strategy
        long hoursUntilEvent = Duration.between(LocalDateTime.now(), eventDate).toHours();
        RefundStrategy strategy = strategySelector.select(hoursUntilEvent);
        RefundRequest request = new RefundRequest(reason);
        RefundResult result = strategy.calculateRefund(sale, request, eventDate);

        // 5. NoRefundStrategy path — log, invalidate, then throw 400
        if (strategy instanceof NoRefundStrategy) {
            Map<String, Object> details = new HashMap<>();
            details.put("saleId", saleId);
            details.put("strategyName", result.getStrategyName());
            details.put("denialReason", "refund window expired");
            details.put("hoursUntilEvent", hoursUntilEvent);
            details.put("method", sale.getMethod() != null ? sale.getMethod().name() : null);
            details.put("amount", sale.getAmount());

            // Log REFUND_DENIED to MongoDB BEFORE throwing
            notifyObservers("REFUND_DENIED", Map.of(
                    "saleId", saleId,
                    "method", sale.getMethod() != null ? sale.getMethod().name() : "",
                    "amount", sale.getAmount() != null ? sale.getAmount() : 0.0,
                    "strategyName", result.getStrategyName(),
                    "hoursUntilEvent", hoursUntilEvent
            ));

            // Invalidate caches BEFORE throwing
            cacheEviction.evictPattern("S5-F10");
            cacheEviction.evictDetail("S5-F11", saleId);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "refund window expired");
        }

        // 6. Full or Partial — update the sale
        sale.setStatus(TicketSale.SaleStatus.REFUNDED);

        Map<String, Object> details = new HashMap<>(
                sale.getTransactionDetails() != null ? sale.getTransactionDetails() : new HashMap<>());
        details.put("refundAmount", result.getRefundAmount());
        details.put("refundPolicy", result.getStrategyName());
        details.put("refundReason", reason);
        details.put("refundedAt", LocalDateTime.now().toString());
        sale.setTransactionDetails(details);

        TicketSale saved = ticketSaleRepository.save(sale);

        // 7. Log REFUNDED to MongoDB via observer
        notifyObservers("SALE_REFUNDED", Map.of(
                "saleId", saleId,
                "method", saved.getMethod() != null ? saved.getMethod().name() : "",
                "amount", saved.getAmount() != null ? saved.getAmount() : 0.0,
                "strategyName", result.getStrategyName(),
                "refundAmount", result.getRefundAmount(),
                "hoursUntilEvent", hoursUntilEvent,
                "reason", reason != null ? reason : ""
        ));

        // 8. Invalidate caches
        cacheEviction.evictPattern("S5-F10");
        cacheEviction.evictDetail("S5-F11", saleId);
        cacheEviction.evictDetail("ticket-sale", saleId);

        return saved;
    }
}