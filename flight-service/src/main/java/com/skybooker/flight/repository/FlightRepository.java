package com.skybooker.flight.repository;

import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.entity.FlightStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, String> {

    Optional<Flight> findByFlightNumberIgnoreCase(String flightNumber);

    boolean existsByFlightNumberIgnoreCase(String flightNumber);

    List<Flight> findByAirlineId(String airlineId);

    List<Flight> findByStatus(FlightStatus status);

    long countByAirlineId(String airlineId);

    List<Flight> findByOriginAirportCodeIgnoreCaseAndDestinationAirportCodeIgnoreCaseAndDepartureTimeBetweenAndStatusNotAndAvailableSeatsGreaterThanEqual(
            String originAirportCode,
            String destinationAirportCode,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            FlightStatus excludedStatus,
            Integer passengerCount
    );

    List<Flight> findByOriginAirportCodeIgnoreCaseAndDestinationAirportCodeIgnoreCaseAndDepartureTimeBetweenAndStatusNot(
            String originAirportCode,
            String destinationAirportCode,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            FlightStatus excludedStatus
    );
}