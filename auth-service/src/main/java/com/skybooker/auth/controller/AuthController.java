package com.skybooker.auth.controller;

import com.skybooker.auth.dto.*;
import com.skybooker.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/register/request-otp")
    public RegistrationOtpResponse requestRegistrationOtp(@Valid @RequestBody RegisterRequest request) {
        return authService.requestRegistrationOtp(request);
    }

    @PostMapping("/register/verify-otp")
    public AuthResponse verifyRegistrationOtp(@Valid @RequestBody VerifyRegistrationOtpRequest request) {
        return authService.verifyRegistrationOtp(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public MessageResponse logout(Authentication authentication,
                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return authService.logout(authentication.getName(), authHeader);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @GetMapping("/profile")
    public UserResponse getProfile(Authentication authentication) {
        return authService.getProfile(authentication.getName());
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(Authentication authentication,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(authentication.getName(), request);
    }

    @PutMapping("/password")
    public MessageResponse changePassword(Authentication authentication,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(authentication.getName(), request);
    }

    @PutMapping("/deactivate")
    public MessageResponse deactivateAccount(Authentication authentication) {
        return authService.deactivateOwnAccount(authentication.getName());
    }

    @GetMapping("/validate")
    public TokenValidationResponse validateToken(@RequestParam("token") String token) {
        return authService.validateToken(token);
    }
}
