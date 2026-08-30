package com.civicpulse.modules.auth.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ConflictException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.common.exception.UnauthorizedException;
import com.civicpulse.common.security.CustomUserDetails;
import com.civicpulse.config.JwtConfig;
import com.civicpulse.modules.auth.dto.*;
import com.civicpulse.modules.auth.jwt.JwtTokenProvider;
import com.civicpulse.modules.auth.model.OtpChallenge;
import com.civicpulse.modules.auth.model.OtpPurpose;
import com.civicpulse.modules.auth.model.RefreshToken;
import com.civicpulse.modules.auth.repository.RefreshTokenRepository;
import com.civicpulse.modules.user.dto.UserDto;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.model.UserRole;
import com.civicpulse.modules.user.repository.UserRepository;
import com.civicpulse.ratelimit.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtConfig jwtConfig;
    private final RateLimiterService rateLimiterService;
    private final OtpService otpService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse register(RegisterRequest request, String clientIp) {
        rateLimiterService.checkAuthRateLimit(clientIp);

        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with this email address already exists.");
        }

        UserRole assignedRole = request.getRole() != null ? request.getRole() : UserRole.MEMBER;
        if (assignedRole == UserRole.ADMIN || assignedRole == UserRole.MODERATOR) {
            assignedRole = UserRole.MEMBER;
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(assignedRole)
                .phoneNumber(request.getPhoneNumber())
                .active(true)
                .emailVerified(false) // Account starts in pending unverified state
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered pending account for: [PROTECTED] with role: {}", savedUser.getRole());

        // Create 8-digit OTP verification challenge
        OtpChallenge challenge = otpService.createChallenge(savedUser, normalizedEmail, OtpPurpose.REGISTRATION_VERIFICATION, null);

        return AuthResponse.builder()
                .verificationRequired(true)
                .challengeId(challenge.getId())
                .email(normalizedEmail)
                .message("Account registered. Please enter the 8-digit verification code sent to your email to activate your account.")
                .user(UserDto.fromEntity(savedUser))
                .build();
    }

    @Transactional
    public AuthResponse verifyRegistration(VerifyOtpRequest request, String clientIp) {
        rateLimiterService.checkAuthRateLimit(clientIp);

        otpService.verifyOtp(
                request.getChallengeId(),
                request.getEmail(),
                request.getOtp(),
                OtpPurpose.REGISTRATION_VERIFICATION
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Account activated and email verified for: [PROTECTED]");
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp) {
        rateLimiterService.checkAuthRateLimit(clientIp);

        String normalizedEmail = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account has been suspended. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        // 1. Check if email verification is pending
        if (!user.isEmailVerified()) {
            OtpChallenge challenge = otpService.createChallenge(user, normalizedEmail, OtpPurpose.REGISTRATION_VERIFICATION, null);
            return AuthResponse.builder()
                    .verificationRequired(true)
                    .challengeId(challenge.getId())
                    .email(normalizedEmail)
                    .message("Your email address is not yet verified. A new 8-digit verification code has been sent.")
                    .build();
        }

        // 2. Check if Multi-Factor Authentication is required (forced for Admin or user-enabled)
        if (user.isMfaEnabled() || user.getRole() == UserRole.ADMIN) {
            OtpChallenge mfaChallenge = otpService.createChallenge(user, normalizedEmail, OtpPurpose.LOGIN_MFA, null);
            return AuthResponse.builder()
                    .mfaRequired(true)
                    .challengeId(mfaChallenge.getId())
                    .email(normalizedEmail)
                    .message("Multi-factor authentication required. Please enter the 8-digit code sent to your email/authenticator.")
                    .build();
        }

        log.info("User successfully logged in: [PROTECTED]");
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse verifyMfaLogin(MfaLoginVerifyRequest request, String clientIp) {
        rateLimiterService.checkAuthRateLimit(clientIp);

        OtpChallenge challenge = otpService.createChallenge(null, "temp", OtpPurpose.LOGIN_MFA, null);
        // Verify code
        otpService.verifyOtp(
                request.getChallengeId(),
                request.getCode(),
                request.getCode(),
                OtpPurpose.LOGIN_MFA
        );

        return generateAuthResponse(challenge.getUser());
    }

    @Transactional
    public AuthResponse verifyMfaLogin(UUID challengeId, String email, String code, String clientIp) {
        rateLimiterService.checkAuthRateLimit(clientIp);

        otpService.verifyOtp(
                challengeId,
                email,
                code,
                OtpPurpose.LOGIN_MFA
        );

        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UnauthorizedException("User session expired."));

        log.info("MFA verification successful for: [PROTECTED]");
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String rawToken = request.getRefreshToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token."));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            refreshTokenRepository.revokeAllUserTokens(refreshToken.getUser());
            throw new UnauthorizedException("Refresh token is expired or revoked. Please log in again.");
        }

        // Token rotation
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is inactive.");
        }

        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Transactional
    public MfaSetupResponse setupMfa(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String secret = generateSecureRandomToken().substring(0, 32);
        List<String> backupCodes = generateBackupCodes();

        user.setMfaSecret(secret);
        user.setMfaBackupCodes(String.join(",", backupCodes.stream().map(this::hashToken).collect(Collectors.toList())));
        userRepository.save(user);

        return MfaSetupResponse.builder()
                .secret(secret)
                .qrCodeUri("otpauth://totp/CivicPulse:" + user.getEmail() + "?secret=" + secret + "&issuer=CivicPulse")
                .backupCodes(backupCodes)
                .message("Scan the QR code or enter the secret in your authenticator app, then confirm with a verification code.")
                .build();
    }

    @Transactional
    public void enableMfa(UUID userId, MfaVerifyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is required to enable MFA.");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
        log.info("MFA enabled for user: [PROTECTED]");
    }

    @Transactional
    public void disableMfa(UUID userId, MfaVerifyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is required to disable MFA.");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaBackupCodes(null);
        userRepository.save(user);
        log.info("MFA disabled for user: [PROTECTED]");
    }

    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int part1 = 1000 + secureRandom.nextInt(9000);
            int part2 = 1000 + secureRandom.nextInt(9000);
            codes.add(part1 + "-" + part2);
        }
        return codes;
    }

    public AuthResponse generateAuthResponse(User user) {
        CustomUserDetails userDetails = CustomUserDetails.create(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getRole().name(),
                user.isActive()
        );

        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String rawRefreshToken = generateSecureRandomToken();
        String tokenHash = hashToken(rawRefreshToken);

        Instant expiryInstant = Instant.now().plusMillis(jwtConfig.getRefreshExpirationMs());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiryInstant)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpirationMs() / 1000)
                .user(UserDto.fromEntity(user))
                .build();
    }

    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
