package com.skybooker.airline.dto;

public record AirportResponse(
        String airportId,
        String name,
        String iataCode,
        String icaoCode,
        String city,
        String country,
        Double latitude,
        Double longitude,
        String timezone,
        boolean active
) {
}