package com.skybooker.airline.controller;

import com.skybooker.airline.dto.AirportRequest;
import com.skybooker.airline.dto.AirportResponse;
import com.skybooker.airline.dto.MessageResponse;
import com.skybooker.airline.exception.AccessDeniedException;
import com.skybooker.airline.service.AirportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/airports")
public class AirportController {

    private final AirportService airportService;

    public AirportController(AirportService airportService) {
        this.airportService = airportService;
    }

    @PostMapping
    public AirportResponse createAirport(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @Valid @RequestBody AirportRequest request) {
        requireAdmin(role);
        return airportService.createAirport(request);
    }

    @GetMapping
    public List<AirportResponse> getAllAirports() {
        return airportService.getAllAirports();
    }

    @GetMapping("/active")
    public List<AirportResponse> getActiveAirports() {
        return airportService.getActiveAirports();
    }

    @GetMapping("/{airportId}")
    public AirportResponse getAirportById(@PathVariable String airportId) {
        return airportService.getAirportById(airportId);
    }

    @GetMapping("/iata/{iataCode}")
    public AirportResponse getAirportByIataCode(@PathVariable String iataCode) {
        return airportService.getAirportByIataCode(iataCode);
    }

    @GetMapping("/icao/{icaoCode}")
    public AirportResponse getAirportByIcaoCode(@PathVariable String icaoCode) {
        return airportService.getAirportByIcaoCode(icaoCode);
    }

    @GetMapping("/city/{city}")
    public List<AirportResponse> getAirportsByCity(@PathVariable String city) {
        return airportService.getAirportsByCity(city);
    }

    @GetMapping("/country/{country}")
    public List<AirportResponse> getAirportsByCountry(@PathVariable String country) {
        return airportService.getAirportsByCountry(country);
    }

    @GetMapping("/search")
    public List<AirportResponse> searchAirports(@RequestParam(required = false) String keyword) {
        return airportService.searchAirports(keyword);
    }

    @PutMapping("/{airportId}")
    public AirportResponse updateAirport(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String airportId,
            @Valid @RequestBody AirportRequest request) {
        requireAdmin(role);
        return airportService.updateAirport(airportId, request);
    }

    @PutMapping("/{airportId}/deactivate")
    public MessageResponse deactivateAirport(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String airportId) {
        requireAdmin(role);
        return airportService.deactivateAirport(airportId);
    }

    @PutMapping("/{airportId}/activate")
    public MessageResponse activateAirport(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String airportId) {
        requireAdmin(role);
        return airportService.activateAirport(airportId);
    }

    @DeleteMapping("/{airportId}")
    public MessageResponse deleteAirport(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String airportId) {
        requireAdmin(role);
        return airportService.deleteAirport(airportId);
    }

    private void requireAdmin(String role) {
        if (role == null || !role.equalsIgnoreCase("ADMIN")) {
            throw new AccessDeniedException("Only ADMIN can perform this action");
        }
    }
}