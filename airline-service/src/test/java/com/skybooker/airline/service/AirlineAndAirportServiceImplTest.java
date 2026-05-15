package com.skybooker.airline.service;

import com.skybooker.airline.dto.AirlineRequest;
import com.skybooker.airline.dto.AirlineResponse;
import com.skybooker.airline.dto.AirportRequest;
import com.skybooker.airline.dto.AirportResponse;
import com.skybooker.airline.entity.Airline;
import com.skybooker.airline.entity.Airport;
import com.skybooker.airline.exception.ResourceNotFoundException;
import com.skybooker.airline.repository.AirlineRepository;
import com.skybooker.airline.repository.AirportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirlineAndAirportServiceImplTest {

    @Mock
    private AirlineRepository airlineRepository;

    @Mock
    private AirportRepository airportRepository;

    private AirlineServiceImpl airlineService;
    private AirportServiceImpl airportService;

    @BeforeEach
    void setUp() {
        airlineService = new AirlineServiceImpl(airlineRepository);
        airportService = new AirportServiceImpl(airportRepository);
    }

    @Test
    void createAirlineNormalizesCodesAndSavesActiveAirline() {
        when(airlineRepository.save(any(Airline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AirlineResponse response = airlineService.createAirline(new AirlineRequest(
                " Sky India ", " si ", " ski ", null, " India ", "ops@sky.test", "999"
        ));

        assertEquals("Sky India", response.name());
        assertEquals("SI", response.iataCode());
        assertEquals("SKI", response.icaoCode());
        assertTrue(response.active());
        verify(airlineRepository).save(any(Airline.class));
    }

    @Test
    void createAirlineRejectsDuplicateIataCode() {
        when(airlineRepository.existsByIataCodeIgnoreCase("SI")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> airlineService.createAirline(new AirlineRequest(
                "Sky India", "SI", null, null, "India", null, null
        )));

        verify(airlineRepository, never()).save(any());
    }

    @Test
    void createAirportNormalizesCodesAndSavesActiveAirport() {
        when(airportRepository.save(any(Airport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AirportResponse response = airportService.createAirport(new AirportRequest(
                " Delhi Airport ", " del ", " vidp ", " Delhi ", " India ", 28.56, 77.1, " Asia/Kolkata "
        ));

        assertEquals("Delhi Airport", response.name());
        assertEquals("DEL", response.iataCode());
        assertEquals("VIDP", response.icaoCode());
        assertTrue(response.active());
    }

    @Test
    void searchAirportsReturnsActiveAirportsWhenKeywordIsBlank() {
        Airport airport = new Airport();
        airport.setName("Delhi Airport");
        airport.setIataCode("DEL");
        airport.setActive(true);
        when(airportRepository.findByActiveTrue()).thenReturn(List.of(airport));

        List<AirportResponse> response = airportService.searchAirports(" ");

        assertEquals(1, response.size());
        assertEquals("DEL", response.get(0).iataCode());
        verify(airportRepository).findByActiveTrue();
    }

    @Test
    void getAirportByIdThrowsWhenAirportDoesNotExist() {
        when(airportRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> airportService.getAirportById("missing"));
    }
}
