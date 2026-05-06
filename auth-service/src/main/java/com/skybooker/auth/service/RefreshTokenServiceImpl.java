package com.skybooker.auth.service;

import com.skybooker.auth.entity.RefreshToken;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.exception.TokenException;
import com.skybooker.auth.repository.RefreshTokenRepository;
import com.skybooker.auth.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Override
    public RefreshToken createRefreshToken(User user, String tokenValue) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenValue(tokenValue);
        refreshToken.setUserId(user.getUserId());
        refreshToken.setExpiresAt(jwtService.extractExpiration(tokenValue));
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken verifyRefreshToken(String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenValue(tokenValue)
                .orElseThrow(() -> new TokenException("Refresh token not found"));

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
            refreshTokenRepository.save(token);
        });
    }

    @Override
    public void revokeAllUserTokens(String userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserId(userId);
        for (RefreshToken token : tokens) {
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(tokens);
    }
}