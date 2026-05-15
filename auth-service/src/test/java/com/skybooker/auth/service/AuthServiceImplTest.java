package com.skybooker.auth.service;

import com.skybooker.auth.dto.LoginRequest;
import com.skybooker.auth.dto.RegisterRequest;
import com.skybooker.auth.dto.UpdateProfileRequest;
import com.skybooker.auth.dto.VerifyRegistrationOtpRequest;
import com.skybooker.auth.entity.AuthProvider;
import com.skybooker.auth.entity.RegistrationOtp;
import com.skybooker.auth.entity.Role;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.exception.UnauthorizedException;
import com.skybooker.auth.repository.RegistrationOtpRepository;
import com.skybooker.auth.repository.UserRepository;
import com.skybooker.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationOtpRepository registrationOtpRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private WelcomeEmailService welcomeEmailService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                registrationOtpRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                refreshTokenService,
                welcomeEmailService
        );
    }

    @Test
    void requestRegistrationOtpCreatesPendingOtpAndSendsEmail() {
        RegisterRequest request = new RegisterRequest(
                " Divya ",
                "DIVYA@TEST.COM ",
                "password123",
                "9999999999",
                Role.ADMIN,
                "P123",
                "Indian"
        );
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(registrationOtpRepository.save(any(RegistrationOtp.class))).thenAnswer(invocation -> {
            RegistrationOtp otp = invocation.getArgument(0);
            otp.setRegistrationToken("reg-token");
            return otp;
        });

        var response = authService.requestRegistrationOtp(request);

        ArgumentCaptor<RegistrationOtp> otpCaptor = ArgumentCaptor.forClass(RegistrationOtp.class);
        verify(registrationOtpRepository).deleteByEmail("divya@test.com");
        verify(registrationOtpRepository).save(otpCaptor.capture());
        RegistrationOtp savedOtp = otpCaptor.getValue();
        assertThat(savedOtp.getEmail()).isEqualTo("divya@test.com");
        assertThat(savedOtp.getFullName()).isEqualTo("Divya");
        assertThat(savedOtp.getRole()).isEqualTo(Role.PASSENGER.name());
        assertThat(savedOtp.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedOtp.getOtpCode()).hasSize(6);
        verify(welcomeEmailService).sendRegistrationOtpEmail(eq("divya@test.com"), eq("Divya"), eq(savedOtp.getOtpCode()));
        assertThat(response.registrationToken()).isEqualTo("reg-token");
    }

    @Test
    void verifyRegistrationOtpCreatesPassengerAccountAndReturnsTokens() {
        RegistrationOtp pending = pendingOtp();
        User savedUser = passengerUser();
        when(registrationOtpRepository.findById("reg-token")).thenReturn(Optional.of(pending));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken("divya@test.com", "user-1", Role.PASSENGER.name())).thenReturn("access-token");
        when(jwtService.generateRefreshToken("divya@test.com", "user-1")).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(3_600_000L);

        var response = authService.verifyRegistrationOtp(new VerifyRegistrationOtpRequest("reg-token", "123456"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.PASSENGER);
        assertThat(userCaptor.getValue().getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(userCaptor.getValue().isActive()).isTrue();
        verify(registrationOtpRepository).delete(pending);
        verify(welcomeEmailService).sendRegistrationSuccessEmail(savedUser);
        verify(refreshTokenService).createRefreshToken(savedUser, "refresh-token");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().email()).isEqualTo("divya@test.com");
    }

    @Test
    void loginAuthenticatesLowercaseEmailAndCreatesRefreshToken() {
        User user = passengerUser();
        when(userRepository.findByEmail("divya@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken("divya@test.com", "user-1", Role.PASSENGER.name())).thenReturn("access-token");
        when(jwtService.generateRefreshToken("divya@test.com", "user-1")).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(3_600_000L);

        var response = authService.login(new LoginRequest(" DIVYA@TEST.COM ", "password123"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenService).createRefreshToken(user, "refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().role()).isEqualTo(Role.PASSENGER);
    }

    @Test
    void updateProfileRejectsPhoneAlreadyInUse() {
        User user = passengerUser();
        when(userRepository.findByEmail("divya@test.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByPhone("8888888888")).thenReturn(true);

        assertThatThrownBy(() -> authService.updateProfile(
                "divya@test.com",
                new UpdateProfileRequest("Divya Sharma", "8888888888", null, null)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone already in use");
    }

    @Test
    void inactiveUserCannotLogin() {
        User user = passengerUser();
        user.setActive(false);
        when(userRepository.findByEmail("divya@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("divya@test.com", "password123")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("deactivated");
    }

    private RegistrationOtp pendingOtp() {
        RegistrationOtp otp = new RegistrationOtp();
        otp.setRegistrationToken("reg-token");
        otp.setEmail("divya@test.com");
        otp.setOtpCode("123456");
        otp.setFullName("Divya");
        otp.setPasswordHash("encoded-password");
        otp.setPhone("9999999999");
        otp.setRole(Role.PASSENGER.name());
        otp.setPassportNumber("P123");
        otp.setNationality("Indian");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        return otp;
    }

    private User passengerUser() {
        User user = new User();
        user.setUserId("user-1");
        user.setFullName("Divya");
        user.setEmail("divya@test.com");
        user.setPasswordHash("encoded-password");
        user.setPhone("9999999999");
        user.setRole(Role.PASSENGER);
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setPassportNumber("P123");
        user.setNationality("Indian");
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
