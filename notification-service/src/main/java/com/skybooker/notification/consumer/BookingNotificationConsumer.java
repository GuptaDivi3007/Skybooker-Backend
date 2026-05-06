package com.skybooker.notification.consumer;

import org.springframework.stereotype.Component;

/**
 * RabbitMQ handling is centralized in BookingNotificationListener so booking
 * events are not split between two competing consumers.
 */
@Component
public class BookingNotificationConsumer {
}
