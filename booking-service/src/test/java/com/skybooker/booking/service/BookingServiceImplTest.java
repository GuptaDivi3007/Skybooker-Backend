package com.skybooker.booking.service;

import com.skybooker.booking.client.FlightClient;
import com.skybooker.booking.client.PassengerClient;
import com.skybooker.booking.client.PaymentClient;
import com.skybooker.booking.client.SeatClient;
import com.skybooker.booking.dto.*;
import com.skybooker.booking.entity.*;
import com.skybooker.booking.exception.BadRequestException;
import com.skybooker.booking.exception.ResourceNotFoundException;
import com.skybooker.booking.producer.BookingNotificationProducer;
import com.skybooker.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private FlightClient flightClient;
    @Mock
    private SeatClient seatClient;
    @Mock
    private BookingNotificationProducer notificationProducer;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private PassengerClient passengerClient;

    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(bookingRepository, flightClient, seatClient, notificationProducer, paymentClient, passengerClient);
        ReflectionTestUtils.setField(service, "taxRate", 0.12);
        ReflectionTestUtils.setField(service, "mealPrice", 450.0);
        ReflectionTestUtils.setField(service, "baggagePrice", 180.0);
    }

    @Test
    void calculateFareIncludesTaxesMealAndBaggage() {
        when(flightClient.getFlightById("flight-1")).thenReturn(flight(100));

        FareSummaryResponse response = service.calculateFare(new FareCalculationRequest("flight-1", 2, MealPreference.VEG, 5));

        assertEquals(9000.0, response.baseFare());
        assertEquals(1080.0, response.taxes());
        assertEquals(900.0, response.mealCost());
        assertEquals(900.0, response.baggageCost());
        assertEquals(11880.0, response.totalFare());
    }

    @Test
    void createBookingRejectsSeatCountMismatch() {
        BookingRequest request = bookingRequest(List.of("seat-1"), 2);

        assertThrows(BadRequestException.class, () -> service.createBooking("user-1", request));
        verifyNoInteractions(flightClient, seatClient, paymentClient);
    }

    @Test
    void confirmBookingConfirmsSeatsAndDecrementsFlightSeats() {
        Booking booking = booking(BookingStatus.PENDING);
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(flightClient.getFlightById("flight-1")).thenReturn(flight(10));
        when(passengerClient.getPassengersByBooking("booking-1")).thenReturn(List.of());

        BookingResponse response = service.confirmBooking("booking-1");

        assertEquals(BookingStatus.CONFIRMED, response.status());
        verify(seatClient).confirmSeat("seat-1", "user-1");
        verify(flightClient).decrementSeats("flight-1", 1);
        verify(notificationProducer).sendBookingConfirmed(any(NotificationEvent.class));
    }

    @Test
    void cancelConfirmedBookingReleasesConfirmedSeatAndIncrementsFlightSeats() {
        Booking booking = booking(BookingStatus.CONFIRMED);
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(flightClient.getFlightById("flight-1")).thenReturn(flight(10));
        when(passengerClient.getPassengersByBooking("booking-1")).thenReturn(List.of());

        BookingResponse response = service.cancelBooking("booking-1");

        assertEquals(BookingStatus.CANCELLED, response.status());
        verify(seatClient).cancelConfirmedSeat("seat-1", "user-1");
        verify(flightClient).incrementSeats("flight-1", 1);
        verify(notificationProducer).sendBookingCancelled(any(NotificationEvent.class));
    }

    @Test
    void linkPaymentRejectsBookingIdMismatch() {
        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking(BookingStatus.PENDING)));

        assertThrows(BadRequestException.class,
                () -> service.linkPayment("booking-1", new PaymentLinkRequest("other-booking", "payment-1")));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(bookingRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById("missing"));
    }

    private BookingRequest bookingRequest(List<String> seatIds, int passengerCount) {
        return new BookingRequest(
                "flight-1",
                seatIds,
                TripType.ONE_WAY,
                passengerCount,
                List.of(new PassengerRequest(
                        null, "Mr", "Aarav", "Mehta", LocalDate.now().minusYears(25),
                        "MALE", "P1234567", "Indian", LocalDate.now().plusYears(5), "ADULT"
                )),
                MealPreference.NONE,
                0,
                "user@test.com",
                "9999999999"
        );
    }

    private FlightResponse flight(int availableSeats) {
        return new FlightResponse(
                "flight-1", "SB101", "airline-1", "DEL", "BOM",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2),
                120, "ON_TIME", "A320", 180, availableSeats, 4500.0
        );
    }

    private Booking booking(BookingStatus status) {
        Booking booking = new Booking();
        booking.setBookingId("booking-1");
        booking.setUserId("user-1");
        booking.setFlightId("flight-1");
        booking.setPnrCode("ABC123");
        booking.setTripType(TripType.ONE_WAY);
        booking.setStatus(status);
        booking.setSeatIds(List.of("seat-1"));
        booking.setPassengerCount(1);
        booking.setBaseFare(4500.0);
        booking.setTaxes(540.0);
        booking.setMealCost(0.0);
        booking.setBaggageCost(0.0);
        booking.setTotalFare(5040.0);
        booking.setMealPreference(MealPreference.NONE);
        booking.setLuggageKg(0);
        booking.setContactEmail("user@test.com");
        booking.setContactPhone("9999999999");
        booking.setBookedAt(LocalDateTime.now());
        return booking;
    }
}
