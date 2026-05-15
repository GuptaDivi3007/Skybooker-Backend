package com.skybooker.notification.service;

import com.skybooker.notification.dto.NotificationEvent;
import com.skybooker.notification.dto.NotificationRequest;
import com.skybooker.notification.dto.NotificationResponse;
import com.skybooker.notification.entity.*;
import com.skybooker.notification.exception.ResourceNotFoundException;
import com.skybooker.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationRepository);
    }

    @Test
    void sendNotificationMarksNotificationAsSent() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = service.sendNotification(new NotificationRequest(
                "user-1", "user@test.com", null, "Ticket", "Booked",
                NotificationType.BOOKING_CONFIRMED, NotificationChannel.EMAIL, "booking-1", "payment-1"
        ));

        assertEquals(NotificationStatus.SENT, response.status());
        assertEquals(NotificationChannel.EMAIL, response.channel());
        assertNotNull(response.sentAt());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void consumeNotificationEventCreatesAppAndEmailNotifications() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = service.consumeNotificationEvent(event("BOOKING_CONFIRMED", "user@test.com", null));

        assertEquals(NotificationChannel.APP, response.channel());
        verify(notificationRepository, times(4)).save(any(Notification.class));
    }

    @Test
    void markAsReadUpdatesReadStatus() {
        Notification notification = notification();
        when(notificationRepository.findById("notification-1")).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = service.markAsRead("notification-1");

        assertTrue(response.readStatus());
    }

    @Test
    void getUnreadNotificationsMapsRepositoryResults() {
        when(notificationRepository.findByUserIdAndReadStatusFalseOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(notification()));

        List<NotificationResponse> response = service.getUnreadNotifications("user-1");

        assertEquals(1, response.size());
        assertFalse(response.get(0).readStatus());
    }

    @Test
    void getNotificationByIdThrowsWhenMissing() {
        when(notificationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getNotificationById("missing"));
    }

    private NotificationEvent event(String type, String email, String phone) {
        return new NotificationEvent(
                "BOOKING_EVENT", type, "user-1", email, phone, "Ticket", "Booked",
                "booking-1", "PNR123", "payment-1", "SB101", "airline-1",
                "DEL", "BOM", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2),
                120, "A320", "1A", "Aarav Mehta", 1,
                1000.0, 120.0, 0.0, 0.0, 1120.0, "CONFIRMED", LocalDateTime.now()
        );
    }

    private Notification notification() {
        Notification notification = new Notification();
        notification.setUserId("user-1");
        notification.setRecipientEmail("user@test.com");
        notification.setTitle("Ticket");
        notification.setMessage("Booked");
        notification.setType(NotificationType.BOOKING_CONFIRMED);
        notification.setChannel(NotificationChannel.APP);
        notification.setStatus(NotificationStatus.SENT);
        notification.setReadStatus(false);
        notification.setBookingId("booking-1");
        notification.setSentAt(LocalDateTime.now());
        return notification;
    }
}
