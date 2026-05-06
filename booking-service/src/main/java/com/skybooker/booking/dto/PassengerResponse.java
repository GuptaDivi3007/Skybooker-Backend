package com.skybooker.booking.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PassengerResponse(
        String passengerId,
        String bookingId,
        String title,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String passportNumber,
        String nationality,
        LocalDate passportExpiry,
        String seatId,
        String seatNumber,
        String ticketNumber,
        String passengerType,
        boolean checkedIn,
        LocalDateTime checkInTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
