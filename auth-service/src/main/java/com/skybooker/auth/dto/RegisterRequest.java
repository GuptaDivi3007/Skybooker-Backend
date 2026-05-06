package com.skybooker.auth.dto;

import com.skybooker.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        Role role,

        @Size(max = 30, message = "Passport number must be at most 30 characters")
        String passportNumber,

        @Size(max = 50, message = "Nationality must be at most 50 characters")
        String nationality
) {
}