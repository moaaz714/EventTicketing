package com.team22.eventticketing.event;

import feign.BookingServiceClient;
import feign.TicketServiceClient;
import feign.UserServiceClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "com.team22.eventticketing.event",
        "dto"
})
@EnableFeignClients(clients = {
        BookingServiceClient.class,
        UserServiceClient.class,
        TicketServiceClient.class
})
@EnableCaching
public class EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}