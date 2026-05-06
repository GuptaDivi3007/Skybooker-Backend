package com.skybooker.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record ProcessPaymentRequest(

        @NotBlank(message = "Payment method is required")
        String paymentMethod

        // Example: CARD, UPI, NET_BANKING
) {}