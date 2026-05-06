package com.skybooker.passenger.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PassengerBulkRequest(

        @NotEmpty(message = "Passenger list cannot be empty")
        List<PassengerRequest> passengers
) {}