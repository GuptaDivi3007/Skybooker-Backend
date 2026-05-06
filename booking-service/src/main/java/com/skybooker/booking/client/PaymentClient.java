package com.skybooker.booking.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentClient {

    private final RestTemplate restTemplate;

    @Value("${services.payment-service.base-url}")
    private String paymentBaseUrl;

    public PaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void createPayment(String userId, String bookingId, double amount) {

        String url = paymentBaseUrl + "/payments/razorpay/order";

        Map<String, Object> body = new HashMap<>();
        body.put("bookingId", bookingId);
        body.put("userId", userId);
        body.put("amount", amount);
        body.put("currency", "INR");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // REQUIRED
        headers.set("X-Authenticated-User-Role", "INTERNAL");
        headers.set("X-Authenticated-User-Id", userId);
        headers.set("X-Authenticated-User-Email", "booking-service@internal");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(url, entity, Object.class);
    }
}