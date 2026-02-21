package com.palmonas.crm.module.auth.service;

import com.palmonas.crm.common.exception.AuthenticationException;
import com.palmonas.crm.common.exception.BadRequestException;
import com.palmonas.crm.module.auth.dto.*;
import com.palmonas.crm.module.auth.model.RefreshToken;
import com.palmonas.crm.module.auth.repository.RefreshTokenRepository;
import com.palmonas.crm.module.auth.security.JwtTokenProvider;
import com.palmonas.crm.module.user.model.Role;
import com.palmonas.crm.module.user.model.User;
import com.palmonas.crm.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!user.isActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        log.info("User logged in: {}", user.getEmail());
        return generateTokens(user);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.VIEWER)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());
        return generateTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

        if (storedToken.isExpired()) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new AuthenticationException("Refresh token expired");
        }

        // Rotate: revoke old token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        log.info("Token refreshed for user: {}", user.getEmail());
        return generateTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    log.info("User logged out, token revoked");
                });
    }

    private TokenResponse generateTokens(User user) {
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshTokenStr = tokenProvider.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .expiresAt(Instant.now().plusMillis(tokenProvider.getRefreshTokenExpiry()))
                .build();
        refreshTokenRepository.save(refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpiry() / 1000)
                .user(TokenResponse.UserInfo.builder()
                        .id(user.getId().toString())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}
