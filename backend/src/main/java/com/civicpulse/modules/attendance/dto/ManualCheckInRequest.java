package com.civicpulse.modules.attendance.dto;

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
public class ManualCheckInRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotBlank(message = "Ticket code is required")
    private String ticketCode;

    private String deviceInfo;
}
