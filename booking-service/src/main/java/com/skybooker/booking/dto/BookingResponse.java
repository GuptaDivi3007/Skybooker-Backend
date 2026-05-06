package com.skybooker.booking.dto;

import com.skybooker.booking.entity.BookingStatus;
import com.skybooker.booking.entity.MealPreference;
import com.skybooker.booking.entity.TripType;

import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        String bookingId,
        String userId,
        String flightId,
        String pnrCode,
        TripType tripType,
        BookingStatus status,
        List<String> seatIds,
        Integer passengerCount,
        Double baseFare,
        Double taxes,
        Double mealCost,
        Double baggageCost,
        Double totalFare,
        MealPreference mealPreference,
        Integer luggageKg,
        String contactEmail,
        String contactPhone,
        String paymentId,
        LocalDateTime bookedAt,
        LocalDateTime updatedAt
) {
}