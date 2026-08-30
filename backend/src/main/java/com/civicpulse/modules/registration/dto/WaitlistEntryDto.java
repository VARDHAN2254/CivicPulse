package com.civicpulse.modules.registration.dto;

import com.civicpulse.modules.registration.model.WaitlistEntry;
import com.civicpulse.modules.registration.model.WaitlistStatus;
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
public class WaitlistEntryDto {

    private UUID id;
    private UUID eventId;
    private String eventTitle;
    private String eventSlug;
    private Instant startTime;
    private String organizationName;
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private int position;
    private WaitlistStatus status;
    private Instant joinedAt;
    private Instant promotedAt;

    public static WaitlistEntryDto fromEntity(WaitlistEntry entry) {
        if (entry == null) return null;
        return WaitlistEntryDto.builder()
                .id(entry.getId())
                .eventId(entry.getEvent().getId())
                .eventTitle(entry.getEvent().getTitle())
                .eventSlug(entry.getEvent().getSlug())
                .startTime(entry.getEvent().getStartTime())
                .organizationName(entry.getEvent().getOrganization().getName())
                .userId(entry.getUser().getId())
                .userEmail(entry.getUser().getEmail())
                .userFullName(entry.getUser().getFullName())
                .position(entry.getPosition())
                .status(entry.getStatus())
                .joinedAt(entry.getJoinedAt())
                .promotedAt(entry.getPromotedAt())
                .build();
    }
}
