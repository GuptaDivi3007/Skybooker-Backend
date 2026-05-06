package com.skybooker.flight.controller;

import com.skybooker.flight.dto.*;
import com.skybooker.flight.entity.FlightStatus;
import com.skybooker.flight.exception.AccessDeniedException;
import com.skybooker.flight.service.FlightService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @PostMapping
    public FlightResponse addFlight(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @Valid @RequestBody FlightRequest request) {
        requireAdminOrAirlineStaff(role);
        return flightService.addFlight(request);
    }

    @GetMapping
    public List<FlightResponse> getAllFlights() {
        return flightService.getAllFlights();
    }

    @GetMapping("/{flightId}")
    public FlightResponse getFlightById(@PathVariable String flightId) {
        return flightService.getFlightById(flightId);
    }

    @GetMapping("/number/{flightNumber}")
    public FlightResponse getFlightByNumber(@PathVariable String flightNumber) {
        return flightService.getFlightByNumber(flightNumber);
    }

    @GetMapping("/airline/{airlineId}")
    public List<FlightResponse> getFlightsByAirline(@PathVariable String airlineId) {
        return flightService.getFlightsByAirline(airlineId);
    }

    @GetMapping("/status/{status}")
    public List<FlightResponse> getFlightsByStatus(@PathVariable FlightStatus status) {
        return flightService.getFlightsByStatus(status);
    }

    @GetMapping("/count/airline/{airlineId}")
    public Long countFlightsByAirline(@PathVariable String airlineId) {
        return flightService.countFlightsByAirline(airlineId);
    }

    @PostMapping("/search")
    public List<FlightResponse> searchFlights(@Valid @RequestBody FlightSearchRequest request) {
        return flightService.searchFlights(request);
    }

    @GetMapping("/search")
    public List<FlightResponse> searchFlightsByQuery(
            @RequestParam String originAirportCode,
            @RequestParam String destinationAirportCode,
            @RequestParam String departureDate,
            @RequestParam(defaultValue = "1") Integer passengers) {
        return flightService.searchFlights(
                new FlightSearchRequest(
                        originAirportCode,
                        destinationAirportCode,
                        java.time.LocalDate.parse(departureDate),
                        passengers
                )
        );
    }

    @PostMapping("/round-trip")
    public RoundTripSearchResponse searchRoundTrip(@Valid @RequestBody RoundTripSearchRequest request) {
        return flightService.searchRoundTrip(request);
    }

    @PutMapping("/{flightId}")
    public FlightResponse updateFlight(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String flightId,
            @Valid @RequestBody FlightRequest request) {
        requireAdminOrAirlineStaff(role);
        return flightService.updateFlight(flightId, request);
    }

    @PutMapping("/{flightId}/status")
    public FlightResponse updateStatus(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String flightId,
            @Valid @RequestBody FlightStatusUpdateRequest request) {
        requireAdminOrAirlineStaff(role);
        return flightService.updateStatus(flightId, request);
    }

    @PutMapping("/{flightId}/decrement-seats")
    public FlightResponse decrementSeats(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String flightId,
            @RequestParam @Min(1) Integer seats) {
        requireInternalOrAdminOrStaff(role);
        return flightService.decrementSeats(flightId, seats);
    }

    @PutMapping("/{flightId}/increment-seats")
    public FlightResponse incrementSeats(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String flightId,
            @RequestParam @Min(1) Integer seats) {
        requireInternalOrAdminOrStaff(role);
        return flightService.incrementSeats(flightId, seats);
    }

    @DeleteMapping("/{flightId}")
    public MessageResponse deleteFlight(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String flightId) {
        requireAdminOrAirlineStaff(role);
        return flightService.deleteFlight(flightId);
    }

    private void requireAdminOrAirlineStaff(String role) {
        if (role == null ||
                (!role.equalsIgnoreCase("ADMIN") && !role.equalsIgnoreCase("AIRLINE_STAFF"))) {
            throw new AccessDeniedException("Only ADMIN or AIRLINE_STAFF can perform this action");
        }
    }

    private void requireInternalOrAdminOrStaff(String role) {
        if (role == null ||
                (!role.equalsIgnoreCase("ADMIN")
                        && !role.equalsIgnoreCase("AIRLINE_STAFF")
                        && !role.equalsIgnoreCase("INTERNAL"))) {
            throw new AccessDeniedException("Only ADMIN, AIRLINE_STAFF, or INTERNAL service can perform this action");
        }
    }
}