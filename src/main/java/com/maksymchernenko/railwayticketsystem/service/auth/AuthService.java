package com.maksymchernenko.railwayticketsystem.service.auth;

import com.maksymchernenko.railwayticketsystem.dto.auth.AuthResponse;
import com.maksymchernenko.railwayticketsystem.dto.auth.LoginRequest;
import com.maksymchernenko.railwayticketsystem.dto.auth.RefreshTokenRequest;
import com.maksymchernenko.railwayticketsystem.dto.auth.RegisterRequest;
import com.maksymchernenko.railwayticketsystem.dto.auth.UserResponse;
import com.maksymchernenko.railwayticketsystem.security.CustomUserDetails;
import org.jspecify.annotations.Nullable;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    UserResponse currentUser(@Nullable CustomUserDetails userDetails);
}
