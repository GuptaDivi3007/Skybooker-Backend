package com.skybooker.airline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AirportRequest(
        @NotBlank(message = "Airport name is required")
        @Size(max = 150, message = "Airport name must be at most 150 characters")
        String name,

        @NotBlank(message = "IATA code is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Airport IATA code must be exactly 3 letters")
        String iataCode,

        @Pattern(regexp = "^$|^[A-Za-z]{4}$", message = "Airport ICAO code must be exactly 4 letters")
        String icaoCode,

        @Size(max = 80, message = "City must be at most 80 characters")
        String city,

        @Size(max = 80, message = "Country must be at most 80 characters")
        String country,

        Double latitude,

        Double longitude,

        @Size(max = 80, message = "Timezone must be at most 80 characters")
        String timezone
) {
}