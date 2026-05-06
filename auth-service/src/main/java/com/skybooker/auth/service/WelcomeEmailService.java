package com.skybooker.auth.service;

import com.skybooker.auth.entity.User;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class WelcomeEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.from:no-reply@skybooker.local}")
    private String fromEmail;

    public WelcomeEmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendRegistrationSuccessEmail(User user) {
        if (!emailEnabled || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            System.out.println("WELCOME EMAIL not sent because JavaMailSender is not configured. Intended recipient: " + user.getEmail());
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Welcome to SkyBooker - Registration Successful");
            helper.setText(buildWelcomeBody(user), true);
            mailSender.send(mimeMessage);
            System.out.println("WELCOME EMAIL sent to: " + user.getEmail());
        } catch (Exception ex) {
            System.out.println("WELCOME EMAIL failed for " + user.getEmail() + ": " + ex.getMessage());
        }
    }

    public void sendRegistrationOtpEmail(String email, String fullName, String otp) {
        if (!emailEnabled || email == null || email.isBlank()) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            System.out.println("REGISTRATION OTP EMAIL not sent because JavaMailSender is not configured. Intended recipient: " + email);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Verify your SkyBooker registration OTP");
            helper.setText(buildOtpBody(fullName, otp), true);
            mailSender.send(mimeMessage);
            System.out.println("REGISTRATION OTP EMAIL sent to: " + email);
        } catch (Exception ex) {
            System.out.println("REGISTRATION OTP EMAIL failed for " + email + ": " + ex.getMessage());
        }
    }

    private String buildWelcomeBody(User user) {
        return """
                <div style="font-family:Arial,sans-serif;color:#07172d;line-height:1.6">
                  <h2 style="margin-bottom:8px">Welcome to SkyBooker</h2>
                  <p>Hi %s,</p>
                  <p>Your SkyBooker account has been successfully registered.</p>
                  <div style="border:1px solid #d8e6f8;border-radius:12px;padding:16px;margin:18px 0">
                    <p><strong>Email:</strong> %s</p>
                    <p><strong>Account type:</strong> %s</p>
                  </div>
                  <p>You can now search flights, save passenger details, select seats, complete Razorpay payment, and receive booking updates on this email address.</p>
                  <p style="color:#5d6b7f">Thank you for choosing SkyBooker.</p>
                </div>
                """.formatted(
                escape(user.getFullName()),
                escape(user.getEmail()),
                user.getRole() == null ? "PASSENGER" : escape(user.getRole().name())
        );
    }

    private String buildOtpBody(String fullName, String otp) {
        return """
                <div style="font-family:Arial,sans-serif;color:#07172d;line-height:1.6">
                  <h2 style="margin-bottom:8px">Verify your SkyBooker account</h2>
                  <p>Hi %s,</p>
                  <p>Use this OTP to complete your SkyBooker registration:</p>
                  <div style="font-size:32px;font-weight:900;letter-spacing:6px;color:#0057b8;border:1px solid #d8e6f8;border-radius:12px;padding:16px;margin:18px 0;text-align:center">%s</div>
                  <p>This OTP is valid for 10 minutes. Your account will be created only after this OTP is verified.</p>
                  <p style="color:#5d6b7f">If you did not request this, please ignore this email.</p>
                </div>
                """.formatted(escape(fullName), escape(otp));
    }

    private String escape(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
