package com.civicpulse.modules.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {

    private boolean confirmed;
    private boolean waitlisted;
    private Integer waitlistPosition;
    private String ticketCode;
    private String message;
    private RegistrationDto registration;
    private WaitlistEntryDto waitlistEntry;
}
