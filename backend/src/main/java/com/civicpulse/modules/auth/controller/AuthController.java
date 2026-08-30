package com.civicpulse.modules.auth.controller;

import com.civicpulse.common.response.ApiResponse;
import com.civicpulse.common.security.CustomUserDetails;
import com.civicpulse.modules.auth.dto.*;
import com.civicpulse.modules.auth.model.OtpChallenge;
import com.civicpulse.modules.auth.service.AuthService;
import com.civicpulse.modules.auth.service.OtpService;
import com.civicpulse.modules.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, token refresh, OTP verification, MFA, and account recovery")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final OtpService otpService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account (issues OTP email verification challenge)")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        AuthResponse response = authService.register(request, clientIp);
        return new ResponseEntity<>(ApiResponse.success("Account created. Please enter the 8-digit verification code sent to your email.", response), HttpStatus.CREATED);
    }

    @PostMapping("/verify-registration")
    @Operation(summary = "Verify account email using 8-digit OTP code")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyRegistration(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        AuthResponse response = authService.verifyRegistration(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Account verified successfully.", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT or MFA challenge")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        AuthResponse response = authService.login(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Login request processed.", response));
    }

    @PostMapping("/mfa/verify-login")
    @Operation(summary = "Complete MFA challenge with 8-digit code to obtain JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyMfaLogin(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        AuthResponse response = authService.verifyMfaLogin(request.getChallengeId(), request.getEmail(), request.getOtp(), clientIp);
        return ResponseEntity.ok(ApiResponse.success("MFA authentication successful.", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully.", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token and terminate active session")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) TokenRefreshRequest request) {
        if (request != null) {
            authService.logout(request.getRefreshToken());
        }
        return ResponseEntity.ok(ApiResponse.message("Logged out successfully."));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset OTP code (Constant-time enumeration defense)")
    public ResponseEntity<ApiResponse<OtpChallengeResponse>> forgotPassword(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        OtpChallengeResponse response = passwordResetService.requestPasswordReset(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("If an account with that email exists, verification instructions have been generated.", response));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify single-use 8-digit OTP code")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        VerifyOtpResponse response = passwordResetService.verifyResetOtp(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Verification code confirmed.", response));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Confirm password reset using single-use resetAuthToken")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetWithOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        passwordResetService.confirmPasswordReset(request, clientIp);
        return ResponseEntity.ok(ApiResponse.message("Password has been successfully reset. Please log in with your new password."));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend a new 8-digit OTP with cooldown protection")
    public ResponseEntity<ApiResponse<OtpChallengeResponse>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request
    ) {
        OtpChallenge newChallenge = otpService.resendOtp(request.getChallengeId(), request.getEmail(), request.getPurpose());
        return ResponseEntity.ok(ApiResponse.success("A new 8-digit verification code has been dispatched.", otpService.toResponse(newChallenge)));
    }

    @PostMapping("/mfa/setup")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Initialize TOTP Multi-Factor Authentication setup")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> setupMfa(@AuthenticationPrincipal CustomUserDetails currentUser) {
        MfaSetupResponse response = authService.setupMfa(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/mfa/enable")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Enable MFA with step-up password verification")
    public ResponseEntity<ApiResponse<Void>> enableMfa(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody MfaVerifyRequest request
    ) {
        authService.enableMfa(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.message("Multi-factor authentication has been enabled."));
    }

    @PostMapping("/mfa/disable")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Disable MFA with step-up password verification")
    public ResponseEntity<ApiResponse<Void>> disableMfa(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody MfaVerifyRequest request
    ) {
        authService.disableMfa(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.message("Multi-factor authentication has been disabled."));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
