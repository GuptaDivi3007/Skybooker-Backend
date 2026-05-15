package com.skybooker.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.auth.entity.RefreshToken;
import com.skybooker.auth.entity.Role;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.exception.TokenException;
import com.skybooker.auth.repository.RefreshTokenRepository;
import com.skybooker.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new RefreshTokenServiceImpl(refreshTokenRepository, jwtService, redisTemplate, objectMapper);
        ReflectionTestUtils.setField(service, "refreshTokenPrefix", "test:refresh:");
        ReflectionTestUtils.setField(service, "userRefreshTokenPrefix", "test:user-refresh:");
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void createRefreshTokenPersistsAndCachesToken() {
        when(jwtService.extractExpiration("refresh-token")).thenReturn(LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken token = service.createRefreshToken(user(), "refresh-token");

        assertEquals("refresh-token", token.getTokenValue());
        assertFalse(token.isRevoked());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(valueOperations).set(eq("test:refresh:refresh-token"), anyString(), any());
        verify(setOperations).add("test:user-refresh:user-1", "refresh-token");
    }

    @Test
    void verifyRefreshTokenFallsBackToRepositoryWhenRedisMisses() {
        when(valueOperations.get("test:refresh:refresh-token")).thenReturn(null);
        RefreshToken stored = token(false, LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenValue("refresh-token")).thenReturn(Optional.of(stored));

        RefreshToken verified = service.verifyRefreshToken("refresh-token");

        assertEquals("user-1", verified.getUserId());
        verify(refreshTokenRepository).findByTokenValue("refresh-token");
    }

    @Test
    void verifyRefreshTokenRejectsRevokedToken() {
        when(valueOperations.get("test:refresh:refresh-token")).thenReturn(null);
        when(refreshTokenRepository.findByTokenValue("refresh-token"))
                .thenReturn(Optional.of(token(true, LocalDateTime.now().plusDays(1))));

        assertThrows(TokenException.class, () -> service.verifyRefreshToken("refresh-token"));
    }

    @Test
    void verifyRefreshTokenRejectsExpiredToken() {
        when(valueOperations.get("test:refresh:refresh-token")).thenReturn(null);
        when(refreshTokenRepository.findByTokenValue("refresh-token"))
                .thenReturn(Optional.of(token(false, LocalDateTime.now().minusMinutes(1))));

        assertThrows(TokenException.class, () -> service.verifyRefreshToken("refresh-token"));
    }

    @Test
    void revokeAllUserTokensRevokesDatabaseTokensAndRedisTokens() {
        RefreshToken token = token(false, LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findAllByUserId("user-1")).thenReturn(java.util.List.of(token));
        when(redisTemplate.opsForSet().members("test:user-refresh:user-1")).thenReturn(Set.of("refresh-token"));
        when(valueOperations.get("test:refresh:refresh-token")).thenReturn(null);

        service.revokeAllUserTokens("user-1");

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).saveAll(java.util.List.of(token));
    }

    private User user() {
        User user = new User();
        user.setUserId("user-1");
        user.setFullName("Aarav Mehta");
        user.setEmail("user@test.com");
        user.setRole(Role.PASSENGER);
        return user;
    }

    private RefreshToken token(boolean revoked, LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setTokenId("token-1");
        token.setTokenValue("refresh-token");
        token.setUserId("user-1");
        token.setExpiresAt(expiresAt);
        token.setRevoked(revoked);
        token.setCreatedAt(LocalDateTime.now());
        return token;
    }
}
