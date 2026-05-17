package com.team22.eventticketing.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import feign.EventServiceClient;
import feign.UserServiceClient;
import feign.TicketServiceClient;
import feign.BookingServiceClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(clients = {EventServiceClient.class, UserServiceClient.class, TicketServiceClient.class, BookingServiceClient.class})
@EnableCaching
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }

}
