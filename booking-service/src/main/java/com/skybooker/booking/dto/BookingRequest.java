package com.skybooker.booking.dto;

import com.skybooker.booking.entity.MealPreference;
import com.skybooker.booking.entity.TripType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record BookingRequest(
        @NotBlank(message = "Flight id is required")
        String flightId,

        @NotEmpty(message = "At least one seat id is required")
        List<String> seatIds,

        @NotNull(message = "Trip type is required")
        TripType tripType,

        @NotNull(message = "Passenger count is required")
        @Min(value = 1, message = "Passenger count must be at least 1")
        Integer passengerCount,

        @NotEmpty(message = "Passenger details are required")
        @Valid
        List<PassengerRequest> passengers,

        MealPreference mealPreference,

        @Min(value = 0, message = "Luggage kg cannot be negative")
        Integer luggageKg,

        @Email(message = "Invalid contact email")
        String contactEmail,

        String contactPhone
) {
}