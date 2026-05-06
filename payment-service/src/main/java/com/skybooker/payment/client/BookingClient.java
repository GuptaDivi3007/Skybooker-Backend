package com.skybooker.payment.client;

import com.skybooker.payment.dto.BookingPaymentLinkRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BookingClient {

    private final RestTemplate restTemplate;

    @Value("${services.booking-service.base-url}")
    private String bookingServiceBaseUrl;

    public BookingClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void confirmBooking(String bookingId) {
        HttpEntity<Void> entity = new HttpEntity<>(internalHeaders());

        restTemplate.exchange(
                bookingServiceBaseUrl + "/bookings/" + bookingId + "/confirm",
                HttpMethod.PUT,
                entity,
                Object.class
        );
    }

    public void linkPayment(String bookingId, String paymentId) {
        HttpHeaders headers = internalHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        BookingPaymentLinkRequest body = new BookingPaymentLinkRequest(bookingId, paymentId);
        HttpEntity<BookingPaymentLinkRequest> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange(
                bookingServiceBaseUrl + "/bookings/" + bookingId + "/payment",
                HttpMethod.PUT,
                entity,
                Object.class
        );
    }

    public void cancelBooking(String bookingId) {
        HttpEntity<Void> entity = new HttpEntity<>(internalHeaders());

        restTemplate.exchange(
                bookingServiceBaseUrl + "/bookings/" + bookingId + "/cancel",
                HttpMethod.PUT,
                entity,
                Object.class
        );
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Authenticated-User-Role", "INTERNAL");
        headers.set("X-Authenticated-User-Id", "payment-service");
        headers.set("X-Authenticated-User-Email", "payment-service@internal");
        return headers;
    }
}