package com.skybooker.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record RazorpayPaymentVerifyRequest(
        @NotBlank(message = "Local payment id is required")
        String paymentId,

        @NotBlank(message = "Razorpay order id is required")
        String razorpayOrderId,

        @NotBlank(message = "Razorpay payment id is required")
        String razorpayPaymentId,

        @NotBlank(message = "Razorpay signature is required")
        String razorpaySignature,

        String paymentMethod
) {
}
