package com.skybooker.flight.dto;

import com.skybooker.flight.entity.FlightStatus;

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
        FlightStatus status,
        String aircraftType,
        Integer totalSeats,
        Integer availableSeats,
        Double basePrice
) {
}