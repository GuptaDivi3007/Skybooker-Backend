package com.skybooker.auth.service;

import com.skybooker.auth.entity.AuthProvider;
import com.skybooker.auth.entity.Role;
import com.skybooker.auth.entity.User;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Properties;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WelcomeEmailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    private WelcomeEmailService welcomeEmailService;

    @BeforeEach
    void setUp() {
        welcomeEmailService = new WelcomeEmailService(mailSenderProvider);
        ReflectionTestUtils.setField(welcomeEmailService, "fromEmail", "no-reply@skybooker.local");
    }

    @Test
    void registrationSuccessEmailIsSkippedWhenEmailDisabled() {
        ReflectionTestUtils.setField(welcomeEmailService, "emailEnabled", false);

        welcomeEmailService.sendRegistrationSuccessEmail(user());

        verify(mailSenderProvider, never()).getIfAvailable();
    }

    @Test
    void registrationSuccessEmailIsSentWhenMailSenderExists() {
        ReflectionTestUtils.setField(welcomeEmailService, "emailEnabled", true);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(message);

        welcomeEmailService.sendRegistrationSuccessEmail(user());

        verify(mailSender).send(message);
    }

    @Test
    void registrationOtpEmailIsSentWhenEnabled() {
        ReflectionTestUtils.setField(welcomeEmailService, "emailEnabled", true);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(message);

        welcomeEmailService.sendRegistrationOtpEmail("divya@test.com", "Divya", "123456");

        verify(mailSender).send(message);
    }

    @Test
    void registrationOtpEmailIsSkippedForBlankEmail() {
        ReflectionTestUtils.setField(welcomeEmailService, "emailEnabled", true);

        welcomeEmailService.sendRegistrationOtpEmail(" ", "Divya", "123456");

        verify(mailSenderProvider, never()).getIfAvailable();
    }

    private User user() {
        User user = new User();
        user.setUserId("user-1");
        user.setFullName("Divya");
        user.setEmail("divya@test.com");
        user.setRole(Role.PASSENGER);
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
