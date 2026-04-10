package com.team22.eventticketing.ticket.controller;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets") // Requirement 4.a [cite: 98]
public class ticketController {

    @GetMapping("/health")
    public String statusCheck(){
        return "OK";
    }
}
