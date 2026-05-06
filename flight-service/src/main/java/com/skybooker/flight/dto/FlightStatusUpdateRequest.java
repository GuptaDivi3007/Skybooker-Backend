package com.skybooker.flight.dto;

import com.skybooker.flight.entity.FlightStatus;
import jakarta.validation.constraints.NotNull;

public record FlightStatusUpdateRequest(
        @NotNull(message = "Flight status is required")
        FlightStatus status
) {
}