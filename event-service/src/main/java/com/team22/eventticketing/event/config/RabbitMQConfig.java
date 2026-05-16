package com.team22.eventticketing.event.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ==========================================
    // JSON CONVERTER (Required for Java Records)
    // ==========================================
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    // ==========================================
    // 1. EXCHANGES
    // ==========================================

    // Producer Exchange (S2-EVENTS requirement)
    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange("event.events");
    }

    // Consumer Exchange Reference (Where your listener gets messages from)
    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange("booking.events");
    }

    // Dead Letter Exchange
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange("event.dlx");
    }

    // ==========================================
    // 2. QUEUES (With DLQ wiring per rubric)
    // ==========================================

    @Bean
    public Queue sagaListenerDlq() {
        return QueueBuilder.durable("event.booking.saga-listener.dlq").build();
    }

    @Bean
    public Queue sagaListenerQueue() {
        return QueueBuilder.durable("event.booking.saga-listener")
                .withArgument("x-dead-letter-exchange", "event.dlx")
                .withArgument("x-dead-letter-routing-key", "event.booking.saga-listener.dlq")
                .build();
    }

    // ==========================================
    // 3. BINDINGS
    // ==========================================

    // Bind DLQ to DLX
    @Bean
    public Binding bindDlq() {
        return BindingBuilder.bind(sagaListenerDlq())
                .to(deadLetterExchange())
                .with("event.booking.saga-listener.dlq");
    }

    // Bind Listener Queue to Booking events
    @Bean
    public Binding bindPlaced() {
        return BindingBuilder.bind(sagaListenerQueue())
                .to(bookingExchange())
                .with("booking.placed");
    }

    @Bean
    public Binding bindCompleted() {
        return BindingBuilder.bind(sagaListenerQueue())
                .to(bookingExchange())
                .with("booking.completed");
    }

    @Bean
    public Binding bindCancelled() {
        return BindingBuilder.bind(sagaListenerQueue())
                .to(bookingExchange())
                .with("booking.cancelled");
    }
}