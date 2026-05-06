package com.skybooker.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record BookingPaymentLinkRequest(
        @NotBlank(message = "Booking id is required")
        String bookingId,

        @NotBlank(message = "Payment id is required")
        String paymentId
) {
}