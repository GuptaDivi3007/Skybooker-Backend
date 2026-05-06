package com.skybooker.booking.dto;

import com.skybooker.booking.entity.MealPreference;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FareCalculationRequest(
        @NotBlank(message = "Flight id is required")
        String flightId,

        @NotNull(message = "Passenger count is required")
        @Min(value = 1, message = "Passenger count must be at least 1")
        Integer passengerCount,

        MealPreference mealPreference,

        @Min(value = 0, message = "Luggage kg cannot be negative")
        Integer luggageKg
) {
}