package com.skybooker.airline.service;

import com.skybooker.airline.dto.AirlineRequest;
import com.skybooker.airline.dto.AirlineResponse;
import com.skybooker.airline.dto.MessageResponse;
import com.skybooker.airline.entity.Airline;
import com.skybooker.airline.exception.ResourceNotFoundException;
import com.skybooker.airline.repository.AirlineRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AirlineServiceImpl implements AirlineService {

    private final AirlineRepository airlineRepository;

    public AirlineServiceImpl(AirlineRepository airlineRepository) {
        this.airlineRepository = airlineRepository;
    }

    @Override
    public AirlineResponse createAirline(AirlineRequest request) {
        String iataCode = normalizeRequired(request.iataCode(), "IATA code");
        String icaoCode = normalizeOptional(request.icaoCode());

        if (airlineRepository.existsByIataCodeIgnoreCase(iataCode)) {
            throw new IllegalArgumentException("Airline already exists with IATA code: " + iataCode);
        }

        if (icaoCode != null && airlineRepository.existsByIcaoCodeIgnoreCase(icaoCode)) {
            throw new IllegalArgumentException("Airline already exists with ICAO code: " + icaoCode);
        }

        Airline airline = new Airline();
        airline.setName(normalizeName(request.name(), "Airline name"));
        airline.setIataCode(iataCode);
        airline.setIcaoCode(icaoCode);
        airline.setLogoUrl(blankToNull(request.logoUrl()));
        airline.setCountry(blankToNull(request.country()));
        airline.setContactEmail(blankToNull(request.contactEmail()));
        airline.setContactPhone(blankToNull(request.contactPhone()));
        airline.setActive(true);

        Airline saved = airlineRepository.save(airline);
        return mapToResponse(saved);
    }

    @Override
    public AirlineResponse getAirlineById(String airlineId) {
        return mapToResponse(findAirlineById(airlineId));
    }

    @Override
    public AirlineResponse getAirlineByIataCode(String iataCode) {
        Airline airline = airlineRepository.findByIataCodeIgnoreCase(normalizeRequired(iataCode, "IATA code"))
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with IATA code: " + iataCode));

        return mapToResponse(airline);
    }

    @Override
    public AirlineResponse getAirlineByIcaoCode(String icaoCode) {
        Airline airline = airlineRepository.findByIcaoCodeIgnoreCase(normalizeRequired(icaoCode, "ICAO code"))
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with ICAO code: " + icaoCode));

        return mapToResponse(airline);
    }

    @Override
    public List<AirlineResponse> getAllAirlines() {
        return airlineRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AirlineResponse> getActiveAirlines() {
        return airlineRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public AirlineResponse updateAirline(String airlineId, AirlineRequest request) {
        Airline airline = findAirlineById(airlineId);

        String newIataCode = normalizeRequired(request.iataCode(), "IATA code");
        String newIcaoCode = normalizeOptional(request.icaoCode());

        if (!airline.getIataCode().equalsIgnoreCase(newIataCode)
                && airlineRepository.existsByIataCodeIgnoreCase(newIataCode)) {
            throw new IllegalArgumentException("Airline already exists with IATA code: " + newIataCode);
        }

        if (newIcaoCode != null
                && (airline.getIcaoCode() == null || !airline.getIcaoCode().equalsIgnoreCase(newIcaoCode))
                && airlineRepository.existsByIcaoCodeIgnoreCase(newIcaoCode)) {
            throw new IllegalArgumentException("Airline already exists with ICAO code: " + newIcaoCode);
        }

        airline.setName(normalizeName(request.name(), "Airline name"));
        airline.setIataCode(newIataCode);
        airline.setIcaoCode(newIcaoCode);
        airline.setLogoUrl(blankToNull(request.logoUrl()));
        airline.setCountry(blankToNull(request.country()));
        airline.setContactEmail(blankToNull(request.contactEmail()));
        airline.setContactPhone(blankToNull(request.contactPhone()));

        Airline saved = airlineRepository.save(airline);
        return mapToResponse(saved);
    }

    @Override
    public MessageResponse deactivateAirline(String airlineId) {
        Airline airline = findAirlineById(airlineId);
        airline.setActive(false);
        airlineRepository.save(airline);
        return new MessageResponse("Airline deactivated successfully");
    }

    @Override
    public MessageResponse activateAirline(String airlineId) {
        Airline airline = findAirlineById(airlineId);
        airline.setActive(true);
        airlineRepository.save(airline);
        return new MessageResponse("Airline activated successfully");
    }

    @Override
    public MessageResponse deleteAirline(String airlineId) {
        Airline airline = findAirlineById(airlineId);
        airlineRepository.delete(airline);
        return new MessageResponse("Airline deleted successfully");
    }

    private Airline findAirlineById(String airlineId) {
        return airlineRepository.findById(airlineId)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with id: " + airlineId));
    }

    private AirlineResponse mapToResponse(Airline airline) {
        return new AirlineResponse(
                airline.getAirlineId(),
                airline.getName(),
                airline.getIataCode(),
                airline.getIcaoCode(),
                airline.getLogoUrl(),
                airline.getCountry(),
                airline.getContactEmail(),
                airline.getContactPhone(),
                airline.isActive()
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