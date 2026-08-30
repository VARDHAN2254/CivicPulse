package com.civicpulse.modules.auth.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.modules.auth.dto.OtpChallengeResponse;
import com.civicpulse.modules.auth.dto.PasswordResetRequest;
import com.civicpulse.modules.auth.dto.PasswordResetWithOtpRequest;
import com.civicpulse.modules.auth.dto.VerifyOtpRequest;
import com.civicpulse.modules.auth.dto.VerifyOtpResponse;
import com.civicpulse.modules.auth.model.OtpChallenge;
import com.civicpulse.modules.auth.model.OtpPurpose;
import com.civicpulse.modules.auth.repository.RefreshTokenRepository;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.repository.UserRepository;
import com.civicpulse.ratelimit.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiterService;
    private final OtpService otpService;

    @Transactional
    public OtpChallengeResponse requestPasswordReset(PasswordResetRequest request, String clientIp) {
        rateLimiterService.checkAuthRateLimit(clientIp);

        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Constant-time dummy work to prevent account enumeration via timing differences
            otpService.hashToken("dummy-salt-" + email + "-" + System.currentTimeMillis());
            log.info("Password reset requested for nonexistent email: [PROTECTED]");
            return OtpChallengeResponse.builder()
                    .challengeId(UUID.randomUUID())
                    .email(email)
                    .purpose(OtpPurpose.PASSWORD_RESET)
                    .expiresInSeconds(OtpService.OTP_TTL_SECONDS)
                    .cooldownSeconds(OtpService.COOLDOWN_SECONDS)
                    .resendsRemaining(OtpService.MAX_RESENDS)
                    .build();
        }

        OtpChallenge challenge = otpService.createChallenge(user, email, OtpPurpose.PASSWORD_RESET, null);
        return otpService.toResponse(challenge);
    }

    @Transactional
    public VerifyOtpResponse verifyResetOtp(VerifyOtpRequest request, String clientIp) {
        rateLimiterService.checkAuthRateLimit(clientIp);
        return otpService.verifyOtp(
                request.getChallengeId(),
                request.getEmail(),
                request.getOtp(),
                OtpPurpose.PASSWORD_RESET
        );
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetWithOtpRequest request, String clientIp) {
        rateLimiterService.checkAuthRateLimit(clientIp);

        OtpChallenge challenge = otpService.consumeResetAuthToken(request.getResetAuthToken());
        User user = challenge.getUser();

        if (user == null) {
            user = userRepository.findByEmail(challenge.getEmail())
                    .orElseThrow(() -> new BadRequestException("User account associated with this challenge no longer exists."));
        }

        // Enforce password policy
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters in length.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Security requirement: Revoke all active refresh sessions upon password reset
        refreshTokenRepository.revokeAllUserTokens(user);

        log.info("Password successfully reset and all existing sessions revoked for user: [PROTECTED]");
    }
}
