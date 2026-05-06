package com.skybooker.notification.listener;

import com.skybooker.notification.config.RabbitMQConfig;
import com.skybooker.notification.dto.NotificationEvent;
import com.skybooker.notification.entity.Notification;
import com.skybooker.notification.entity.NotificationChannel;
import com.skybooker.notification.entity.NotificationStatus;
import com.skybooker.notification.entity.NotificationType;
import com.skybooker.notification.repository.NotificationRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookingNotificationListener {

    private static final String ADMIN_NOTIFICATION_USER_ID = "ADMIN";

    private final NotificationRepository notificationRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.from:no-reply@skybooker.local}")
    private String fromEmail;

    public BookingNotificationListener(NotificationRepository notificationRepository,
                                       ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.notificationRepository = notificationRepository;
        this.mailSenderProvider = mailSenderProvider;
    }

    @RabbitListener(queues = RabbitMQConfig.BOOKING_CONFIRMED_QUEUE)
    public void consumeBookingConfirmed(NotificationEvent event) {
        processNotification(event, "BOOKING_CONFIRMED");
    }

    @RabbitListener(queues = RabbitMQConfig.BOOKING_CANCELLED_QUEUE)
    public void consumeBookingCancelled(NotificationEvent event) {
        processNotification(event, "CANCELLATION");
    }

    @RabbitListener(queues = RabbitMQConfig.BOOKING_STATUS_QUEUE)
    public void consumeBookingStatus(NotificationEvent event) {
        processNotification(event, event.notificationType());
    }

    private void processNotification(NotificationEvent event, String fallbackType) {

        System.out.println("RabbitMQ message received in notification-service");
        System.out.println("Event Type: " + event.eventType());
        System.out.println("Notification Type: " + event.notificationType());
        System.out.println("Booking Id: " + event.bookingId());
        System.out.println("PNR Code: " + event.pnrCode());

        saveAdminAppNotification(event, fallbackType);

        if (event.recipientEmail() != null && !event.recipientEmail().isBlank()) {
            boolean sent = sendEmail(event);
            saveNotification(event, NotificationChannel.EMAIL, fallbackType,
                    sent ? NotificationStatus.SENT : NotificationStatus.FAILED,
                    sent ? null : "Email delivery failed. Check SMTP credentials or recipient mailbox.");
        }
    }

    private void saveNotification(NotificationEvent event,
                                  NotificationChannel channel,
                                  String fallbackType,
                                  NotificationStatus status,
                                  String failureReason) {

        Notification notification = new Notification();

        notification.setUserId(event.userId());
        notification.setRecipientEmail(event.recipientEmail());
        notification.setRecipientPhone(event.recipientPhone());
        notification.setBookingId(event.bookingId());
        notification.setPaymentId(event.paymentId());
        notification.setTitle(event.title());
        notification.setMessage(event.message());
        notification.setType(convertType(event.notificationType(), fallbackType));
        notification.setChannel(channel);
        notification.setStatus(status);
        notification.setReadStatus(false);
        notification.setFailureReason(failureReason);
        if (status == NotificationStatus.SENT) {
            notification.setSentAt(LocalDateTime.now());
        }

        notificationRepository.save(notification);
    }

    private void saveAdminAppNotification(NotificationEvent event, String fallbackType) {
        Notification notification = new Notification();

        notification.setUserId(ADMIN_NOTIFICATION_USER_ID);
        notification.setRecipientEmail(null);
        notification.setRecipientPhone(null);
        notification.setBookingId(event.bookingId());
        notification.setPaymentId(event.paymentId());
        notification.setTitle("Passenger " + event.title());
        notification.setMessage("Passenger " + nullSafe(event.recipientEmail()) + " - " + event.message());
        notification.setType(convertType(event.notificationType(), fallbackType));
        notification.setChannel(NotificationChannel.APP);
        notification.setStatus(NotificationStatus.SENT);
        notification.setReadStatus(false);
        notification.setSentAt(LocalDateTime.now());

        notificationRepository.save(notification);
    }

    private NotificationType convertType(String notificationType, String fallbackType) {
        String typeToUse = notificationType;

        if (typeToUse == null || typeToUse.isBlank()) {
            typeToUse = fallbackType;
        }

        try {
            return NotificationType.valueOf(typeToUse);
        } catch (Exception e) {
            return NotificationType.BOOKING_CONFIRMED;
        }
    }

    private boolean sendEmail(NotificationEvent event) {
        if (!emailEnabled) {
            System.out.println("EMAIL not sent because MAIL_ENABLED=false. Intended recipient: " + event.recipientEmail());
            return false;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            System.out.println("EMAIL not sent because JavaMailSender is not configured. Intended recipient: " + event.recipientEmail());
            return false;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(event.recipientEmail());
            helper.setSubject(event.title());
            helper.setText(buildEmailBody(event), true);
            if ("BOOKING_CONFIRMED".equalsIgnoreCase(event.notificationType())) {
                helper.addAttachment(ticketFileName(event, "E-Ticket"),
                        new ByteArrayResource(buildTicketPdf(event)),
                        "application/pdf");
                helper.addAttachment(ticketFileName(event, "Boarding-Pass"),
                        new ByteArrayResource(buildBoardingPassPdf(event)),
                        "application/pdf");
            }
            mailSender.send(mimeMessage);
            System.out.println("EMAIL sent to: " + event.recipientEmail());
            return true;
        } catch (Exception ex) {
            System.out.println("EMAIL failed for " + event.recipientEmail() + ": " + ex.getMessage());
            return false;
        }
    }

    private String buildEmailBody(NotificationEvent event) {
        boolean confirmed = "BOOKING_CONFIRMED".equalsIgnoreCase(event.notificationType());
        String statusLine = confirmed
                ? "Your payment is successful and your ticket is booked."
                : event.message();

        return """
                <div style="font-family:Arial,sans-serif;color:#07172d;line-height:1.6">
                  <h2 style="margin-bottom:8px">SkyBooker</h2>
                  <p>%s</p>
                  <div style="border:1px solid #d8e6f8;border-radius:12px;padding:16px;margin:18px 0">
                    <p><strong>PNR:</strong> %s</p>
                    <p><strong>Booking ID:</strong> %s</p>
                    <p><strong>Payment ID:</strong> %s</p>
                  </div>
                  <p>Your e-ticket and boarding pass are attached with this email. You can download both files directly from this message.</p>
                  <p>You can also download your e-ticket PDF and boarding pass from the SkyBooker payment confirmation page in the app.</p>
                  <p style="color:#5d6b7f">Please carry a valid government ID/passport for airport verification.</p>
                </div>
                """.formatted(
                escape(statusLine),
                escape(event.pnrCode()),
                escape(event.bookingId()),
                escape(event.paymentId())
        );
    }

    private byte[] buildTicketPdf(NotificationEvent event) throws IOException {
        return buildPdf(
                "SkyBooker E-Ticket",
                "Your payment is successful and your ticket is booked.",
                List.of(
                        new PdfRow("PNR", printable(event.pnrCode())),
                        new PdfRow("Booking ID", printable(event.bookingId())),
                        new PdfRow("Flight", printable(event.flightNumber())),
                        new PdfRow("Airline", printable(event.airlineId())),
                        new PdfRow("Route", route(event)),
                        new PdfRow("Departure", printableDateTime(event.departureTime())),
                        new PdfRow("Seats", printable(event.seatNumbers())),
                        new PdfRow("Passengers", printable(event.passengerNames())),
                        new PdfRow("Status", printable(event.bookingStatus())),
                        new PdfRow("Payment ID", printable(event.paymentId())),
                        new PdfRow("Base Fare", money(event.baseFare())),
                        new PdfRow("Taxes", money(event.taxes())),
                        new PdfRow("Meal", money(event.mealCost())),
                        new PdfRow("Baggage", money(event.baggageCost())),
                        new PdfRow("Total Paid", money(event.totalFare()))
                ),
                "Please carry a valid government ID/passport for airport verification."
        );
    }

    private byte[] buildBoardingPassPdf(NotificationEvent event) throws IOException {
        return buildPdf(
                "SkyBooker Boarding Pass",
                "Ready for airport check-in",
                List.of(
                        new PdfRow("PNR", printable(event.pnrCode())),
                        new PdfRow("Passenger(s)", printable(event.passengerNames())),
                        new PdfRow("Flight", printable(event.flightNumber())),
                        new PdfRow("Route", route(event)),
                        new PdfRow("Departure", printableDateTime(event.departureTime())),
                        new PdfRow("Seats", printable(event.seatNumbers())),
                        new PdfRow("Booking ID", printable(event.bookingId())),
                        new PdfRow("Payment ID", printable(event.paymentId())),
                        new PdfRow("Status", printable(event.bookingStatus()))
                ),
                "Arrive early and keep this boarding pass with your government ID/passport."
        );
    }

    private byte[] buildPdf(String title, String subtitle, List<PdfRow> rows, String footer) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawHeader(content, title, subtitle);
                drawRows(content, rows);
                drawFooter(content, footer);
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void drawHeader(PDPageContentStream content, String title, String subtitle) throws IOException {
        content.setNonStrokingColor(new Color(0, 87, 184));
        content.addRect(0, 760, 595, 82);
        content.fill();

        content.setNonStrokingColor(Color.WHITE);
        writeText(content, PDType1Font.HELVETICA_BOLD, 24, 52, 800, title);
        writeText(content, PDType1Font.HELVETICA, 12, 52, 780, subtitle);
    }

    private void drawRows(PDPageContentStream content, List<PdfRow> rows) throws IOException {
        float y = 700;
        for (PdfRow row : rows) {
            if (y < 96) {
                break;
            }
            content.setNonStrokingColor(new Color(247, 251, 255));
            content.addRect(52, y - 30, 490, 40);
            content.fill();
            content.setStrokingColor(new Color(216, 230, 248));
            content.addRect(52, y - 30, 490, 40);
            content.stroke();

            content.setNonStrokingColor(new Color(93, 107, 127));
            writeText(content, PDType1Font.HELVETICA_BOLD, 8, 70, y - 4, row.label().toUpperCase());
            content.setNonStrokingColor(new Color(7, 23, 45));
            writeText(content, PDType1Font.HELVETICA_BOLD, 11, 70, y - 20, truncate(row.value(), 86));
            y -= 44;
        }
    }

    private void drawFooter(PDPageContentStream content, String footer) throws IOException {
        content.setNonStrokingColor(new Color(93, 107, 127));
        writeText(content, PDType1Font.HELVETICA, 11, 52, 110, footer);
        writeText(content, PDType1Font.HELVETICA, 10, 52, 88, "Generated by SkyBooker Airline Reservations");
    }

    private void writeText(PDPageContentStream content,
                           PDType1Font font,
                           int fontSize,
                           float x,
                           float y,
                           String text) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(safePdfText(text));
        content.endText();
    }

    private String safePdfText(String value) {
        return printable(value)
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private String truncate(String value, int maxLength) {
        String printable = printable(value);
        if (printable.length() <= maxLength) {
            return printable;
        }
        return printable.substring(0, maxLength - 3) + "...";
    }

    private String route(NotificationEvent event) {
        return printable(event.originAirportCode()) + " to " + printable(event.destinationAirportCode());
    }

    private String printableDateTime(LocalDateTime value) {
        return value == null ? "-" : value.toString().replace("T", " ");
    }

    private String money(Double value) {
        return value == null ? "-" : "INR " + String.format("%.2f", value);
    }

    private String ticketFileName(NotificationEvent event, String documentType) {
        String pnr = event.pnrCode() == null || event.pnrCode().isBlank() ? "SkyBooker" : event.pnrCode().trim();
        return "SkyBooker-" + documentType + "-" + pnr + ".pdf";
    }

    private record PdfRow(String label, String value) {
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

    private String nullSafe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown passenger";
        }
        return value.trim();
    }

    private String printable(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }
}
