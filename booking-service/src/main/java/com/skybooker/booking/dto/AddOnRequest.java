package com.skybooker.booking.dto;

import com.skybooker.booking.entity.MealPreference;
import jakarta.validation.constraints.Min;

public record AddOnRequest(
        MealPreference mealPreference,

        @Min(value = 0, message = "Luggage kg cannot be negative")
        Integer luggageKg
) {
}