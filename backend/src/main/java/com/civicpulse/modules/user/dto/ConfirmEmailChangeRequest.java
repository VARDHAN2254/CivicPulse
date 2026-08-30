package com.civicpulse.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmEmailChangeRequest {

    @NotNull(message = "Challenge ID is required")
    private UUID challengeId;

    @NotBlank(message = "OTP verification code is required")
    @Pattern(regexp = "^[0-9]{8}$", message = "OTP code must be exactly 8 numeric digits")
    private String otp;
}
