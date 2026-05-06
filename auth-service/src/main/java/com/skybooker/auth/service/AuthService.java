package com.skybooker.auth.service;

import com.skybooker.auth.dto.*;
import com.skybooker.auth.entity.Role;

import java.util.List;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    RegistrationOtpResponse requestRegistrationOtp(RegisterRequest request);

    AuthResponse verifyRegistrationOtp(VerifyRegistrationOtpRequest request);

    AuthResponse login(LoginRequest request);

    MessageResponse logout(String email, String authHeader);

    AuthResponse refresh(RefreshTokenRequest request);

    UserResponse getProfile(String email);

    UserResponse updateProfile(String email, UpdateProfileRequest request);

    MessageResponse changePassword(String email, ChangePasswordRequest request);

    MessageResponse deactivateOwnAccount(String email);

    TokenValidationResponse validateToken(String token);

    List<UserResponse> getAllUsers();

    List<UserResponse> getUsersByRole(Role role);

    UserResponse getUserById(String userId);

    MessageResponse suspendUser(String userId);

    MessageResponse reactivateUser(String userId);

    MessageResponse deleteUser(String userId);
}
