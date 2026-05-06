package com.skybooker.booking.dto;

import com.skybooker.booking.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "Booking status is required")
        BookingStatus status
) {
}