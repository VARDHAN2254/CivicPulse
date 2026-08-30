package com.civicpulse.modules.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCheckInRequest {

    @NotBlank(message = "QR Token is required")
    private String qrToken;

    private String deviceInfo;
}
