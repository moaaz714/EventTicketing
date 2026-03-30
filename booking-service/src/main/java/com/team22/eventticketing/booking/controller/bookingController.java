package com.team22.eventticketing.booking.controller;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings") // Requirement 4.a [cite: 98]
public class bookingController {

    @GetMapping("/health")
    public String statusCheck(){
        return "OK";
    }
}
