package com.skybooker.auth.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        @Size(max = 30, message = "Passport number must be at most 30 characters")
        String passportNumber,

        @Size(max = 50, message = "Nationality must be at most 50 characters")
        String nationality
) {
}