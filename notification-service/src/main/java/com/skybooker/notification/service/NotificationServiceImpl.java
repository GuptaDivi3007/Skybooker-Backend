package com.skybooker.notification.service;

import com.skybooker.notification.dto.*;
import com.skybooker.notification.entity.*;
import com.skybooker.notification.exception.ResourceNotFoundException;
import com.skybooker.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public NotificationResponse sendNotification(NotificationRequest request) {
        Notification notification = createNotification(request);
        Notification saved = notificationRepository.save(notification);
        simulateSend(saved);
        return map(notificationRepository.save(saved));
    }

    @Override
    public NotificationResponse consumeNotificationEvent(NotificationEvent event) {
        NotificationType type = parseType(event.notificationType());

        List<NotificationResponse> responses = new ArrayList<>();

        responses.add(sendNotification(new NotificationRequest(
                event.userId(), event.recipientEmail(), event.recipientPhone(),
                event.title(), event.message(), type, NotificationChannel.APP,
                event.bookingId(), event.paymentId()
        )));

        if (event.recipientEmail() != null && !event.recipientEmail().isBlank()) {
            responses.add(sendNotification(new NotificationRequest(
                    event.userId(), event.recipientEmail(), event.recipientPhone(),
                    event.title(), event.message(), type, NotificationChannel.EMAIL,
                    event.bookingId(), event.paymentId()
            )));
        }

        if (event.recipientPhone() != null && !event.recipientPhone().isBlank()) {
            responses.add(sendNotification(new NotificationRequest(
                    event.userId(), event.recipientEmail(), event.recipientPhone(),
                    event.title(), event.message(), type, NotificationChannel.SMS,
                    event.bookingId(), event.paymentId()
            )));
        }

        return responses.get(0);
    }

    @Override
    public NotificationResponse getNotificationById(String notificationId) {
        return map(findNotification(notificationId));
    }

    @Override
    public List<NotificationResponse> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::map).toList();
    }

    @Override
    public List<NotificationResponse> getAdminAppNotifications() {
        return notificationRepository.findByUserIdAndChannelOrderByCreatedAtDesc("ADMIN", NotificationChannel.APP)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndReadStatusFalseOrderByCreatedAtDesc(userId).stream().map(this::map).toList();
    }

    @Override
    public Long countUnreadNotifications(String userId) {
        return notificationRepository.countByUserIdAndReadStatusFalse(userId);
    }

    @Override
    public List<NotificationResponse> getNotificationsByBooking(String bookingId) {
        return notificationRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream().map(this::map).toList();
    }

    @Override
    public List<NotificationResponse> getNotificationsByPayment(String paymentId) {
        return notificationRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId).stream().map(this::map).toList();
    }

    @Override
    public List<NotificationResponse> getNotificationsByStatus(NotificationStatus status) {
        return notificationRepository.findByStatus(status).stream().map(this::map).toList();
    }

    @Override
    public List<NotificationResponse> getNotificationsByType(NotificationType type) {
        return notificationRepository.findByType(type).stream().map(this::map).toList();
    }

    @Override
    public NotificationResponse markAsRead(String notificationId) {
        Notification notification = findNotification(notificationId);
        notification.setReadStatus(true);
        return map(notificationRepository.save(notification));
    }

    @Override
    public MessageResponse deleteNotification(String notificationId) {
        Notification notification = findNotification(notificationId);
        notificationRepository.delete(notification);
        return new MessageResponse("Notification deleted successfully");
    }

    private Notification createNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(required(request.userId(), "User id"));
        notification.setRecipientEmail(blankToNull(request.recipientEmail()));
        notification.setRecipientPhone(blankToNull(request.recipientPhone()));
        notification.setTitle(required(request.title(), "Title"));
        notification.setMessage(required(request.message(), "Message"));
        notification.setType(request.type());
        notification.setChannel(request.channel());
        notification.setBookingId(blankToNull(request.bookingId()));
        notification.setPaymentId(blankToNull(request.paymentId()));
        notification.setStatus(NotificationStatus.PENDING);
        return notification;
    }

    private void simulateSend(Notification notification) {
        try {
            System.out.println("========================================");
            System.out.println("Notification Channel: " + notification.getChannel());
            System.out.println("Notification Type: " + notification.getType());
            System.out.println("To User: " + notification.getUserId());
            System.out.println("Email: " + notification.getRecipientEmail());
            System.out.println("Phone: " + notification.getRecipientPhone());
            System.out.println("Title: " + notification.getTitle());
            System.out.println("Message: " + notification.getMessage());
            System.out.println("Booking Id: " + notification.getBookingId());
            System.out.println("========================================");

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setFailureReason(null);
        } catch (Exception ex) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(ex.getMessage());
        }
    }

    private NotificationType parseType(String type) {
        if (type == null || type.isBlank()) {
            return NotificationType.SYSTEM_ALERT;
        }
        return NotificationType.valueOf(type.trim().toUpperCase());
    }

    private Notification findNotification(String notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private NotificationResponse map(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getUserId(),
                notification.getRecipientEmail(),
                notification.getRecipientPhone(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getReadStatus(),
                notification.getBookingId(),
                notification.getPaymentId(),
                notification.getFailureReason(),
                notification.getCreatedAt(),
                notification.getSentAt()
        );
    }
}
