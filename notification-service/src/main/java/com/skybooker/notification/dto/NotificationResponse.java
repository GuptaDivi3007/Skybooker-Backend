package com.skybooker.notification.dto;

import com.skybooker.notification.entity.NotificationChannel;
import com.skybooker.notification.entity.NotificationStatus;
import com.skybooker.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        String notificationId,
        String userId,
        String recipientEmail,
        String recipientPhone,
        String title,
        String message,
        NotificationType type,
        NotificationChannel channel,
        NotificationStatus status,
        Boolean readStatus,
        String bookingId,
        String paymentId,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime sentAt
) {
}
