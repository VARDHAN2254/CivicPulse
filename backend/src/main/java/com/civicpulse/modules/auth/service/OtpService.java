package com.civicpulse.modules.auth.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.modules.auth.dto.OtpChallengeResponse;
import com.civicpulse.modules.auth.dto.VerifyOtpResponse;
import com.civicpulse.modules.auth.model.OtpChallenge;
import com.civicpulse.modules.auth.model.OtpPurpose;
import com.civicpulse.modules.auth.repository.OtpChallengeRepository;
import com.civicpulse.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpChallengeRepository otpChallengeRepository;
    private final OtpDeliveryService otpDeliveryService;
    private final SecureRandom secureRandom = new SecureRandom();

    public static final long OTP_TTL_SECONDS = 300L; // 5 minutes
    public static final long COOLDOWN_SECONDS = 60L; // 1 minute
    public static final int MAX_ATTEMPTS = 5;
    public static final int MAX_RESENDS = 3;

    @Transactional
    public OtpChallenge createChallenge(User user, String email, OtpPurpose purpose, String metadata) {
        String normalizedEmail = email.toLowerCase().trim();

        // 1. Invalidate any existing active challenges for this email and purpose
        otpChallengeRepository.invalidateActiveChallenges(normalizedEmail, purpose, Instant.now());

        // 2. Generate cryptographically secure 8-digit numeric OTP (10000000 - 99999999)
        int codeInt = 10000000 + secureRandom.nextInt(90000000);
        String rawOtp = String.valueOf(codeInt);
        String otpHash = hashToken(rawOtp);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(OTP_TTL_SECONDS, ChronoUnit.SECONDS);
        Instant cooldownUntil = now.plus(COOLDOWN_SECONDS, ChronoUnit.SECONDS);

        OtpChallenge challenge = OtpChallenge.builder()
                .user(user)
                .email(normalizedEmail)
                .purpose(purpose)
                .otpHash(otpHash)
                .attempts(0)
                .maxAttempts(MAX_ATTEMPTS)
                .resendCount(0)
                .maxResends(MAX_RESENDS)
                .cooldownUntil(cooldownUntil)
                .expiresAt(expiresAt)
                .metadata(metadata)
                .build();

        OtpChallenge savedChallenge = otpChallengeRepository.save(challenge);

        // 3. Deliver OTP securely to user's registered delivery channel
        otpDeliveryService.deliverOtp(normalizedEmail, purpose, rawOtp, OTP_TTL_SECONDS);

        log.info("Created OTP challenge [{}] for purpose {} with TTL {}s", savedChallenge.getId(), purpose, OTP_TTL_SECONDS);
        return savedChallenge;
    }

    @Transactional
    public OtpChallenge resendOtp(UUID challengeId, String email, OtpPurpose purpose) {
        String normalizedEmail = email.toLowerCase().trim();

        OtpChallenge currentChallenge = otpChallengeRepository.findActiveChallenge(challengeId, normalizedEmail, purpose)
                .orElseThrow(() -> new BadRequestException("No active verification challenge found. Please start a new request."));

        if (currentChallenge.isInCooldown()) {
            long remainingCooldown = Duration.between(Instant.now(), currentChallenge.getCooldownUntil()).getSeconds();
            throw new BadRequestException("Please wait " + Math.max(1, remainingCooldown) + " seconds before requesting a new code.");
        }

        if (currentChallenge.getResendCount() >= currentChallenge.getMaxResends()) {
            currentChallenge.setInvalidatedAt(Instant.now());
            otpChallengeRepository.save(currentChallenge);
            throw new BadRequestException("Maximum resend attempts reached. Please start a new request.");
        }

        int newResendCount = currentChallenge.getResendCount() + 1;

        // Invalidate previous challenge
        currentChallenge.setInvalidatedAt(Instant.now());
        otpChallengeRepository.save(currentChallenge);

        // Generate new 8-digit OTP
        int codeInt = 10000000 + secureRandom.nextInt(90000000);
        String rawOtp = String.valueOf(codeInt);
        String otpHash = hashToken(rawOtp);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(OTP_TTL_SECONDS, ChronoUnit.SECONDS);
        Instant cooldownUntil = now.plus(COOLDOWN_SECONDS, ChronoUnit.SECONDS);

        OtpChallenge newChallenge = OtpChallenge.builder()
                .user(currentChallenge.getUser())
                .email(normalizedEmail)
                .purpose(purpose)
                .otpHash(otpHash)
                .attempts(0)
                .maxAttempts(MAX_ATTEMPTS)
                .resendCount(newResendCount)
                .maxResends(MAX_RESENDS)
                .cooldownUntil(cooldownUntil)
                .expiresAt(expiresAt)
                .metadata(currentChallenge.getMetadata())
                .build();

        OtpChallenge saved = otpChallengeRepository.save(newChallenge);
        otpDeliveryService.deliverOtp(normalizedEmail, purpose, rawOtp, OTP_TTL_SECONDS);

        log.info("Resent OTP for challenge [{}] (Resend #{}/{})", saved.getId(), newResendCount, MAX_RESENDS);
        return saved;
    }

    @Transactional
    public VerifyOtpResponse verifyOtp(UUID challengeId, String email, String rawOtp, OtpPurpose purpose) {
        String normalizedEmail = email.toLowerCase().trim();

        OtpChallenge challenge = otpChallengeRepository.findActiveChallenge(challengeId, normalizedEmail, purpose)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification session. Please request a new code."));

        if (challenge.isExpired()) {
            challenge.setInvalidatedAt(Instant.now());
            otpChallengeRepository.save(challenge);
            throw new BadRequestException("Verification code has expired. Please request a new code.");
        }

        if (challenge.isInvalidated()) {
            throw new BadRequestException("This verification challenge has been invalidated due to excessive attempts. Please request a new code.");
        }

        String providedHash = hashToken(rawOtp.trim());

        if (!MessageDigest.isEqual(providedHash.getBytes(StandardCharsets.UTF_8), challenge.getOtpHash().getBytes(StandardCharsets.UTF_8))) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            if (challenge.getAttempts() >= challenge.getMaxAttempts()) {
                challenge.setInvalidatedAt(Instant.now());
                otpChallengeRepository.save(challenge);
                log.warn("Challenge [{}] invalidated: exceeded maximum attempts", challenge.getId());
                throw new BadRequestException("Maximum verification attempts exceeded. Verification challenge invalidated.");
            }
            otpChallengeRepository.save(challenge);
            int remaining = challenge.getMaxAttempts() - challenge.getAttempts();
            throw new BadRequestException("Invalid verification code. " + remaining + " attempts remaining.");
        }

        // OTP Verified successfully!
        challenge.setVerifiedAt(Instant.now());

        // Generate single-use resetAuthToken if purpose requires secondary action
        String rawResetToken = generateSecureRandomToken();
        challenge.setResetAuthTokenHash(hashToken(rawResetToken));
        challenge.setResetAuthTokenExpiresAt(Instant.now().plus(OTP_TTL_SECONDS, ChronoUnit.SECONDS));

        otpChallengeRepository.save(challenge);
        log.info("Successfully verified OTP challenge [{}] for purpose {}", challenge.getId(), purpose);

        return VerifyOtpResponse.builder()
                .verified(true)
                .resetAuthToken(rawResetToken)
                .message("Verification code confirmed successfully.")
                .build();
    }

    @Transactional
    public OtpChallenge consumeResetAuthToken(String rawResetAuthToken) {
        if (rawResetAuthToken == null || rawResetAuthToken.isBlank()) {
            throw new BadRequestException("Reset authorization token is missing.");
        }

        String tokenHash = hashToken(rawResetAuthToken.trim());
        OtpChallenge challenge = otpChallengeRepository.findActiveByResetAuthTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset authorization token."));

        if (challenge.isResetAuthTokenExpired()) {
            challenge.setInvalidatedAt(Instant.now());
            otpChallengeRepository.save(challenge);
            throw new BadRequestException("Reset authorization token has expired. Please request a new verification code.");
        }

        if (challenge.isUsed()) {
            throw new BadRequestException("This reset authorization has already been consumed.");
        }

        challenge.setUsedAt(Instant.now());
        return otpChallengeRepository.save(challenge);
    }

    public OtpChallengeResponse toResponse(OtpChallenge challenge) {
        long remainingTtl = Duration.between(Instant.now(), challenge.getExpiresAt()).getSeconds();
        long remainingCooldown = challenge.isInCooldown()
                ? Duration.between(Instant.now(), challenge.getCooldownUntil()).getSeconds()
                : 0L;

        return OtpChallengeResponse.builder()
                .challengeId(challenge.getId())
                .email(challenge.getEmail())
                .purpose(challenge.getPurpose())
                .expiresInSeconds(Math.max(0, remainingTtl))
                .cooldownSeconds(Math.max(0, remainingCooldown))
                .resendsRemaining(challenge.getMaxResends() - challenge.getResendCount())
                .build();
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

    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
