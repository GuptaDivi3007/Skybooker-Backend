package com.skybooker.auth.service;

import com.skybooker.auth.entity.RefreshToken;
import com.skybooker.auth.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user, String tokenValue);

    RefreshToken verifyRefreshToken(String tokenValue);

    void revokeToken(String tokenValue);

    void revokeAllUserTokens(String userId);
}