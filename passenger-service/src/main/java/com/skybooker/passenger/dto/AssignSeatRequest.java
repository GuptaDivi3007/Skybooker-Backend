package com.skybooker.passenger.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignSeatRequest(

        @NotBlank(message = "Seat id is required")
        String seatId,

        @NotBlank(message = "Seat number is required")
        String seatNumber
) {}