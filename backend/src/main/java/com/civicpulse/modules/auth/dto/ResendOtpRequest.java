package com.civicpulse.modules.auth.dto;

import com.civicpulse.modules.auth.model.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResendOtpRequest {

    @NotNull(message = "Challenge ID is required")
    private UUID challengeId;

    @NotBlank(message = "Email is required")
    private String email;

    @NotNull(message = "OTP purpose is required")
    private OtpPurpose purpose;
}
