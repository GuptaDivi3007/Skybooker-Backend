package com.skybooker.flight.service;

import com.skybooker.flight.dto.*;
import com.skybooker.flight.entity.FlightStatus;

import java.util.List;

public interface FlightService {

    FlightResponse addFlight(FlightRequest request);

    FlightResponse getFlightById(String flightId);

    FlightResponse getFlightByNumber(String flightNumber);

    List<FlightResponse> getAllFlights();

    List<FlightResponse> getFlightsByAirline(String airlineId);

    List<FlightResponse> getFlightsByStatus(FlightStatus status);

    List<FlightResponse> searchFlights(FlightSearchRequest request);

    RoundTripSearchResponse searchRoundTrip(RoundTripSearchRequest request);

    FlightResponse updateFlight(String flightId, FlightRequest request);

    FlightResponse updateStatus(String flightId, FlightStatusUpdateRequest request);

    FlightResponse decrementSeats(String flightId, Integer seats);

    FlightResponse incrementSeats(String flightId, Integer seats);

    MessageResponse deleteFlight(String flightId);

    Long countFlightsByAirline(String airlineId);
}