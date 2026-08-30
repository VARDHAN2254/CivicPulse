package com.civicpulse.modules.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrTokenDto {

    private UUID registrationId;
    private String token;
    private long expiresInSeconds;
    private Instant expiresAt;
}
