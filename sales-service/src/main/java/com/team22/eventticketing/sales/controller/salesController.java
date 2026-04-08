package com.team22.eventticketing.sales.controller;

import com.team22.eventticketing.sales.dto.RevenueReportDTO;   // import your DTO
import com.team22.eventticketing.sales.service.SalesService;   // import your service
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales") // Requirement 4.a [cite: 98]
public class salesController {
    private final SalesService salesService;

    public salesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping("/health")
    public String statusCheck(){
        return "OK";
    }
    @GetMapping("/reports/revenue")
    public RevenueReportDTO getRevenueReport(@RequestParam String startDate,
                                             @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        return salesService.getRevenueReport(startDateTime, endDateTime);
    }
}
