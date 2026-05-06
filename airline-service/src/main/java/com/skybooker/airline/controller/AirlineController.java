package com.skybooker.airline.controller;

import com.skybooker.airline.dto.AirlineRequest;
import com.skybooker.airline.dto.AirlineResponse;
import com.skybooker.airline.dto.MessageResponse;
import com.skybooker.airline.exception.AccessDeniedException;
import com.skybooker.airline.service.AirlineService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/airlines")
public class AirlineController {

    private final AirlineService airlineService;

    public AirlineController(AirlineService airlineService) {
        this.airlineService = airlineService;
    }

    @PostMapping
    public AirlineResponse createAirline(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @Valid @RequestBody AirlineRequest request) {
        requireAdmin(role);
        return airlineService.createAirline(request);
    }

    @GetMapping
    public List<AirlineResponse> getAllAirlines() {
        return airlineService.getAllAirlines();
    }

    @GetMapping("/active")
    public List<AirlineResponse> getActiveAirlines() {
        return airlineService.getActiveAirlines();
    }

    @GetMapping("/{airlineId}")
    public AirlineResponse getAirlineById(@PathVariable String airlineId) {
        return airlineService.getAirlineById(airlineId);
    }

    @GetMapping("/iata/{iataCode}")
    public AirlineResponse getAirlineByIataCode(@PathVariable String iataCode) {
        return airlineService.getAirlineByIataCode(iataCode);
    }

    @GetMapping("/icao/{icaoCode}")
    public AirlineResponse getAirlineByIcaoCode(@PathVariable String icaoCode) {
        return airlineService.getAirlineByIcaoCode(icaoCode);
    }

    @PutMapping("/{airlineId}")
    public AirlineResponse updateAirline(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String airlineId,
            @Valid @RequestBody AirlineRequest request) {
        requireAdmin(role);
        return airlineService.updateAirline(airlineId, request);
    }

    @PutMapping("/{airlineId}/deactivate")
    public MessageResponse deactivateAirline(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String airlineId) {
        requireAdmin(role);
        return airlineService.deactivateAirline(airlineId);
    }

    @PutMapping("/{airlineId}/activate")
    public MessageResponse activateAirline(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String airlineId) {
        requireAdmin(role);
        return airlineService.activateAirline(airlineId);
    }

    @DeleteMapping("/{airlineId}")
    public MessageResponse deleteAirline(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String airlineId) {
        requireAdmin(role);
        return airlineService.deleteAirline(airlineId);
    }

    private void requireAdmin(String role) {
        if (role == null || !role.equalsIgnoreCase("ADMIN")) {
            throw new AccessDeniedException("Only ADMIN can perform this action");
        }
    }
}