package com.skybooker.passenger.service;

import com.skybooker.passenger.dto.AssignSeatRequest;
import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;
import com.skybooker.passenger.entity.Gender;
import com.skybooker.passenger.entity.PassengerInfo;
import com.skybooker.passenger.entity.PassengerType;
import com.skybooker.passenger.exception.ResourceNotFoundException;
import com.skybooker.passenger.repository.PassengerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassengerServiceImplTest {

    @Mock
    private PassengerRepository passengerRepository;

    private PassengerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PassengerServiceImpl(passengerRepository);
    }

    @Test
    void addPassengerUppercasesPassportAndDetectsAdultType() {
        when(passengerRepository.existsByTicketNumber(any())).thenReturn(false);
        when(passengerRepository.save(any(PassengerInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PassengerResponse response = service.addPassenger(validRequest(LocalDate.now().minusYears(25), null));

        assertEquals("P1234567", response.passportNumber());
        assertEquals(PassengerType.ADULT, response.passengerType());
        assertFalse(response.checkedIn());
    }

    @Test
    void addPassengerRejectsFutureDateOfBirth() {
        PassengerRequest request = validRequest(LocalDate.now().plusDays(1), PassengerType.ADULT);

        assertThrows(IllegalArgumentException.class, () -> service.addPassenger(request));
    }

    @Test
    void getPassengersByBookingMapsRepositoryResults() {
        when(passengerRepository.findByBookingId("booking-1")).thenReturn(List.of(passenger()));

        List<PassengerResponse> response = service.getPassengersByBooking("booking-1");

        assertEquals(1, response.size());
        assertEquals("Aarav", response.get(0).firstName());
    }

    @Test
    void assignSeatUpdatesSeatDetails() {
        PassengerInfo passenger = passenger();
        when(passengerRepository.findById("passenger-1")).thenReturn(Optional.of(passenger));
        when(passengerRepository.save(any(PassengerInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PassengerResponse response = service.assignSeat("passenger-1", new AssignSeatRequest("seat-1", "12a"));

        assertEquals("seat-1", response.seatId());
        assertEquals("12A", response.seatNumber());
    }

    @Test
    void getPassengerByIdThrowsWhenMissing() {
        when(passengerRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getPassengerById("missing"));
    }

    private PassengerRequest validRequest(LocalDate dateOfBirth, PassengerType passengerType) {
        return new PassengerRequest(
                "booking-1",
                "Mr",
                "Aarav",
                "Mehta",
                dateOfBirth,
                Gender.MALE,
                "p1234567",
                "Indian",
                LocalDate.now().plusYears(5),
                passengerType
        );
    }

    private PassengerInfo passenger() {
        PassengerInfo passenger = new PassengerInfo();
        passenger.setPassengerId("passenger-1");
        passenger.setBookingId("booking-1");
        passenger.setTitle("Mr");
        passenger.setFirstName("Aarav");
        passenger.setLastName("Mehta");
        passenger.setDateOfBirth(LocalDate.now().minusYears(25));
        passenger.setGender(Gender.MALE);
        passenger.setPassportNumber("P1234567");
        passenger.setNationality("Indian");
        passenger.setPassportExpiry(LocalDate.now().plusYears(5));
        passenger.setTicketNumber("TKT1234567890");
        passenger.setPassengerType(PassengerType.ADULT);
        passenger.setCheckedIn(false);
        return passenger;
    }
}
