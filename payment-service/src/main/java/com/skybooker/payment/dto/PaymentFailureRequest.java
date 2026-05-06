package com.skybooker.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentFailureRequest(
        @NotBlank(message = "Failure reason is required")
        String failureReason
) {
}