package com.skybooker.auth.repository;

import com.skybooker.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenValue(String tokenValue);

    List<RefreshToken> findAllByUserId(String userId);
}