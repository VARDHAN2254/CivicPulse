package com.civicpulse.modules.user.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ConflictException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.modules.auth.dto.OtpChallengeResponse;
import com.civicpulse.modules.auth.model.OtpChallenge;
import com.civicpulse.modules.auth.model.OtpPurpose;
import com.civicpulse.modules.auth.repository.RefreshTokenRepository;
import com.civicpulse.modules.auth.service.OtpService;
import com.civicpulse.modules.user.dto.*;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Transactional(readOnly = true)
    public UserDto getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setBio(request.getBio());
        user.setAvatarUrl(request.getAvatarUrl());

        User updatedUser = userRepository.save(user);
        log.info("Updated profile for user: [PROTECTED]");
        return UserDto.fromEntity(updatedUser);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all existing refresh tokens
        refreshTokenRepository.revokeAllUserTokens(user);
        log.info("Password changed and sessions revoked for user: [PROTECTED]");
    }

    @Transactional
    public OtpChallengeResponse requestEmailChange(UUID userId, EmailChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is required to change email address.");
        }

        String newEmail = request.getNewEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("The target email address is already in use.");
        }

        // Issue OTP to current email with target new email stored in metadata
        OtpChallenge challenge = otpService.createChallenge(user, user.getEmail(), OtpPurpose.EMAIL_CHANGE, newEmail);
        return otpService.toResponse(challenge);
    }

    @Transactional
    public void confirmEmailChange(UUID userId, ConfirmEmailChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var verifyResponse = otpService.verifyOtp(request.getChallengeId(), user.getEmail(), request.getOtp(), OtpPurpose.EMAIL_CHANGE);
        OtpChallenge challenge = otpService.consumeResetAuthToken(verifyResponse.getResetAuthToken());

        String newEmail = challenge.getMetadata();
        if (newEmail == null || newEmail.isBlank()) {
            throw new BadRequestException("Email change session invalid.");
        }

        user.setEmail(newEmail);
        user.setEmailVerified(true);
        userRepository.save(user);

        refreshTokenRepository.revokeAllUserTokens(user);
        log.info("Email address updated and sessions rotated for user: [PROTECTED]");
    }
}
