package com.skybooker.auth.dto;

import com.skybooker.auth.entity.Role;

public record TokenValidationResponse(
        boolean valid,
        String email,
        String userId,
        Role role,
        boolean active
) {
}