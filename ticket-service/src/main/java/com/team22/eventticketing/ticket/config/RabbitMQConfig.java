package com.team22.eventticketing.ticket.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TICKET_EVENTS_EXCHANGE  = "ticket.events";
    public static final String EVENT_EVENTS_EXCHANGE   = "event.events";
    public static final String BOOKING_EVENTS_EXCHANGE = "booking.events";

    public static final String EVENT_LISTENER_QUEUE   = "ticket.event-listener";
    public static final String EVENT_LISTENER_DLQ     = "ticket.event-listener.dlq";
    public static final String BOOKING_LISTENER_QUEUE = "ticket.booking.saga-listener";
    public static final String BOOKING_LISTENER_DLQ   = "ticket.booking.saga-listener.dlq";

    @Bean
    public TopicExchange ticketEventsExchange() {
        return new TopicExchange(TICKET_EVENTS_EXCHANGE);
    }

    @Bean
    public TopicExchange eventEventsExchange() {
        return new TopicExchange(EVENT_EVENTS_EXCHANGE);
    }

    @Bean
    public TopicExchange bookingEventsExchange() {
        return new TopicExchange(BOOKING_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue eventListenerQueue() {
        return QueueBuilder.durable(EVENT_LISTENER_QUEUE)
                .withArgument("x-dead-letter-exchange", EVENT_LISTENER_DLQ)
                .withArgument("x-dead-letter-routing-key", EVENT_LISTENER_DLQ)
                .build();
    }

    @Bean
    public Queue eventListenerDLQ() {
        return QueueBuilder.durable(EVENT_LISTENER_DLQ).build();
    }

    @Bean
    public Binding eventListenerBinding(Queue eventListenerQueue,
                                        TopicExchange eventEventsExchange) {
        return BindingBuilder.bind(eventListenerQueue)
                .to(eventEventsExchange)
                .with("event.#");
    }

    @Bean
    public Queue bookingListenerQueue() {
        return QueueBuilder.durable(BOOKING_LISTENER_QUEUE)
                .withArgument("x-dead-letter-exchange", BOOKING_LISTENER_DLQ)
                .withArgument("x-dead-letter-routing-key", BOOKING_LISTENER_DLQ)
                .build();
    }

    @Bean
    public Queue bookingListenerDLQ() {
        return QueueBuilder.durable(BOOKING_LISTENER_DLQ).build();
    }

    @Bean
    public Binding bookingListenerBinding(Queue bookingListenerQueue,
                                          TopicExchange bookingEventsExchange) {
        return BindingBuilder.bind(bookingListenerQueue)
                .to(bookingEventsExchange)
                .with("booking.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate t = new RabbitTemplate(factory);
        t.setMessageConverter(jsonMessageConverter);
        return t;
    }
}
