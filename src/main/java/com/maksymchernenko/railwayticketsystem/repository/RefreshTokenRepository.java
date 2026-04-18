package com.maksymchernenko.railwayticketsystem.repository;

import com.maksymchernenko.railwayticketsystem.model.user.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByExpiresAtUtcBefore(Instant timestamp);
}
