package com.skybooker.auth.dto;

import com.skybooker.auth.entity.Role;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}