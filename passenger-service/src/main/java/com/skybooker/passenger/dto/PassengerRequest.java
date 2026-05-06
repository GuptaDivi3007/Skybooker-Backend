package com.skybooker.passenger.dto;

import com.skybooker.passenger.entity.Gender;
import com.skybooker.passenger.entity.PassengerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PassengerRequest(

        @NotBlank(message = "Booking id is required")
        String bookingId,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth,

        @NotNull(message = "Gender is required")
        Gender gender,

        @NotBlank(message = "Passport number is required")
        String passportNumber,

        @NotBlank(message = "Nationality is required")
        String nationality,

        @NotNull(message = "Passport expiry is required")
        LocalDate passportExpiry,

        PassengerType passengerType
) {}