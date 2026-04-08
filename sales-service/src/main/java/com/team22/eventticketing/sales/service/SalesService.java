package com.team22.eventticketing.sales.service;

import com.team22.eventticketing.sales.entity.TicketSale;
import com.team22.eventticketing.sales.entity.enums.SaleStatus;
import com.team22.eventticketing.sales.entity.enums.PaymentMethod;
import com.team22.eventticketing.sales.repository.TicketSaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SalesService {

    private final TicketSaleRepository ticketSaleRepository;

    public SalesService(TicketSaleRepository ticketSaleRepository) {
        this.ticketSaleRepository = ticketSaleRepository;
    }

    @Transactional
    public TicketSale processBookingPayment(Long bookingId, PaymentMethod method, String cardLastFour) {

        // 1. Check if a COMPLETED ticket sale already exists for this booking
        if (ticketSaleRepository.existsByBookingIdAndStatus(bookingId, SaleStatus.COMPLETED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "already paid");
        }

        // 2. Fetch the PENDING ticket sale
        Optional<TicketSale> pendingSale = ticketSaleRepository.findByBookingIdAndStatus(bookingId, SaleStatus.PENDING);

        TicketSale ticketSale = pendingSale.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        // 3. Update TicketSale status to COMPLETED
        ticketSale.setStatus(SaleStatus.COMPLETED);

        // 4. Populate transactionDetails JSONB
        Map<String, Object> transactionDetails = new HashMap<>();
        transactionDetails.put("method", method.name());
        if (cardLastFour != null) transactionDetails.put("cardLastFour", cardLastFour);
        transactionDetails.put("gatewayResponse", "approved");
        transactionDetails.put("paidAt", LocalDateTime.now().toString());

        ticketSale.setTransactionDetails(transactionDetails);

        // 5. Save and return the updated TicketSale
        return ticketSaleRepository.save(ticketSale);
    }
}