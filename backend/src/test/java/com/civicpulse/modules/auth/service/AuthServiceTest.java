package com.civicpulse.modules.auth.service;

import com.civicpulse.common.exception.ConflictException;
import com.civicpulse.common.exception.UnauthorizedException;
import com.civicpulse.config.JwtConfig;
import com.civicpulse.modules.auth.dto.AuthResponse;
import com.civicpulse.modules.auth.dto.LoginRequest;
import com.civicpulse.modules.auth.dto.RegisterRequest;
import com.civicpulse.modules.auth.jwt.JwtTokenProvider;
import com.civicpulse.modules.auth.model.OtpChallenge;
import com.civicpulse.modules.auth.model.OtpPurpose;
import com.civicpulse.modules.auth.repository.RefreshTokenRepository;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.model.UserRole;
import com.civicpulse.modules.user.repository.UserRepository;
import com.civicpulse.ratelimit.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@civicpulse.org")
                .passwordHash("hashedPassword123")
                .fullName("Test Citizen")
                .role(UserRole.MEMBER)
                .active(true)
                .emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new pending user with OTP challenge")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@civicpulse.org")
                .password("Password@123")
                .fullName("New User")
                .role(UserRole.MEMBER)
                .build();

        OtpChallenge challenge = OtpChallenge.builder()
                .id(UUID.randomUUID())
                .email("new@civicpulse.org")
                .purpose(OtpPurpose.REGISTRATION_VERIFICATION)
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(otpService.createChallenge(any(), eq("new@civicpulse.org"), eq(OtpPurpose.REGISTRATION_VERIFICATION), isNull())).thenReturn(challenge);

        AuthResponse response = authService.register(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.isVerificationRequired()).isTrue();
        assertThat(response.getChallengeId()).isEqualTo(challenge.getId());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when registering duplicate email")
    void register_DuplicateEmail_ThrowsConflict() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@civicpulse.org")
                .password("Password@123")
                .fullName("Test Citizen")
                .build();

        when(userRepository.existsByEmail("test@civicpulse.org")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, "127.0.0.1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should successfully authenticate valid login request for verified user")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("test@civicpulse.org")
                .password("Password@123")
                .build();

        when(userRepository.findByEmail("test@civicpulse.org")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password@123", "hashedPassword123")).thenReturn(true);
        when(jwtConfig.getExpirationMs()).thenReturn(900000L);
        when(jwtConfig.getRefreshExpirationMs()).thenReturn(604800000L);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("sample.jwt.token");

        AuthResponse response = authService.login(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("sample.jwt.token");
        assertThat(response.getUser().getFullName()).isEqualTo("Test Citizen");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException for bad password")
    void login_BadPassword_ThrowsUnauthorized() {
        LoginRequest request = LoginRequest.builder()
                .email("test@civicpulse.org")
                .password("WrongPassword")
                .build();

        when(userRepository.findByEmail("test@civicpulse.org")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword", "hashedPassword123")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }
}
