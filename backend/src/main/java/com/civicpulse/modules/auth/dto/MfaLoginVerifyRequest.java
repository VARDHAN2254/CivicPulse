package com.civicpulse.modules.auth.dto;

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
public class MfaLoginVerifyRequest {

    @NotNull(message = "Challenge ID is required")
    private UUID challengeId;

    @NotBlank(message = "MFA code is required")
    private String code;
}
