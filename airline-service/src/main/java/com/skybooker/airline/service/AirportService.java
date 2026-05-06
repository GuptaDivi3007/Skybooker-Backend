package com.skybooker.airline.service;

import com.skybooker.airline.dto.AirportRequest;
import com.skybooker.airline.dto.AirportResponse;
import com.skybooker.airline.dto.MessageResponse;

import java.util.List;

public interface AirportService {

    AirportResponse createAirport(AirportRequest request);

    AirportResponse getAirportById(String airportId);

    AirportResponse getAirportByIataCode(String iataCode);

    AirportResponse getAirportByIcaoCode(String icaoCode);

    List<AirportResponse> getAllAirports();

    List<AirportResponse> getActiveAirports();

    List<AirportResponse> getAirportsByCity(String city);

    List<AirportResponse> getAirportsByCountry(String country);

    List<AirportResponse> searchAirports(String keyword);

    AirportResponse updateAirport(String airportId, AirportRequest request);

    MessageResponse deactivateAirport(String airportId);

    MessageResponse activateAirport(String airportId);

    MessageResponse deleteAirport(String airportId);
}