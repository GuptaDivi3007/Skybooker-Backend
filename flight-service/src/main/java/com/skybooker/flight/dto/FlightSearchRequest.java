package com.skybooker.flight.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record FlightSearchRequest(
        @NotBlank(message = "Origin airport code is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Origin airport code must be exactly 3 letters")
        String originAirportCode,

        @NotBlank(message = "Destination airport code is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Destination airport code must be exactly 3 letters")
        String destinationAirportCode,

        @NotNull(message = "Departure date is required")
        LocalDate departureDate,

        @NotNull(message = "Passenger count is required")
        @Min(value = 1, message = "Passenger count must be at least 1")
        Integer passengers
) {
}