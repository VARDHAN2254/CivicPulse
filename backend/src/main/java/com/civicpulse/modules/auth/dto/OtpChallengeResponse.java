package com.civicpulse.modules.auth.dto;

import com.civicpulse.modules.auth.model.OtpPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpChallengeResponse {
    private UUID challengeId;
    private String email;
    private OtpPurpose purpose;
    private long expiresInSeconds;
    private long cooldownSeconds;
    private int resendsRemaining;
    private boolean mfaRequired;
    private boolean verificationRequired;
}
