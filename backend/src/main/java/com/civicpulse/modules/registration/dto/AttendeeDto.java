package com.civicpulse.modules.registration.dto;

import com.civicpulse.modules.registration.model.RegistrationStatus;
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
public class AttendeeDto {

    private UUID registrationId;
    private UUID userId;
    private String fullName;
    private String email;
    private String ticketCode;
    private RegistrationStatus status;
    private Instant registeredAt;
}
