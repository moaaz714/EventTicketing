package com.team22.eventticketing.user.controller;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users") // Requirement 4.a [cite: 98]
public class userController {

    @GetMapping("/health")
    public String statusCheck(){
        return "OK";
    }
}
