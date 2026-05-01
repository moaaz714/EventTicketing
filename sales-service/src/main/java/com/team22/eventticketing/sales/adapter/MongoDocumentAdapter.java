package com.team22.eventticketing.sales.adapter;

import com.team22.eventticketing.sales.dto.*;
import com.team22.eventticketing.sales.entity.enums.DiscountType;
import com.team22.eventticketing.sales.entity.enums.PaymentMethod;
import com.team22.eventticketing.sales.entity.enums.SaleStatus;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MongoDocumentAdapter {


    public RevenueReportDTO adaptToRevenueReportDTO(Document doc) {

        double totalRevenue = doc.getDouble("totalRevenue");
        int totalTransactions = doc.getInteger("totalTransactions");
        double avgSale = doc.getDouble("averageSale");
        double refunded = doc.getDouble("refundedAmount");
        int refundCount = doc.getInteger("refundCount");

//        return new RevenueReportDTO(
//                totalRevenue,
//                totalTransactions,
//                avgSale,
//                refunded,
//                refundCount
//        );
        return RevenueReportDTO.builder()
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .averageSale(avgSale)
                .refundedAmount(refunded)
                .refundCount(refundCount)
                .build();
    }
    public RevenueReportDTO adapt(Document doc) {
        return adaptToRevenueReportDTO(doc);
    }

    public PromotionUsageDTO adaptToPromotionUsageDTO(Document doc) {

        return PromotionUsageDTO.builder()
                .promotionId(doc.getLong("promotionId"))
                .code(doc.getString("code"))
                .discountType(DiscountType.valueOf(doc.getString("discountType")))
                .discountValue(doc.getDouble("discountValue"))
                .timesUsed(doc.getInteger("timesUsed"))
                .totalDiscountGiven(doc.getDouble("totalDiscountGiven"))
                .active(doc.getBoolean("active"))
                .expired(doc.getBoolean("expired"))
                .build();
    }
    public PromotionUsageDTO adapt(Document doc) {
        return adaptToPromotionUsageDTO(doc);
    }
    public AppliedPromotionDTO adapt(Document doc) {
        return adaptToAppliedPromotionDTO(doc);
    }
    public AppliedPromotionDTO adaptToAppliedPromotionDTO(Document doc) {

        AppliedPromotionDTO dto = new AppliedPromotionDTO();

        dto.setPromotionCode(doc.getString("promotionCode"));

        dto.setDiscountType(
                DiscountType.valueOf(doc.getString("discountType"))
        );

        dto.setDiscountApplied(doc.getDouble("discountApplied"));

        dto.setAppliedAt(
                doc.get("appliedAt", LocalDateTime.class)
        );

        return dto;
    }
    public UserSaleSummaryDTO adapt(Document doc) {
        return adaptToUserSaleSummaryDTO(doc);
    }
    public UserSaleSummaryDTO adaptToUserSaleSummaryDTO(Document doc) {

        return UserSaleSummaryDTO.builder()
                .userId(doc.getLong("userId"))
                .totalSales(doc.getLong("totalSales"))
                .totalAmount(doc.getDouble("totalAmount"))
                .methodBreakdown((Map<String, Double>) doc.get("methodBreakdown"))
                .build();
    }
    public SaleDetailsDTO adapt(Document doc) {
        return adaptToSaleDetailsDTO(doc);
    }
    public SaleDetailsDTO adaptToSaleDetailsDTO(Document doc) {

        return SaleDetailsDTO.builder()
                .saleId(doc.getLong("saleId"))
                .bookingId(doc.getLong("bookingId"))
                .userId(doc.getLong("userId"))
                .originalAmount(doc.getDouble("originalAmount"))
                .method(PaymentMethod.valueOf(doc.getString("method")))
                .status(SaleStatus.valueOf(doc.getString("status")))
                .transactionDetails((Map<String, Object>) doc.get("transactionDetails"))
                .appliedPromotions(promotions)
                .totalDiscount(doc.getDouble("totalDiscount"))
                .finalAmount(doc.getDouble("finalAmount"))
                .build();
    }
}