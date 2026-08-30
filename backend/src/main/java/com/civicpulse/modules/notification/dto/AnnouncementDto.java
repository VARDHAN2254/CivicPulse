package com.civicpulse.modules.notification.dto;

import com.civicpulse.modules.notification.model.Announcement;
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
public class AnnouncementDto {

    private UUID id;
    private UUID eventId;
    private UUID createdByUserId;
    private String createdByUserName;
    private String title;
    private String message;
    private Instant createdAt;

    public static AnnouncementDto fromEntity(Announcement ann) {
        if (ann == null) return null;
        return AnnouncementDto.builder()
                .id(ann.getId())
                .eventId(ann.getEvent().getId())
                .createdByUserId(ann.getCreatedBy().getId())
                .createdByUserName(ann.getCreatedBy().getFullName())
                .title(ann.getTitle())
                .message(ann.getMessage())
                .createdAt(ann.getCreatedAt())
                .build();
    }
}
