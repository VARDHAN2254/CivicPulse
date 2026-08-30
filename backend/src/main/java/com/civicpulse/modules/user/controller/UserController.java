package com.civicpulse.modules.user.controller;

import com.civicpulse.common.response.ApiResponse;
import com.civicpulse.common.security.CustomUserDetails;
import com.civicpulse.modules.user.dto.ChangePasswordRequest;
import com.civicpulse.modules.user.dto.UpdateProfileRequest;
import com.civicpulse.modules.user.dto.UserDto;
import com.civicpulse.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and account management")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails currentUser) {
        UserDto userDto = userService.getUserProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(userDto));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile details")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserDto updatedUser = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedUser));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change account password with current password reauthentication")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.message("Password updated successfully"));
    }

    @PostMapping("/request-email-change")
    @Operation(summary = "Initiate email address change with step-up verification")
    public ResponseEntity<ApiResponse<com.civicpulse.modules.auth.dto.OtpChallengeResponse>> requestEmailChange(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody com.civicpulse.modules.user.dto.EmailChangeRequest request
    ) {
        var challenge = userService.requestEmailChange(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Verification code sent to your current registered email.", challenge));
    }

    @PostMapping("/confirm-email-change")
    @Operation(summary = "Confirm email address change using 8-digit OTP code")
    public ResponseEntity<ApiResponse<Void>> confirmEmailChange(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody com.civicpulse.modules.user.dto.ConfirmEmailChangeRequest request
    ) {
        userService.confirmEmailChange(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.message("Email address successfully changed. Please log in with your new email."));
    }
}
