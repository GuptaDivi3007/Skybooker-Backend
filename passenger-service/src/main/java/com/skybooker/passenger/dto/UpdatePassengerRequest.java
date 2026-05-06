package com.skybooker.passenger.dto;

import com.skybooker.passenger.entity.Gender;
import com.skybooker.passenger.entity.PassengerType;

import java.time.LocalDate;

public record UpdatePassengerRequest(

        String title,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        String passportNumber,
        String nationality,
        LocalDate passportExpiry,
        PassengerType passengerType
) {}