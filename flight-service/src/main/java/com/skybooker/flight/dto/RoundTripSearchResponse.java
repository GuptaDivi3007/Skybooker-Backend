package com.skybooker.flight.dto;

import java.util.List;

public record RoundTripSearchResponse(
        List<FlightResponse> outboundFlights,
        List<FlightResponse> returnFlights
) {
}