package com.skybooker.notification.controller;

import com.skybooker.notification.dto.*;
import com.skybooker.notification.entity.NotificationStatus;
import com.skybooker.notification.entity.NotificationType;
import com.skybooker.notification.exception.AccessDeniedException;
import com.skybooker.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public NotificationResponse sendNotification(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @Valid @RequestBody NotificationRequest request) {
        requireAdminOrInternal(role);
        return notificationService.sendNotification(request);
    }

    @GetMapping("/{notificationId}")
    public NotificationResponse getNotificationById(@PathVariable String notificationId) {
        return notificationService.getNotificationById(notificationId);
    }

    @GetMapping("/me")
    public List<NotificationResponse> getMyNotifications(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) String userId) {
        requireAuthenticated(userId);
        return notificationService.getUserNotifications(userId);
    }

    @GetMapping("/admin/app")
    public List<NotificationResponse> getAdminAppNotifications(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role) {
        requireAdminOrInternal(role);
        return notificationService.getAdminAppNotifications();
    }

    @GetMapping("/me/unread")
    public List<NotificationResponse> getMyUnreadNotifications(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) String userId) {
        requireAuthenticated(userId);
        return notificationService.getUnreadNotifications(userId);
    }

    @GetMapping("/me/unread/count")
    public Long countMyUnreadNotifications(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) String userId) {
        requireAuthenticated(userId);
        return notificationService.countUnreadNotifications(userId);
    }

    @GetMapping("/user/{userId}")
    public List<NotificationResponse> getUserNotifications(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String userId) {
        requireAdminOrInternal(role);
        return notificationService.getUserNotifications(userId);
    }

    @GetMapping("/booking/{bookingId}")
    public List<NotificationResponse> getNotificationsByBooking(@PathVariable String bookingId) {
        return notificationService.getNotificationsByBooking(bookingId);
    }

    @GetMapping("/payment/{paymentId}")
    public List<NotificationResponse> getNotificationsByPayment(@PathVariable String paymentId) {
        return notificationService.getNotificationsByPayment(paymentId);
    }

    @GetMapping("/status/{status}")
    public List<NotificationResponse> getNotificationsByStatus(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable NotificationStatus status) {
        requireAdminOrInternal(role);
        return notificationService.getNotificationsByStatus(status);
    }

    @GetMapping("/type/{type}")
    public List<NotificationResponse> getNotificationsByType(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable NotificationType type) {
        requireAdminOrInternal(role);
        return notificationService.getNotificationsByType(type);
    }

    @PutMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(@PathVariable String notificationId) {
        return notificationService.markAsRead(notificationId);
    }

    @DeleteMapping("/{notificationId}")
    public MessageResponse deleteNotification(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String notificationId) {
        requireAdminOrInternal(role);
        return notificationService.deleteNotification(notificationId);
    }

    private void requireAuthenticated(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("Authenticated user is required");
        }
    }

    private void requireAdminOrInternal(String role) {
        if (role == null ||
                (!role.equalsIgnoreCase("ADMIN") && !role.equalsIgnoreCase("INTERNAL"))) {
            throw new AccessDeniedException("Only ADMIN or INTERNAL can perform this action");
        }
    }
}
