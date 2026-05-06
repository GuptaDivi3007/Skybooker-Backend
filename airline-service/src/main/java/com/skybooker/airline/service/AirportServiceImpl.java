package com.skybooker.airline.service;

import com.skybooker.airline.dto.AirportRequest;
import com.skybooker.airline.dto.AirportResponse;
import com.skybooker.airline.dto.MessageResponse;
import com.skybooker.airline.entity.Airport;
import com.skybooker.airline.exception.ResourceNotFoundException;
import com.skybooker.airline.repository.AirportRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AirportServiceImpl implements AirportService {

    private final AirportRepository airportRepository;

    public AirportServiceImpl(AirportRepository airportRepository) {
        this.airportRepository = airportRepository;
    }

    @Override
    public AirportResponse createAirport(AirportRequest request) {
        String iataCode = normalizeRequired(request.iataCode(), "IATA code");
        String icaoCode = normalizeOptional(request.icaoCode());

        if (airportRepository.existsByIataCodeIgnoreCase(iataCode)) {
            throw new IllegalArgumentException("Airport already exists with IATA code: " + iataCode);
        }

        if (icaoCode != null && airportRepository.existsByIcaoCodeIgnoreCase(icaoCode)) {
            throw new IllegalArgumentException("Airport already exists with ICAO code: " + icaoCode);
        }

        Airport airport = new Airport();
        airport.setName(normalizeName(request.name(), "Airport name"));
        airport.setIataCode(iataCode);
        airport.setIcaoCode(icaoCode);
        airport.setCity(blankToNull(request.city()));
        airport.setCountry(blankToNull(request.country()));
        airport.setLatitude(request.latitude());
        airport.setLongitude(request.longitude());
        airport.setTimezone(blankToNull(request.timezone()));
        airport.setActive(true);

        Airport saved = airportRepository.save(airport);
        return mapToResponse(saved);
    }

    @Override
    public AirportResponse getAirportById(String airportId) {
        return mapToResponse(findAirportById(airportId));
    }

    @Override
    public AirportResponse getAirportByIataCode(String iataCode) {
        Airport airport = airportRepository.findByIataCodeIgnoreCase(normalizeRequired(iataCode, "IATA code"))
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with IATA code: " + iataCode));

        return mapToResponse(airport);
    }

    @Override
    public AirportResponse getAirportByIcaoCode(String icaoCode) {
        Airport airport = airportRepository.findByIcaoCodeIgnoreCase(normalizeRequired(icaoCode, "ICAO code"))
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with ICAO code: " + icaoCode));

        return mapToResponse(airport);
    }

    @Override
    public List<AirportResponse> getAllAirports() {
        return airportRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AirportResponse> getActiveAirports() {
        return airportRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AirportResponse> getAirportsByCity(String city) {
        return airportRepository.findByCityContainingIgnoreCase(city)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AirportResponse> getAirportsByCountry(String country) {
        return airportRepository.findByCountryContainingIgnoreCase(country)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AirportResponse> searchAirports(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getActiveAirports();
        }

        String searchValue = keyword.trim();

        return airportRepository
                .findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrCountryContainingIgnoreCaseOrIataCodeContainingIgnoreCase(
                        searchValue,
                        searchValue,
                        searchValue,
                        searchValue
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public AirportResponse updateAirport(String airportId, AirportRequest request) {
        Airport airport = findAirportById(airportId);

        String newIataCode = normalizeRequired(request.iataCode(), "IATA code");
        String newIcaoCode = normalizeOptional(request.icaoCode());

        if (!airport.getIataCode().equalsIgnoreCase(newIataCode)
                && airportRepository.existsByIataCodeIgnoreCase(newIataCode)) {
            throw new IllegalArgumentException("Airport already exists with IATA code: " + newIataCode);
        }

        if (newIcaoCode != null
                && (airport.getIcaoCode() == null || !airport.getIcaoCode().equalsIgnoreCase(newIcaoCode))
                && airportRepository.existsByIcaoCodeIgnoreCase(newIcaoCode)) {
            throw new IllegalArgumentException("Airport already exists with ICAO code: " + newIcaoCode);
        }

        airport.setName(normalizeName(request.name(), "Airport name"));
        airport.setIataCode(newIataCode);
        airport.setIcaoCode(newIcaoCode);
        airport.setCity(blankToNull(request.city()));
        airport.setCountry(blankToNull(request.country()));
        airport.setLatitude(request.latitude());
        airport.setLongitude(request.longitude());
        airport.setTimezone(blankToNull(request.timezone()));

        Airport saved = airportRepository.save(airport);
        return mapToResponse(saved);
    }

    @Override
    public MessageResponse deactivateAirport(String airportId) {
        Airport airport = findAirportById(airportId);
        airport.setActive(false);
        airportRepository.save(airport);
        return new MessageResponse("Airport deactivated successfully");
    }

    @Override
    public MessageResponse activateAirport(String airportId) {
        Airport airport = findAirportById(airportId);
        airport.setActive(true);
        airportRepository.save(airport);
        return new MessageResponse("Airport activated successfully");
    }

    @Override
    public MessageResponse deleteAirport(String airportId) {
        Airport airport = findAirportById(airportId);
        airportRepository.delete(airport);
        return new MessageResponse("Airport deleted successfully");
    }

    private Airport findAirportById(String airportId) {
        return airportRepository.findById(airportId)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with id: " + airportId));
    }

    private AirportResponse mapToResponse(Airport airport) {
        return new AirportResponse(
                airport.getAirportId(),
                airport.getName(),
                airport.getIataCode(),
                airport.getIcaoCode(),
                airport.getCity(),
                airport.getCountry(),
                airport.getLatitude(),
                airport.getLongitude(),
                airport.getTimezone(),
                airport.isActive()
        );
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim().toUpperCase();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private String normalizeName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}