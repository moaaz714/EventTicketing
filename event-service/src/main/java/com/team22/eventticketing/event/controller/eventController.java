package com.team22.eventticketing.event.controller;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events") // Requirement 4.a [cite: 98]
public class eventController {

    @GetMapping("/health")
    public String statusCheck(){
        return "OK";
    }
}
