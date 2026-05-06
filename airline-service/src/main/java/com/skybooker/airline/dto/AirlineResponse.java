package com.skybooker.airline.dto;

public record AirlineResponse(
        String airlineId,
        String name,
        String iataCode,
        String icaoCode,
        String logoUrl,
        String country,
        String contactEmail,
        String contactPhone,
        boolean active
) {
}