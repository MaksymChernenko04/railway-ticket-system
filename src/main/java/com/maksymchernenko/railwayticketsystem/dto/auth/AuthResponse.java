package com.maksymchernenko.railwayticketsystem.dto.auth;

import com.maksymchernenko.railwayticketsystem.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {
}
