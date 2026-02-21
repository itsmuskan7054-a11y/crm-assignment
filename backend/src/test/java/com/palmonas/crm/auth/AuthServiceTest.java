package com.palmonas.crm.auth;

import com.palmonas.crm.common.exception.AuthenticationException;
import com.palmonas.crm.common.exception.BadRequestException;
import com.palmonas.crm.module.auth.dto.LoginRequest;
import com.palmonas.crm.module.auth.dto.RegisterRequest;
import com.palmonas.crm.module.auth.dto.TokenResponse;
import com.palmonas.crm.module.auth.repository.RefreshTokenRepository;
import com.palmonas.crm.module.auth.security.JwtTokenProvider;
import com.palmonas.crm.module.auth.service.AuthService;
import com.palmonas.crm.module.user.model.Role;
import com.palmonas.crm.module.user.model.User;
import com.palmonas.crm.module.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginShouldReturnTokensForValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashed")
                .fullName("Test User")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(tokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(tokenProvider.getAccessTokenExpiry()).thenReturn(900000L);
        when(tokenProvider.getRefreshTokenExpiry()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TokenResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Test User", response.getUser().getFullName());
    }

    @Test
    void loginShouldThrowForInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong");

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashed")
                .isActive(true)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(AuthenticationException.class, () -> authService.login(request));
    }

    @Test
    void loginShouldThrowForNonexistentUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nobody@example.com");
        request.setPassword("password");

        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> authService.login(request));
    }

    @Test
    void registerShouldThrowForDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password");
        request.setFullName("Test");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }
}
