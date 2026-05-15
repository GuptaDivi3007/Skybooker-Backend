package com.skybooker.auth.service;

import com.skybooker.auth.entity.RefreshToken;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.exception.TokenException;
import com.skybooker.auth.repository.RefreshTokenRepository;
import com.skybooker.auth.security.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.refresh-token-prefix:skybooker:auth:refresh:}")
    private String refreshTokenPrefix;

    @Value("${app.redis.user-refresh-token-prefix:skybooker:auth:user-refresh:}")
    private String userRefreshTokenPrefix;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
                                   JwtService jwtService,
                                   StringRedisTemplate redisTemplate,
                                   ObjectMapper objectMapper) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public RefreshToken createRefreshToken(User user, String tokenValue) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenValue(tokenValue);
        refreshToken.setUserId(user.getUserId());
        refreshToken.setExpiresAt(jwtService.extractExpiration(tokenValue));
        refreshToken.setRevoked(false);
        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        cacheRefreshToken(savedToken);
        return savedToken;
    }

    @Override
    public RefreshToken verifyRefreshToken(String tokenValue) {
        RefreshToken refreshToken = findRefreshTokenInRedis(tokenValue)
                .orElseGet(() -> {
                    RefreshToken token = refreshTokenRepository.findByTokenValue(tokenValue)
                            .orElseThrow(() -> new TokenException("Refresh token not found"));
                    cacheRefreshToken(token);
                    return token;
                });

        if (refreshToken.isRevoked()) {
            throw new TokenException("Refresh token is revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Override
    public void revokeToken(String tokenValue) {
        refreshTokenRepository.findByTokenValue(tokenValue).ifPresent(token -> {
            token.setRevoked(true);
            RefreshToken savedToken = refreshTokenRepository.save(token);
            cacheRefreshToken(savedToken);
        });
    }

    @Override
    public void revokeAllUserTokens(String userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserId(userId);
        for (RefreshToken token : tokens) {
            token.setRevoked(true);
            cacheRefreshToken(token);
        }
        refreshTokenRepository.saveAll(tokens);
        revokeRedisTokensForUser(userId);
    }

    private Optional<RefreshToken> findRefreshTokenInRedis(String tokenValue) {
        try {
            String cachedValue = redisTemplate.opsForValue().get(refreshTokenKey(tokenValue));
            if (cachedValue == null || cachedValue.isBlank()) {
                return Optional.empty();
            }

            CachedRefreshToken cachedToken = objectMapper.readValue(cachedValue, CachedRefreshToken.class);
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setTokenId(cachedToken.tokenId());
            refreshToken.setTokenValue(cachedToken.tokenValue());
            refreshToken.setUserId(cachedToken.userId());
            refreshToken.setExpiresAt(cachedToken.expiresAt());
            refreshToken.setRevoked(cachedToken.revoked());
            refreshToken.setCreatedAt(cachedToken.createdAt());
            return Optional.of(refreshToken);
        } catch (RedisConnectionFailureException ex) {
            return Optional.empty();
        } catch (JsonProcessingException ex) {
            redisTemplate.delete(refreshTokenKey(tokenValue));
            return Optional.empty();
        }
    }

    private void cacheRefreshToken(RefreshToken refreshToken) {
        try {
            Duration ttl = Duration.between(LocalDateTime.now(), refreshToken.getExpiresAt());
            if (ttl.isNegative() || ttl.isZero()) {
                redisTemplate.delete(refreshTokenKey(refreshToken.getTokenValue()));
                return;
            }

            CachedRefreshToken cachedToken = new CachedRefreshToken(
                    refreshToken.getTokenId(),
                    refreshToken.getTokenValue(),
                    refreshToken.getUserId(),
                    refreshToken.getExpiresAt(),
                    refreshToken.isRevoked(),
                    refreshToken.getCreatedAt()
            );

            redisTemplate.opsForValue().set(
                    refreshTokenKey(refreshToken.getTokenValue()),
                    objectMapper.writeValueAsString(cachedToken),
                    ttl
            );
            redisTemplate.opsForSet().add(userRefreshTokenKey(refreshToken.getUserId()), refreshToken.getTokenValue());
            redisTemplate.expire(userRefreshTokenKey(refreshToken.getUserId()), ttl);
        } catch (RedisConnectionFailureException | JsonProcessingException ignored) {
            // Database remains the source of truth if Redis is temporarily unavailable.
        }
    }

    private void revokeRedisTokensForUser(String userId) {
        try {
            String userKey = userRefreshTokenKey(userId);
            Set<String> tokenValues = redisTemplate.opsForSet().members(userKey);
            if (tokenValues == null || tokenValues.isEmpty()) {
                return;
            }

            for (String tokenValue : tokenValues) {
                findRefreshTokenInRedis(tokenValue).ifPresent(token -> {
                    token.setRevoked(true);
                    cacheRefreshToken(token);
                });
            }
        } catch (RedisConnectionFailureException ignored) {
            // Revocation is already persisted in MySQL.
        }
    }

    private String refreshTokenKey(String tokenValue) {
        return refreshTokenPrefix + tokenValue;
    }

    private String userRefreshTokenKey(String userId) {
        return userRefreshTokenPrefix + userId;
    }

    private record CachedRefreshToken(
            String tokenId,
            String tokenValue,
            String userId,
            LocalDateTime expiresAt,
            boolean revoked,
            LocalDateTime createdAt
    ) {
    }
}
