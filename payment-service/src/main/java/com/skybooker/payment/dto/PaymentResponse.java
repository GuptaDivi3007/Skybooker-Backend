package com.skybooker.payment.dto;

import com.skybooker.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        String paymentId,
        String bookingId,
        String userId,
        double amount,
        String currency,
        PaymentStatus status,
        String transactionId,
        String paymentMethod,
        String razorpayOrderId,
        String razorpayPaymentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
