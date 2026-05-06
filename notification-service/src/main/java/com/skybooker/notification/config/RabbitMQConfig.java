package com.skybooker.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_EXCHANGE = "skybooker.notification.exchange";
    public static final String BOOKING_CONFIRMED_QUEUE = "notification.booking.confirmed.queue";
    public static final String BOOKING_CANCELLED_QUEUE = "notification.booking.cancelled.queue";
    public static final String BOOKING_STATUS_QUEUE = "notification.booking.status.queue";
    public static final String BOOKING_CONFIRMED_ROUTING_KEY = "booking.confirmed";
    public static final String BOOKING_CANCELLED_ROUTING_KEY = "booking.cancelled";
    public static final String BOOKING_STATUS_ROUTING_KEY = "booking.status";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue bookingConfirmedQueue() {
        return QueueBuilder.durable(BOOKING_CONFIRMED_QUEUE).build();
    }

    @Bean
    public Queue bookingCancelledQueue() {
        return QueueBuilder.durable(BOOKING_CANCELLED_QUEUE).build();
    }

    @Bean
    public Queue bookingStatusQueue() {
        return QueueBuilder.durable(BOOKING_STATUS_QUEUE).build();
    }

    @Bean
    public Binding bookingConfirmedBinding() {
        return BindingBuilder.bind(bookingConfirmedQueue()).to(notificationExchange()).with(BOOKING_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding bookingCancelledBinding() {
        return BindingBuilder.bind(bookingCancelledQueue()).to(notificationExchange()).with(BOOKING_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding bookingStatusBinding() {
        return BindingBuilder.bind(bookingStatusQueue()).to(notificationExchange()).with(BOOKING_STATUS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
