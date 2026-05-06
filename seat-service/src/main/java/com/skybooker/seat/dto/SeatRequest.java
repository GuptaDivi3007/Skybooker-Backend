package com.skybooker.seat.dto;

import com.skybooker.seat.entity.SeatClass;
import com.skybooker.seat.entity.SeatStatus;
import jakarta.validation.constraints.*;

public record SeatRequest(
        @NotBlank(message = "Seat number is required")
        String seatNumber,

        @NotNull(message = "Seat class is required")
        SeatClass seatClass,

        @NotNull(message = "Row number is required")
        @Min(value = 1, message = "Row number must be at least 1")
        Integer rowNumber,

        @NotBlank(message = "Column letter is required")
        String columnLetter,

        boolean windowSeat,

        boolean aisleSeat,

        boolean extraLegroom,

        @NotNull(message = "Price multiplier is required")
        @DecimalMin(value = "0.1", message = "Price multiplier must be at least 0.1")
        Double priceMultiplier,

        SeatStatus status
) {
}