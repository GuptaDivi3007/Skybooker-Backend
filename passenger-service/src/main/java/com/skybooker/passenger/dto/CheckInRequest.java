package com.skybooker.passenger.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckInRequest(

        @NotBlank(message = "Seat id is required")
        String seatId,

        @NotBlank(message = "Seat number is required")
        String seatNumber
) {}