package com.team22.eventticketing.sales.controller;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales") // Requirement 4.a [cite: 98]
public class salesController {

    @GetMapping("/health")
    public String statusCheck(){
        return "OK";
    }
}
