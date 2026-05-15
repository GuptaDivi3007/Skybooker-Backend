package com.skybooker.auth.service;

import com.skybooker.auth.dto.*;
import com.skybooker.auth.entity.AuthProvider;
import com.skybooker.auth.entity.RegistrationOtp;
import com.skybooker.auth.entity.Role;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.exception.ResourceNotFoundException;
import com.skybooker.auth.exception.TokenException;
import com.skybooker.auth.exception.UnauthorizedException;
import com.skybooker.auth.repository.RegistrationOtpRepository;
import com.skybooker.auth.repository.UserRepository;
import com.skybooker.auth.security.JwtService;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RegistrationOtpRepository registrationOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final WelcomeEmailService welcomeEmailService;

    public AuthServiceImpl(UserRepository userRepository,
                           RegistrationOtpRepository registrationOtpRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           RefreshTokenService refreshTokenService,
                           WelcomeEmailService welcomeEmailService) {
        this.userRepository = userRepository;
        this.registrationOtpRepository = registrationOtpRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.welcomeEmailService = welcomeEmailService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        throw new IllegalArgumentException("Email OTP verification is required. Please request and verify OTP to register.");
    }

    @Override
    public RegistrationOtpResponse requestRegistrationOtp(RegisterRequest request) {
        validateNewUserDetails(request);
        String email = request.email().toLowerCase().trim();

        registrationOtpRepository.deleteByEmail(email);

        RegistrationOtp registrationOtp = new RegistrationOtp();
        registrationOtp.setEmail(email);
        registrationOtp.setOtpCode(generateOtp());
        registrationOtp.setFullName(request.fullName().trim());
        registrationOtp.setPasswordHash(passwordEncoder.encode(request.password()));
        registrationOtp.setPhone(blankToNull(request.phone()));
        registrationOtp.setRole(Role.PASSENGER.name());
        registrationOtp.setPassportNumber(blankToNull(request.passportNumber()));
        registrationOtp.setNationality(blankToNull(request.nationality()));
        registrationOtp.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        RegistrationOtp savedOtp = registrationOtpRepository.save(registrationOtp);
        welcomeEmailService.sendRegistrationOtpEmail(
                savedOtp.getEmail(),
                savedOtp.getFullName(),
                savedOtp.getOtpCode()
        );

        return new RegistrationOtpResponse(
                "OTP sent to your email. Verify it to create your SkyBooker account.",
                savedOtp.getRegistrationToken(),
                10
        );
    }

    @Override
    public AuthResponse verifyRegistrationOtp(VerifyRegistrationOtpRequest request) {
        RegistrationOtp pending = registrationOtpRepository.findById(request.registrationToken())
                .orElseThrow(() -> new IllegalArgumentException("Registration OTP request not found. Please request a new OTP."));

        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            registrationOtpRepository.delete(pending);
            throw new IllegalArgumentException("OTP expired. Please request a new OTP.");
        }

        if (!pending.getOtpCode().equals(request.otp())) {
            throw new IllegalArgumentException("Invalid OTP. Please check your email and try again.");
        }

        if (userRepository.existsByEmail(pending.getEmail())) {
            registrationOtpRepository.delete(pending);
            throw new IllegalArgumentException("Email already registered");
        }

        if (pending.getPhone() != null && userRepository.existsByPhone(pending.getPhone())) {
            registrationOtpRepository.delete(pending);
            throw new IllegalArgumentException("Phone already registered");
        }

        if (pending.getPassportNumber() != null && userRepository.existsByPassportNumber(pending.getPassportNumber())) {
            registrationOtpRepository.delete(pending);
            throw new IllegalArgumentException("Passport number already registered");
        }

        User user = new User();
        user.setFullName(pending.getFullName());
        user.setEmail(pending.getEmail());
        user.setPasswordHash(pending.getPasswordHash());
        user.setPhone(pending.getPhone());
        user.setRole(Role.valueOf(pending.getRole()));
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setPassportNumber(pending.getPassportNumber());
        user.setNationality(pending.getNationality());

        User savedUser = userRepository.save(user);
        registrationOtpRepository.delete(pending);
        welcomeEmailService.sendRegistrationSuccessEmail(savedUser);

        return createAuthResponse(savedUser);
    }

    private void validateNewUserDetails(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (request.phone() != null && !request.phone().isBlank() && userRepository.existsByPhone(request.phone())) {
            throw new IllegalArgumentException("Phone already registered");
        }

        if (request.passportNumber() != null && !request.passportNumber().isBlank()
                && userRepository.existsByPassportNumber(request.passportNumber())) {
            throw new IllegalArgumentException("Passport number already registered");
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Your account is deactivated");
        }

        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getUserId(),
                user.getRole().name()
        );

        String refreshTokenValue = jwtService.generateRefreshToken(
                user.getEmail(),
                user.getUserId()
        );

        refreshTokenService.createRefreshToken(user, refreshTokenValue);

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000,
                mapToUserResponse(user)
        );
    }

    @Override
    public MessageResponse logout(String email, String authHeader) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        refreshTokenService.revokeAllUserTokens(user.getUserId());

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String tokenEmail = jwtService.extractEmail(token);
                if (!email.equalsIgnoreCase(tokenEmail)) {
                    throw new UnauthorizedException("Token does not belong to current user");
                }
            } catch (Exception ignored) {
            }
        }

        return new MessageResponse("Logout successful");
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshTokenValue = request.refreshToken();

        refreshTokenService.verifyRefreshToken(refreshTokenValue);

        String email = jwtService.extractEmail(refreshTokenValue);
        String userId = jwtService.extractUserId(refreshTokenValue);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        if (!user.getUserId().equals(userId)) {
            throw new TokenException("Refresh token user mismatch");
        }

        String newAccessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getUserId(),
                user.getRole().name()
        );

        return new AuthResponse(
                newAccessToken,
                refreshTokenValue,
                "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000,
                mapToUserResponse(user)
        );
    }

    @Override
    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToUserResponse(user);
    }

    @Override
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }

        if (request.phone() != null) {
            String phone = blankToNull(request.phone());
            if (phone != null && !phone.equals(user.getPhone()) && userRepository.existsByPhone(phone)) {
                throw new IllegalArgumentException("Phone already in use");
            }
            user.setPhone(phone);
        }

        if (request.passportNumber() != null) {
            String passport = blankToNull(request.passportNumber());
            if (passport != null && !passport.equals(user.getPassportNumber())
                    && userRepository.existsByPassportNumber(passport)) {
                throw new IllegalArgumentException("Passport number already in use");
            }
            user.setPassportNumber(passport);
        }

        if (request.nationality() != null) {
            user.setNationality(blankToNull(request.nationality()));
        }

        User updated = userRepository.save(user);
        return mapToUserResponse(updated);
    }

    @Override
    public MessageResponse changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getProvider() == AuthProvider.GOOGLE && user.getPasswordHash() == null) {
            throw new IllegalArgumentException("Password change not available for Google-only account");
        }

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokens(user.getUserId());

        return new MessageResponse("Password changed successfully");
    }

    @Override
    public MessageResponse deactivateOwnAccount(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(false);
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(user.getUserId());

        return new MessageResponse("Account deactivated successfully");
    }

    @Override
    public TokenValidationResponse validateToken(String token) {
        try {
            String email = jwtService.extractEmail(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            boolean valid = jwtService.isTokenValid(token, email);

            return new TokenValidationResponse(
                    valid,
                    user.getEmail(),
                    user.getUserId(),
                    user.getRole(),
                    user.isActive()
            );
        } catch (Exception ex) {
            return new TokenValidationResponse(false, null, null, null, false);
        }
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    @Override
    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findAllByRole(role)
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    @Override
    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapToUserResponse(user);
    }

    @Override
    public MessageResponse suspendUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setActive(false);
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(user.getUserId());

        return new MessageResponse("User suspended successfully");
    }

    @Override
    public MessageResponse reactivateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setActive(true);
        userRepository.save(user);

        return new MessageResponse("User reactivated successfully");
    }

    @Override
    public MessageResponse deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        refreshTokenService.revokeAllUserTokens(user.getUserId());
        userRepository.delete(user);

        return new MessageResponse("User deleted successfully");
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getProvider(),
                user.isActive(),
                user.getPassportNumber(),
                user.getNationality(),
                user.getCreatedAt()
        );
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getUserId(),
                user.getRole().name()
        );

        String refreshTokenValue = jwtService.generateRefreshToken(
                user.getEmail(),
                user.getUserId()
        );

        refreshTokenService.createRefreshToken(user, refreshTokenValue);

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000,
                mapToUserResponse(user)
        );
    }

    private String generateOtp() {
        int code = 100000 + new Random().nextInt(900000);
        return String.valueOf(code);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
