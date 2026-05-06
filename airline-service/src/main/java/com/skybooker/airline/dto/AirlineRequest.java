package com.skybooker.airline.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AirlineRequest(
        @NotBlank(message = "Airline name is required")
        @Size(max = 120, message = "Airline name must be at most 120 characters")
        String name,

        @NotBlank(message = "IATA code is required")
        @Pattern(regexp = "^[A-Za-z0-9]{2,3}$", message = "Airline IATA code must be 2 or 3 letters/digits")
        String iataCode,

        @Pattern(regexp = "^$|^[A-Za-z0-9]{3,4}$", message = "Airline ICAO code must be 3 or 4 letters/digits")
        String icaoCode,

        String logoUrl,

        @Size(max = 80, message = "Country must be at most 80 characters")
        String country,

        @Email(message = "Invalid contact email")
        String contactEmail,

        @Size(max = 30, message = "Contact phone must be at most 30 characters")
        String contactPhone
) {
}