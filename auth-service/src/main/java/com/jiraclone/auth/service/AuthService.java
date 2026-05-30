// auth-service/src/main/java/com/jiraclone/auth/service/AuthService.java
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
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class AuthService {

    private static final String ROLE_USER = "ROLE_USER";
    private static final String BEARER = "Bearer";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(List.of(ROLE_USER));

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException("Email already exists", ex);
        }

        return createSession(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        refreshTokenRepository.deleteByUserId(user.getId());
        return createSession(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token is expired");
        }

        if (!jwtTokenProvider.validateToken(refreshToken.getToken())) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String userIdFromToken;
        try {
            userIdFromToken = jwtTokenProvider.getUserIdFromToken(refreshToken.getToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid refresh token", ex);
        }

        if (!refreshToken.getUserId().toString().equals(userIdFromToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        return createSession(user);
    }

    public void logout(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token is expired");
        }

        if (!jwtTokenProvider.validateToken(refreshToken.getToken())) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String userIdFromToken;
        try {
            userIdFromToken = jwtTokenProvider.getUserIdFromToken(refreshToken.getToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid refresh token", ex);
        }

        if (!refreshToken.getUserId().toString().equals(userIdFromToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    private AuthResponse createSession(User user) {
        Instant now = Instant.now();
        String userId = user.getId().toString();
        List<String> roles = user.getRoles() == null ? List.of() : List.copyOf(user.getRoles());

        String accessToken = jwtTokenProvider.generateAccessToken(userId, roles);
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(userId);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUserId(user.getId());
        refreshToken.setExpiresAt(now.plusMillis(refreshTokenExpiryMs));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                BEARER,
                Duration.ofMillis(accessTokenExpiryMs).toSeconds()
        );
    }
}
