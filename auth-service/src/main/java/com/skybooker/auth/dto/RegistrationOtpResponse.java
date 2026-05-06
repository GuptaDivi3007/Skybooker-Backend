package com.skybooker.auth.dto;

public record RegistrationOtpResponse(
        String message,
        String registrationToken,
        int expiresInMinutes
) {
}
