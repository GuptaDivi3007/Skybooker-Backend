package com.skybooker.airline.repository;

import com.skybooker.airline.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AirportRepository extends JpaRepository<Airport, String> {

    Optional<Airport> findByIataCodeIgnoreCase(String iataCode);

    Optional<Airport> findByIcaoCodeIgnoreCase(String icaoCode);

    List<Airport> findByActiveTrue();

    List<Airport> findByCityContainingIgnoreCase(String city);

    List<Airport> findByCountryContainingIgnoreCase(String country);

    List<Airport> findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrCountryContainingIgnoreCaseOrIataCodeContainingIgnoreCase(
            String name,
            String city,
            String country,
            String iataCode
    );

    boolean existsByIataCodeIgnoreCase(String iataCode);

    boolean existsByIcaoCodeIgnoreCase(String icaoCode);
}