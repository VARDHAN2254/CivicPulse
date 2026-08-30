package com.civicpulse.modules.notification.dto;

import com.civicpulse.modules.notification.model.Notification;
import com.civicpulse.modules.notification.model.NotificationType;
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
public class NotificationDto {

    private UUID id;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String message;
    private String linkUrl;
    private boolean read;
    private Instant readAt;
    private Instant createdAt;

    public static NotificationDto fromEntity(Notification notif) {
        if (notif == null) return null;
        return NotificationDto.builder()
                .id(notif.getId())
                .userId(notif.getUser().getId())
                .type(notif.getType())
                .title(notif.getTitle())
                .message(notif.getMessage())
                .linkUrl(notif.getLinkUrl())
                .read(notif.isRead())
                .readAt(notif.getReadAt())
                .createdAt(notif.getCreatedAt())
                .build();
    }
}
