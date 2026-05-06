package com.skybooker.auth.dto;

import com.skybooker.auth.entity.AuthProvider;
import com.skybooker.auth.entity.Role;

import java.time.LocalDateTime;

public record UserResponse(
        String userId,
        String fullName,
        String email,
        String phone,
        Role role,
        AuthProvider provider,
        boolean active,
        String passportNumber,
        String nationality,
        LocalDateTime createdAt
) {
}