package com.skybooker.booking.producer;

import com.skybooker.booking.config.RabbitMQConfig;
import com.skybooker.booking.dto.NotificationEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BookingNotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${notification.booking-confirmed-routing-key}")
    private String bookingConfirmedRoutingKey;

    @Value("${notification.booking-cancelled-routing-key}")
    private String bookingCancelledRoutingKey;

    @Value("${notification.booking-status-routing-key}")
    private String bookingStatusRoutingKey;

    public BookingNotificationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendBookingConfirmed(NotificationEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EXCHANGE, bookingConfirmedRoutingKey, event);
    }

    public void sendBookingCancelled(NotificationEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EXCHANGE, bookingCancelledRoutingKey, event);
    }

    public void sendBookingStatusChanged(NotificationEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EXCHANGE, bookingStatusRoutingKey, event);
    }
}
