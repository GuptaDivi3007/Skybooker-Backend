package com.skybooker.seat.dto;

import com.skybooker.seat.entity.SeatClass;
import com.skybooker.seat.entity.SeatStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record UpdateSeatRequest(
        String seatNumber,

        SeatClass seatClass,

        @Min(value = 1, message = "Row number must be at least 1")
        Integer rowNumber,

        String columnLetter,

        Boolean windowSeat,

        Boolean aisleSeat,

        Boolean extraLegroom,

        @DecimalMin(value = "0.1", message = "Price multiplier must be at least 0.1")
        Double priceMultiplier,

        SeatStatus status
) {
}