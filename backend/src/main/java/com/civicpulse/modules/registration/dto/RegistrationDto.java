package com.civicpulse.modules.registration.dto;

import com.civicpulse.modules.registration.model.EventRegistration;
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
public class RegistrationDto {

    private UUID id;
    private String ticketCode;
    private UUID eventId;
    private String eventTitle;
    private String eventSlug;
    private Instant startTime;
    private Instant endTime;
    private String venueName;
    private String organizationName;
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private RegistrationStatus status;
    private Instant registeredAt;
    private Instant cancelledAt;

    public static RegistrationDto fromEntity(EventRegistration reg) {
        if (reg == null) return null;
        return RegistrationDto.builder()
                .id(reg.getId())
                .ticketCode(reg.getTicketCode())
                .eventId(reg.getEvent().getId())
                .eventTitle(reg.getEvent().getTitle())
                .eventSlug(reg.getEvent().getSlug())
                .startTime(reg.getEvent().getStartTime())
                .endTime(reg.getEvent().getEndTime())
                .venueName(reg.getEvent().getVenueName() != null ? reg.getEvent().getVenueName() : reg.getEvent().getCity())
                .organizationName(reg.getEvent().getOrganization().getName())
                .userId(reg.getUser().getId())
                .userEmail(reg.getUser().getEmail())
                .userFullName(reg.getUser().getFullName())
                .status(reg.getStatus())
                .registeredAt(reg.getRegisteredAt())
                .cancelledAt(reg.getCancelledAt())
                .build();
    }
}
