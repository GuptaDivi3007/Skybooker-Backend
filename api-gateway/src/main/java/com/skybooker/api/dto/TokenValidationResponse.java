package com.skybooker.api.dto;

public record TokenValidationResponse(
        boolean valid,
        String email,
        String userId,
        String role,
        boolean active
) {
}