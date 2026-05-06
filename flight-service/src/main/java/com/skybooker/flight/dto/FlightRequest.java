package com.skybooker.flight.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record FlightRequest(
        @NotBlank(message = "Flight number is required")
        String flightNumber,

        @NotBlank(message = "Airline id is required")
        String airlineId,

        @NotBlank(message = "Origin airport code is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Origin airport code must be exactly 3 letters")
        String originAirportCode,

        @NotBlank(message = "Destination airport code is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Destination airport code must be exactly 3 letters")
        String destinationAirportCode,

        @NotNull(message = "Departure time is required")
        LocalDateTime departureTime,

        @NotNull(message = "Arrival time is required")
        LocalDateTime arrivalTime,

        @NotBlank(message = "Aircraft type is required")
        String aircraftType,

        @NotNull(message = "Total seats is required")
        @Min(value = 1, message = "Total seats must be at least 1")
        Integer totalSeats,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "1.0", message = "Base price must be greater than 0")
        Double basePrice
) {
}