package com.skybooker.booking.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentLinkRequest(
        @NotBlank(message = "Booking id is required")
        String bookingId,

        @NotBlank(message = "Payment id is required")
        String paymentId
) {
}