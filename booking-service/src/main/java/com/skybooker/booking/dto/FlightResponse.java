package com.skybooker.booking.dto;

import java.time.LocalDateTime;

public record FlightResponse(
        String flightId,
        String flightNumber,
        String airlineId,
        String originAirportCode,
        String destinationAirportCode,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        Integer durationMinutes,
        String status,
        String aircraftType,
        Integer totalSeats,
        Integer availableSeats,
        Double basePrice
) {
}