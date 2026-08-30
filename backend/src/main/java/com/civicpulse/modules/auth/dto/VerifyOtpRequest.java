package com.civicpulse.modules.auth.dto;

import com.civicpulse.modules.auth.model.OtpPurpose;
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
public class VerifyOtpRequest {

    @NotNull(message = "Challenge ID is required")
    private UUID challengeId;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^[0-9]{8}$", message = "OTP code must be exactly 8 numeric digits")
    private String otp;

    @NotNull(message = "OTP purpose is required")
    private OtpPurpose purpose;
}
