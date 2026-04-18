package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.*;
import com.maksymchernenko.railwayticketsystem.dto.auth.AuthResponse;
import com.maksymchernenko.railwayticketsystem.dto.auth.LoginRequest;
import com.maksymchernenko.railwayticketsystem.dto.auth.RefreshTokenRequest;
import com.maksymchernenko.railwayticketsystem.dto.auth.RegisterRequest;
import com.maksymchernenko.railwayticketsystem.exception.auth.InvalidRefreshTokenException;
import com.maksymchernenko.railwayticketsystem.exception.auth.RoleNotConfiguredException;
import com.maksymchernenko.railwayticketsystem.exception.auth.UserAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.model.reference.Role;
import com.maksymchernenko.railwayticketsystem.model.user.*;
import com.maksymchernenko.railwayticketsystem.repository.user.RefreshTokenRepository;
import com.maksymchernenko.railwayticketsystem.repository.user.RoleRepository;
import com.maksymchernenko.railwayticketsystem.repository.user.UserRepository;
import com.maksymchernenko.railwayticketsystem.security.CustomUserDetails;
import com.maksymchernenko.railwayticketsystem.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already used");
        }

        Role role = roleRepository.findById("CLIENT")
                .orElseThrow(() -> new RoleNotConfiguredException("Role CLIENT is not configured"));

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setProvider("LOCAL");

        UserProfile profile = new UserProfile();
        profile.setFirstName(request.firstName().trim());
        profile.setLastName(request.lastName().trim());
        user.setUserProfile(profile);

        User savedUser = userRepository.save(user);
        return issueTokens(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails(User user))) {
            throw new BadCredentialsException("Authentication failed");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        refreshTokenRepository.deleteByExpiresAtUtcBefore(Instant.now());

        String tokenHash = hashToken(request.refreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid"));

        if (storedToken.getExpiresAtUtc().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        User user = storedToken.getUser();
        refreshTokenRepository.delete(storedToken);

        return issueTokens(user);
    }

    public UserResponse currentUser(@Nullable CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new BadCredentialsException("Authentication is required");
        }

        return toUserResponse(userDetails.user());
    }

    private AuthResponse issueTokens(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String rawRefreshToken = UUID.randomUUID().toString() + UUID.randomUUID();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawRefreshToken));
        refreshToken.setExpiresAtUtc(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                toUserResponse(user)
        );
    }

    private UserResponse toUserResponse(User user) {
        UserProfile profile = user.getUserProfile();
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().getName(),
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null
        );
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
