package com.skybooker.notification.service;

import com.skybooker.notification.dto.*;
import com.skybooker.notification.entity.NotificationStatus;
import com.skybooker.notification.entity.NotificationType;

import java.util.List;

public interface NotificationService {

    NotificationResponse sendNotification(NotificationRequest request);

    NotificationResponse consumeNotificationEvent(NotificationEvent event);

    NotificationResponse getNotificationById(String notificationId);

    List<NotificationResponse> getUserNotifications(String userId);

    List<NotificationResponse> getAdminAppNotifications();

    List<NotificationResponse> getUnreadNotifications(String userId);

    Long countUnreadNotifications(String userId);

    List<NotificationResponse> getNotificationsByBooking(String bookingId);

    List<NotificationResponse> getNotificationsByPayment(String paymentId);

    List<NotificationResponse> getNotificationsByStatus(NotificationStatus status);

    List<NotificationResponse> getNotificationsByType(NotificationType type);

    NotificationResponse markAsRead(String notificationId);

    MessageResponse deleteNotification(String notificationId);
}
