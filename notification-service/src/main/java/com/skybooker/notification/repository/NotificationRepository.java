package com.skybooker.notification.repository;

import com.skybooker.notification.entity.Notification;
import com.skybooker.notification.entity.NotificationChannel;
import com.skybooker.notification.entity.NotificationStatus;
import com.skybooker.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Notification> findByUserIdAndChannelOrderByCreatedAtDesc(String userId, NotificationChannel channel);

    List<Notification> findByBookingIdOrderByCreatedAtDesc(String bookingId);

    List<Notification> findByPaymentIdOrderByCreatedAtDesc(String paymentId);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByType(NotificationType type);

    List<Notification> findByUserIdAndReadStatusFalseOrderByCreatedAtDesc(String userId);

    long countByUserIdAndReadStatusFalse(String userId);
}
