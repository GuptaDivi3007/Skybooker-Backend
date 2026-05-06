package com.skybooker.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(

        @NotBlank(message = "Booking id is required")
        String bookingId,

        @Positive(message = "Amount must be greater than 0")
        double amount
) {}