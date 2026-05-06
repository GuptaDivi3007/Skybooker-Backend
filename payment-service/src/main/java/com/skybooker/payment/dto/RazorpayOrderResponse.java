package com.skybooker.payment.dto;

import com.skybooker.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record RazorpayOrderResponse(
        String paymentId,
        String bookingId,
        String userId,
        double amount,
        int amountInPaise,
        String currency,
        PaymentStatus status,
        String razorpayKeyId,
        String razorpayOrderId,
        String receipt,
        LocalDateTime createdAt
) {
}
