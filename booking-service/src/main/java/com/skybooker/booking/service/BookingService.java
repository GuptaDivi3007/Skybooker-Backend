package com.skybooker.booking.service;

import com.skybooker.booking.dto.*;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(String userId, BookingRequest request);

    BookingResponse confirmBooking(String bookingId);

    BookingResponse cancelBooking(String bookingId);

    BookingResponse getById(String bookingId);

    BookingResponse getByPnr(String pnr);

    List<BookingResponse> getUserBookings(String userId);

    List<BookingResponse> getUpcomingBookings(String userId);

    List<BookingResponse> getFlightBookings(String flightId);

    FareSummaryResponse calculateFare(FareCalculationRequest request);

    BookingResponse addAddOns(String bookingId, AddOnRequest request);

    BookingResponse updateStatus(String bookingId, StatusUpdateRequest request);
    
    BookingResponse linkPayment(String bookingId, PaymentLinkRequest request);
}