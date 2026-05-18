package com.team22.eventticketing.event;

import com.team22.eventticketing.contracts.feign.BookingServiceClient;
import com.team22.eventticketing.contracts.feign.TicketServiceClient;
import com.team22.eventticketing.contracts.feign.UserServiceClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
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