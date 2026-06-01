package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.auth.UserResponse;
import com.maksymchernenko.railwayticketsystem.dto.auth.AuthResponse;
import com.maksymchernenko.railwayticketsystem.dto.auth.LoginRequest;
import com.maksymchernenko.railwayticketsystem.dto.auth.RefreshTokenRequest;
import com.maksymchernenko.railwayticketsystem.dto.auth.RegisterRequest;
import com.maksymchernenko.railwayticketsystem.exception.auth.InvalidRefreshTokenException;
import com.maksymchernenko.railwayticketsystem.exception.auth.RoleNotConfiguredException;
import com.maksymchernenko.railwayticketsystem.exception.auth.UserAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.model.reference.Role;
import com.maksymchernenko.railwayticketsystem.model.auth.RefreshToken;
import com.maksymchernenko.railwayticketsystem.model.auth.User;
import com.maksymchernenko.railwayticketsystem.model.auth.UserProfile;
import com.maksymchernenko.railwayticketsystem.repository.auth.RefreshTokenRepository;
import com.maksymchernenko.railwayticketsystem.repository.auth.RoleRepository;
import com.maksymchernenko.railwayticketsystem.repository.auth.UserRepository;
import com.maksymchernenko.railwayticketsystem.security.CustomUserDetails;
import com.maksymchernenko.railwayticketsystem.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final long REFRESH_EXPIRATION_MS = 86_400_000L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", REFRESH_EXPIRATION_MS);
    }

    @Test
    void registerShouldCreateLocalClientUserAndReturnTokens() {
        RegisterRequest request = new RegisterRequest(
                "  Example@Test.COM  ",
                "password123",
                "  John  ",
                "  Doe  "
        );

        Role role = role();
        User savedUser = user(1L, "example@test.com", role, "John", "Doe");

        when(userRepository.existsByEmail("example@test.com")).thenReturn(false);
        when(roleRepository.findById("CLIENT")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponse result = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User userToSave = userCaptor.getValue();

        assertThat(userToSave.getEmail()).isEqualTo("example@test.com");
        assertThat(userToSave.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(userToSave.getRole()).isSameAs(role);
        assertThat(userToSave.getProvider()).isEqualTo("LOCAL");
        assertThat(userToSave.getUserProfile().getFirstName()).isEqualTo("John");
        assertThat(userToSave.getUserProfile().getLastName()).isEqualTo("Doe");
        assertThat(userToSave.getUserProfile().getUser()).isSameAs(userToSave);

        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken issuedRefreshToken = refreshTokenCaptor.getValue();

        assertThat(issuedRefreshToken.getUser()).isSameAs(savedUser);
        assertThat(issuedRefreshToken.getTokenHash()).isEqualTo(hash(result.refreshToken()));
        assertThat(issuedRefreshToken.getExpiresAtUtc()).isAfter(Instant.now());

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresInSeconds()).isEqualTo(900L);
        assertThat(result.user()).isEqualTo(new UserResponse(1L, "example@test.com", "CLIENT", "John", "Doe"));
    }

    @Test
    void registerShouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "  Example@Test.COM  ",
                "password123",
                "John",
                "Doe"
        );

        when(userRepository.existsByEmail("example@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Email already used");

        verify(roleRepository, never()).findById(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerShouldThrowWhenClientRoleIsMissing() {
        RegisterRequest request = new RegisterRequest(
                "  Example@Test.COM  ",
                "password123",
                "John",
                "Doe"
        );

        when(userRepository.existsByEmail("example@test.com")).thenReturn(false);
        when(roleRepository.findById("CLIENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RoleNotConfiguredException.class)
                .hasMessage("Role CLIENT is not configured");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginShouldAuthenticateAndIssueTokensForAuthenticatedUser() {
        LoginRequest request = new LoginRequest("  Example@Test.COM  ", "password123");
        User user = user(2L, "example@test.com", role(), "Jane", "Doe");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(user),
                null
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateAccessToken(any(CustomUserDetails.class))).thenReturn("login-access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponse result = authService.login(request);

        verify(authenticationManager).authenticate(argThat(token ->
                "example@test.com".equals(token.getPrincipal()) && request.password().equals(token.getCredentials())
        ));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertThat(result.accessToken()).isEqualTo("login-access-token");
        assertThat(result.user()).isEqualTo(new UserResponse(2L, "example@test.com", "CLIENT", "Jane", "Doe"));
    }

    @Test
    void loginShouldThrowWhenAuthenticationPrincipalIsNotCustomUserDetails() {
        LoginRequest request = new LoginRequest("example@test.com", "password123");
        Authentication authentication = new UsernamePasswordAuthenticationToken("principal", null);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Authentication failed");
    }

    @Test
    void refreshShouldReplaceStoredTokenAndReturnNewTokens() {
        User user = user(3L, "example@test.com", role(), "Iryna", "Koval");
        String rawRefreshToken = "refresh-token";

        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(user);
        storedToken.setTokenHash(hash(rawRefreshToken));
        storedToken.setExpiresAtUtc(Instant.now().plusSeconds(300));

        when(refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))).thenReturn(Optional.of(storedToken));
        when(jwtService.generateAccessToken(any(CustomUserDetails.class))).thenReturn("refreshed-access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponse result = authService.refresh(new RefreshTokenRequest(rawRefreshToken));

        verify(refreshTokenRepository).deleteByExpiresAtUtcBefore(any(Instant.class));
        verify(refreshTokenRepository).delete(storedToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertThat(result.accessToken()).isEqualTo("refreshed-access-token");
        assertThat(result.refreshToken()).isNotBlank().isNotEqualTo(rawRefreshToken);
        assertThat(result.user()).isEqualTo(new UserResponse(3L, "example@test.com", "CLIENT", "Iryna", "Koval"));
    }

    @Test
    void refreshShouldThrowWhenTokenDoesNotExist() {
        when(refreshTokenRepository.findByTokenHash(hash("missing-token"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("missing-token")))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is invalid");
    }

    @Test
    void refreshShouldDeleteExpiredTokenAndThrow() {
        User user = user(4L, "example@test.com", role(), "Oleh", "Bondar");
        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(user);
        storedToken.setTokenHash(hash("expired-token"));
        storedToken.setExpiresAtUtc(Instant.now().minusSeconds(1));

        when(refreshTokenRepository.findByTokenHash(hash("expired-token"))).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("expired-token")))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token has expired");

        verify(refreshTokenRepository).delete(storedToken);
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void currentUserShouldMapAuthenticatedPrincipalToResponse() {
        User user = user(5L, "current@test.com", role(), "Anna", "Melnyk");

        UserResponse result = authService.currentUser(new CustomUserDetails(user));

        assertThat(result).isEqualTo(new UserResponse(5L, "current@test.com", "CLIENT", "Anna", "Melnyk"));
    }

    @Test
    void currentUserShouldThrowWhenAuthenticationIsMissing() {
        assertThatThrownBy(() -> authService.currentUser(null))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Authentication is required");
    }

    private static Role role() {
        Role role = new Role();
        role.setName("CLIENT");
        return role;
    }

    private static User user(Long id, String email, Role role, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setPasswordHash("encoded-password");
        user.setProvider("LOCAL");

        UserProfile profile = new UserProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        user.setUserProfile(profile);
        return user;
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
