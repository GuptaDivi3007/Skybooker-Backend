package com.skybooker.payment.dto;

public record PaymentSummaryResponse(

        String paymentId,
        String bookingId,
        double amount,
        String status
) {}