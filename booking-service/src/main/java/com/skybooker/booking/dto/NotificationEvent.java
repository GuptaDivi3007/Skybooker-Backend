package com.skybooker.booking.dto;

import java.time.LocalDateTime;

public record NotificationEvent(
        String eventType,
        String notificationType,
        String userId,
        String recipientEmail,
        String recipientPhone,
        String title,
        String message,
        String bookingId,
        String pnrCode,
        String paymentId,
        String flightNumber,
        String airlineId,
        String originAirportCode,
        String destinationAirportCode,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        Integer durationMinutes,
        String aircraftType,
        String seatNumbers,
        String passengerNames,
        Integer passengerCount,
        Double baseFare,
        Double taxes,
        Double mealCost,
        Double baggageCost,
        Double totalFare,
        String bookingStatus,
        LocalDateTime createdAt
) {
}
