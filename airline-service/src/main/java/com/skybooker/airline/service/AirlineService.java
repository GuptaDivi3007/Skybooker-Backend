package com.skybooker.airline.service;

import com.skybooker.airline.dto.AirlineRequest;
import com.skybooker.airline.dto.AirlineResponse;
import com.skybooker.airline.dto.MessageResponse;

import java.util.List;

public interface AirlineService {

    AirlineResponse createAirline(AirlineRequest request);

    AirlineResponse getAirlineById(String airlineId);

    AirlineResponse getAirlineByIataCode(String iataCode);

    AirlineResponse getAirlineByIcaoCode(String icaoCode);

    List<AirlineResponse> getAllAirlines();

    List<AirlineResponse> getActiveAirlines();

    AirlineResponse updateAirline(String airlineId, AirlineRequest request);

    MessageResponse deactivateAirline(String airlineId);

    MessageResponse activateAirline(String airlineId);

    MessageResponse deleteAirline(String airlineId);
}