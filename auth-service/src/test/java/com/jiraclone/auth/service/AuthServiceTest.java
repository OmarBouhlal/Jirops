// auth-service/src/test/java/com/jiraclone/auth/service/AuthServiceTest.java
package com.jiraclone.auth.service;

import com.jiraclone.auth.domain.RefreshToken;
import com.jiraclone.auth.domain.User;
import com.jiraclone.auth.dto.AuthResponse;
import com.jiraclone.auth.dto.LoginRequest;
import com.jiraclone.auth.dto.RefreshRequest;
import com.jiraclone.auth.dto.RegisterRequest;
import com.jiraclone.auth.exception.EmailAlreadyExistsException;
import com.jiraclone.auth.exception.InvalidCredentialsException;
import com.jiraclone.auth.exception.InvalidTokenException;
import com.jiraclone.auth.repository.RefreshTokenRepository;
import com.jiraclone.auth.repository.UserRepository;
import com.jiraclone.commons.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtTokenProvider);
        ReflectionTestUtils.setField(authService, "accessTokenExpiryMs", 3_600_000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiryMs", 7_200_000L);
    }

    @Test
    @DisplayName("register_success")
    void register_success() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return user;
        });
        when(jwtTokenProvider.generateAccessToken("11111111-1111-1111-1111-111111111111", List.of("ROLE_USER")))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("11111111-1111-1111-1111-111111111111"))
                .thenReturn("refresh-token");

        AuthResponse response = authService.register(new RegisterRequest("user@example.com", "secret"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-secret");
        assertThat(userCaptor.getValue().getRoles()).containsExactly("ROLE_USER");
        verify(jwtTokenProvider).generateAccessToken("11111111-1111-1111-1111-111111111111", List.of("ROLE_USER"));
        verify(jwtTokenProvider).generateRefreshToken("11111111-1111-1111-1111-111111111111");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("register_emailAlreadyExists")
    void register_emailAlreadyExists() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("user@example.com", "secret")))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("login_success")
    void login_success() {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        User user = new User(userId, "user@example.com", "$2a$10$hash", List.of("ROLE_USER"), Instant.now());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "$2a$10$hash")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(userId.toString(), List.of("ROLE_USER"))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(userId.toString())).thenReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "secret"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).deleteByUserId(userId);
    }

    @Test
    @DisplayName("login_wrongPassword")
    void login_wrongPassword() {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        User user = new User(userId, "user@example.com", "$2a$10$hash", List.of("ROLE_USER"), Instant.now());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "$2a$10$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "bad")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    @DisplayName("login_userNotFound")
    void login_userNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@example.com", "secret")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("refresh_success")
    void refresh_success() {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        User user = new User(userId, "user@example.com", "$2a$10$hash", List.of("ROLE_USER"), Instant.now());
        RefreshToken refreshToken = new RefreshToken(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "refresh-token",
                userId,
                Instant.now().plusSeconds(300),
                false
        );
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("refresh-token")).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(userId.toString(), List.of("ROLE_USER"))).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(userId.toString())).thenReturn("new-refresh");

        AuthResponse response = authService.refresh(new RefreshRequest("refresh-token"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(refreshToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(refreshToken);
        verify(jwtTokenProvider).generateAccessToken(userId.toString(), List.of("ROLE_USER"));
        verify(jwtTokenProvider).generateRefreshToken(userId.toString());
    }

    @Test
    @DisplayName("refresh_revokedToken")
    void refresh_revokedToken() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("refresh-token")))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("refresh_expiredToken")
    void refresh_expiredToken() {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        RefreshToken refreshToken = new RefreshToken(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "refresh-token",
                userId,
                Instant.now().minusSeconds(1),
                false
        );
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("refresh-token")))
                .isInstanceOf(InvalidTokenException.class);
        verify(jwtTokenProvider, never()).validateToken(any());
    }

    @Test
    @DisplayName("refresh_invalidTokenValidationFalse")
    void refresh_invalidTokenValidationFalse() {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        RefreshToken refreshToken = new RefreshToken(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "refresh-token",
                userId,
                Instant.now().plusSeconds(300),
                false
        );
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("refresh-token")))
                .isInstanceOf(InvalidTokenException.class);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("refresh_userIdMismatch")
    void refresh_userIdMismatch() {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        RefreshToken refreshToken = new RefreshToken(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "refresh-token",
                userId,
                Instant.now().plusSeconds(300),
                false
        );
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("refresh-token")).thenReturn(UUID.randomUUID().toString());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("refresh-token")))
                .isInstanceOf(InvalidTokenException.class);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("refresh_userNotFound")
    void refresh_userNotFound() {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        RefreshToken refreshToken = new RefreshToken(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "refresh-token",
                userId,
                Instant.now().plusSeconds(300),
                false
        );
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("refresh-token")).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("refresh-token")))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("logout_success")
    void logout_success() {
        UUID userId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        RefreshToken refreshToken = new RefreshToken(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                "refresh-token",
                userId,
                Instant.now().plusSeconds(300),
                false
        );
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("refresh-token")).thenReturn(userId.toString());

        authService.logout(new RefreshRequest("refresh-token"));

        assertThat(refreshToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    @DisplayName("logout_invalidTokenValidationFalse")
    void logout_invalidTokenValidationFalse() {
        UUID userId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        RefreshToken refreshToken = new RefreshToken(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                "refresh-token",
                userId,
                Instant.now().plusSeconds(300),
                false
        );
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.logout(new RefreshRequest("refresh-token")))
                .isInstanceOf(InvalidTokenException.class);
        verify(refreshTokenRepository, never()).save(refreshToken);
    }

    @Test
    @DisplayName("logout_userIdMismatch")
    void logout_userIdMismatch() {
        UUID userId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        RefreshToken refreshToken = new RefreshToken(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                "refresh-token",
                userId,
                Instant.now().plusSeconds(300),
                false
        );
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("refresh-token")).thenReturn(UUID.randomUUID().toString());

        assertThatThrownBy(() -> authService.logout(new RefreshRequest("refresh-token")))
                .isInstanceOf(InvalidTokenException.class);
        verify(refreshTokenRepository, never()).save(refreshToken);
    }

    @Test
    @DisplayName("login_success_withNullRoles")
    void login_success_withNullRoles() {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        User user = new User(userId, "user@example.com", "$2a$10$hash", null, Instant.now());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "$2a$10$hash")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(userId.toString(), List.of())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(userId.toString())).thenReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "secret"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(jwtTokenProvider).generateAccessToken(userId.toString(), List.of());
    }
}
