package com.civicpulse.modules.auth.dto;

import com.civicpulse.modules.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserDto user;

    // Security & Step-Up Fields
    private boolean verificationRequired;
    private boolean mfaRequired;
    private UUID challengeId;
    private String email;
    private String message;
}
