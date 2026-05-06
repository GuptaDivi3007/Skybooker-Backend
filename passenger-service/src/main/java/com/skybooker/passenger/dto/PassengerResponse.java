package com.skybooker.passenger.dto;

import com.skybooker.passenger.entity.Gender;
import com.skybooker.passenger.entity.PassengerType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PassengerResponse(

        String passengerId,
        String bookingId,

        String title,
        String firstName,
        String lastName,

        LocalDate dateOfBirth,
        Gender gender,

        String passportNumber,
        String nationality,
        LocalDate passportExpiry,

        String seatId,
        String seatNumber,

        String ticketNumber,
        PassengerType passengerType,

        boolean checkedIn,
        LocalDateTime checkInTime,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}