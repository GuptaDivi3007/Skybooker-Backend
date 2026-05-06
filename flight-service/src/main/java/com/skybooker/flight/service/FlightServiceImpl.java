package com.skybooker.flight.service;

import com.skybooker.flight.dto.*;
import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.entity.FlightStatus;
import com.skybooker.flight.exception.ResourceNotFoundException;
import com.skybooker.flight.repository.FlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;

    public FlightServiceImpl(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Override
    public FlightResponse addFlight(FlightRequest request) {
        validateTimes(request.departureTime(), request.arrivalTime());

        String flightNumber = normalizeRequired(request.flightNumber(), "Flight number");

        if (flightRepository.existsByFlightNumberIgnoreCase(flightNumber)) {
            throw new IllegalArgumentException("Flight already exists with number: " + flightNumber);
        }

        Flight flight = new Flight();
        flight.setFlightNumber(flightNumber);
        flight.setAirlineId(normalizeRequired(request.airlineId(), "Airline id"));
        flight.setOriginAirportCode(normalizeAirportCode(request.originAirportCode(), "Origin airport code"));
        flight.setDestinationAirportCode(normalizeAirportCode(request.destinationAirportCode(), "Destination airport code"));
        flight.setDepartureTime(request.departureTime());
        flight.setArrivalTime(request.arrivalTime());
        flight.setDurationMinutes(calculateDurationMinutes(request.departureTime(), request.arrivalTime()));
        flight.setStatus(FlightStatus.ON_TIME);
        flight.setAircraftType(normalizeRequired(request.aircraftType(), "Aircraft type"));
        flight.setTotalSeats(request.totalSeats());
        flight.setAvailableSeats(request.totalSeats());
        flight.setBasePrice(request.basePrice());

        return mapToResponse(flightRepository.save(flight));
    }

    @Override
    public FlightResponse getFlightById(String flightId) {
        return mapToResponse(findFlight(flightId));
    }

    @Override
    public FlightResponse getFlightByNumber(String flightNumber) {
        Flight flight = flightRepository.findByFlightNumberIgnoreCase(normalizeRequired(flightNumber, "Flight number"))
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with number: " + flightNumber));
        return mapToResponse(flight);
    }

    @Override
    public List<FlightResponse> getAllFlights() {
        return flightRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<FlightResponse> getFlightsByAirline(String airlineId) {
        return flightRepository.findByAirlineId(airlineId).stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<FlightResponse> getFlightsByStatus(FlightStatus status) {
        return flightRepository.findByStatus(status).stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<FlightResponse> searchFlights(FlightSearchRequest request) {
        validatePassengerTravelDate(request.departureDate());
        LocalDateTime start = request.departureDate().atStartOfDay();
        LocalDateTime end = request.departureDate().plusDays(1).atStartOfDay().minusNanos(1);

        return flightRepository
                .findByOriginAirportCodeIgnoreCaseAndDestinationAirportCodeIgnoreCaseAndDepartureTimeBetweenAndStatusNot(
                        normalizeAirportCode(request.originAirportCode(), "Origin airport code"),
                        normalizeAirportCode(request.destinationAirportCode(), "Destination airport code"),
                        start,
                        end,
                        FlightStatus.CANCELLED
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validatePassengerTravelDate(LocalDate departureDate) {
        LocalDate today = LocalDate.now();
        LocalDate maxBookingDate = today.plusMonths(3);

        if (departureDate.isBefore(today)) {
            throw new IllegalArgumentException("Please choose today or a future travel date.");
        }

        if (departureDate.isAfter(maxBookingDate)) {
            throw new IllegalArgumentException("Passengers can book flights only within the next 3 months.");
        }
    }

    @Override
    public RoundTripSearchResponse searchRoundTrip(RoundTripSearchRequest request) {
        List<FlightResponse> outbound = searchFlights(new FlightSearchRequest(
                request.originAirportCode(),
                request.destinationAirportCode(),
                request.departureDate(),
                request.passengers()
        ));

        List<FlightResponse> returns = searchFlights(new FlightSearchRequest(
                request.destinationAirportCode(),
                request.originAirportCode(),
                request.returnDate(),
                request.passengers()
        ));

        return new RoundTripSearchResponse(outbound, returns);
    }

    @Override
    public FlightResponse updateFlight(String flightId, FlightRequest request) {
        validateTimes(request.departureTime(), request.arrivalTime());

        Flight flight = findFlight(flightId);

        String newFlightNumber = normalizeRequired(request.flightNumber(), "Flight number");

        if (!flight.getFlightNumber().equalsIgnoreCase(newFlightNumber)
                && flightRepository.existsByFlightNumberIgnoreCase(newFlightNumber)) {
            throw new IllegalArgumentException("Flight already exists with number: " + newFlightNumber);
        }

        int bookedSeats = flight.getTotalSeats() - flight.getAvailableSeats();

        if (request.totalSeats() < bookedSeats) {
            throw new IllegalArgumentException("Total seats cannot be less than already booked seats: " + bookedSeats);
        }

        flight.setFlightNumber(newFlightNumber);
        flight.setAirlineId(normalizeRequired(request.airlineId(), "Airline id"));
        flight.setOriginAirportCode(normalizeAirportCode(request.originAirportCode(), "Origin airport code"));
        flight.setDestinationAirportCode(normalizeAirportCode(request.destinationAirportCode(), "Destination airport code"));
        flight.setDepartureTime(request.departureTime());
        flight.setArrivalTime(request.arrivalTime());
        flight.setDurationMinutes(calculateDurationMinutes(request.departureTime(), request.arrivalTime()));
        flight.setAircraftType(normalizeRequired(request.aircraftType(), "Aircraft type"));
        flight.setBasePrice(request.basePrice());

        int newAvailableSeats = request.totalSeats() - bookedSeats;
        flight.setTotalSeats(request.totalSeats());
        flight.setAvailableSeats(newAvailableSeats);

        return mapToResponse(flightRepository.save(flight));
    }

    @Override
    public FlightResponse updateStatus(String flightId, FlightStatusUpdateRequest request) {
        Flight flight = findFlight(flightId);
        flight.setStatus(request.status());
        return mapToResponse(flightRepository.save(flight));
    }

    @Override
    public FlightResponse decrementSeats(String flightId, Integer seats) {
        if (seats == null || seats <= 0) {
            throw new IllegalArgumentException("Seats must be greater than 0");
        }

        Flight flight = findFlight(flightId);

        if (flight.getStatus() == FlightStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot decrement seats for cancelled flight");
        }

        if (flight.getAvailableSeats() < seats) {
            throw new IllegalArgumentException("Not enough available seats");
        }

        flight.setAvailableSeats(flight.getAvailableSeats() - seats);
        return mapToResponse(flightRepository.save(flight));
    }

    @Override
    public FlightResponse incrementSeats(String flightId, Integer seats) {
        if (seats == null || seats <= 0) {
            throw new IllegalArgumentException("Seats must be greater than 0");
        }

        Flight flight = findFlight(flightId);

        int newAvailableSeats = flight.getAvailableSeats() + seats;

        if (newAvailableSeats > flight.getTotalSeats()) {
            throw new IllegalArgumentException("Available seats cannot exceed total seats");
        }

        flight.setAvailableSeats(newAvailableSeats);
        return mapToResponse(flightRepository.save(flight));
    }

    @Override
    public MessageResponse deleteFlight(String flightId) {
        Flight flight = findFlight(flightId);
        flightRepository.delete(flight);
        return new MessageResponse("Flight deleted successfully");
    }

    @Override
    public Long countFlightsByAirline(String airlineId) {
        return flightRepository.countByAirlineId(airlineId);
    }

    private Flight findFlight(String flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id: " + flightId));
    }

    private void validateTimes(LocalDateTime departure, LocalDateTime arrival) {
        if (departure == null || arrival == null) {
            throw new IllegalArgumentException("Departure time and arrival time are required");
        }

        if (!arrival.isAfter(departure)) {
            throw new IllegalArgumentException("Arrival time must be after departure time");
        }
    }

    private Integer calculateDurationMinutes(LocalDateTime departure, LocalDateTime arrival) {
        return Math.toIntExact(Duration.between(departure, arrival).toMinutes());
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim().toUpperCase();
    }

    private String normalizeAirportCode(String value, String fieldName) {
        String normalized = normalizeRequired(value, fieldName);

        if (!normalized.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException(fieldName + " must be exactly 3 letters");
        }

        return normalized;
    }

    private FlightResponse mapToResponse(Flight flight) {
        return new FlightResponse(
                flight.getFlightId(),
                flight.getFlightNumber(),
                flight.getAirlineId(),
                flight.getOriginAirportCode(),
                flight.getDestinationAirportCode(),
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                flight.getDurationMinutes(),
                flight.getStatus(),
                flight.getAircraftType(),
                flight.getTotalSeats(),
                flight.getAvailableSeats(),
                flight.getBasePrice()
        );
    }
}
