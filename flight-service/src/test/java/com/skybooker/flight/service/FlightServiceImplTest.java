package com.skybooker.flight.service;

import com.skybooker.flight.dto.FlightRequest;
import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.dto.FlightSearchRequest;
import com.skybooker.flight.dto.FlightStatusUpdateRequest;
import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.entity.FlightStatus;
import com.skybooker.flight.exception.ResourceNotFoundException;
import com.skybooker.flight.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceImplTest {

    @Mock
    private FlightRepository flightRepository;

    private FlightServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FlightServiceImpl(flightRepository);
    }

    @Test
    void addFlightNormalizesFieldsAndCalculatesDuration() {
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlightResponse response = service.addFlight(request(" sb101 ", 180));

        assertEquals("SB101", response.flightNumber());
        assertEquals("DEL", response.originAirportCode());
        assertEquals(120, response.durationMinutes());
        assertEquals(FlightStatus.ON_TIME, response.status());
        assertEquals(180, response.availableSeats());
    }

    @Test
    void addFlightRejectsDuplicateFlightNumber() {
        when(flightRepository.existsByFlightNumberIgnoreCase("SB101")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.addFlight(request("SB101", 180)));
        verify(flightRepository, never()).save(any());
    }

    @Test
    void decrementSeatsReducesAvailableSeats() {
        Flight flight = flight(10, 6, FlightStatus.ON_TIME);
        when(flightRepository.findById("flight-1")).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlightResponse response = service.decrementSeats("flight-1", 2);

        assertEquals(4, response.availableSeats());
    }

    @Test
    void decrementSeatsRejectsWhenNotEnoughSeatsAvailable() {
        when(flightRepository.findById("flight-1")).thenReturn(Optional.of(flight(10, 1, FlightStatus.ON_TIME)));

        assertThrows(IllegalArgumentException.class, () -> service.decrementSeats("flight-1", 2));
    }

    @Test
    void searchFlightsRejectsPastTravelDate() {
        FlightSearchRequest search = new FlightSearchRequest("DEL", "BOM", LocalDate.now().minusDays(1), 1);

        assertThrows(IllegalArgumentException.class, () -> service.searchFlights(search));
    }

    @Test
    void getFlightByIdThrowsWhenMissing() {
        when(flightRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getFlightById("missing"));
    }

    @Test
    void updateStatusChangesFlightStatus() {
        Flight flight = flight(100, 100, FlightStatus.ON_TIME);
        when(flightRepository.findById("flight-1")).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlightResponse response = service.updateStatus("flight-1", new FlightStatusUpdateRequest(FlightStatus.DELAYED));

        assertEquals(FlightStatus.DELAYED, response.status());
    }

    private FlightRequest request(String flightNumber, int totalSeats) {
        return new FlightRequest(
                flightNumber,
                " airline-1 ",
                " del ",
                " bom ",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                " Airbus A320 ",
                totalSeats,
                4500.0
        );
    }

    private Flight flight(int totalSeats, int availableSeats, FlightStatus status) {
        Flight flight = new Flight();
        flight.setFlightId("flight-1");
        flight.setFlightNumber("SB101");
        flight.setAirlineId("airline-1");
        flight.setOriginAirportCode("DEL");
        flight.setDestinationAirportCode("BOM");
        flight.setDepartureTime(LocalDateTime.now().plusDays(1));
        flight.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        flight.setDurationMinutes(120);
        flight.setStatus(status);
        flight.setAircraftType("Airbus A320");
        flight.setTotalSeats(totalSeats);
        flight.setAvailableSeats(availableSeats);
        flight.setBasePrice(4500.0);
        return flight;
    }
}
