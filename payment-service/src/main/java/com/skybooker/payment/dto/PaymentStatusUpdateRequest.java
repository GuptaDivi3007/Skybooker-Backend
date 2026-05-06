package com.skybooker.payment.dto;

import com.skybooker.payment.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record PaymentStatusUpdateRequest(

        @NotNull(message = "Payment status is required")
        PaymentStatus status
) {}