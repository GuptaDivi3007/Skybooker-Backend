package com.skybooker.notification.dto;

import com.skybooker.notification.entity.NotificationChannel;
import com.skybooker.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationRequest(
        @NotBlank(message = "User id is required")
        String userId,
        String recipientEmail,
        String recipientPhone,
        @NotBlank(message = "Title is required")
        String title,
        @NotBlank(message = "Message is required")
        String message,
        @NotNull(message = "Notification type is required")
        NotificationType type,
        @NotNull(message = "Notification channel is required")
        NotificationChannel channel,
        String bookingId,
        String paymentId
) {
}
