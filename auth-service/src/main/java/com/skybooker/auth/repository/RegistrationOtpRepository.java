package com.skybooker.auth.repository;

import com.skybooker.auth.entity.RegistrationOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistrationOtpRepository extends JpaRepository<RegistrationOtp, String> {

    Optional<RegistrationOtp> findTopByEmailOrderByCreatedAtDesc(String email);

    void deleteByEmail(String email);
}
