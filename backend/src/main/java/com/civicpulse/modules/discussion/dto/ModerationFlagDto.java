package com.civicpulse.modules.discussion.dto;

import com.civicpulse.modules.discussion.model.ModerationFlag;
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
public class ModerationFlagDto {

    private UUID id;
    private UUID postId;
    private String postContent;
    private UUID postAuthorUserId;
    private String postAuthorName;
    private UUID reportedByUserId;
    private String reportedByName;
    private String reason;
    private String status;
    private String resolutionNotes;
    private Instant createdAt;

    public static ModerationFlagDto fromEntity(ModerationFlag flag) {
        if (flag == null) return null;
        return ModerationFlagDto.builder()
                .id(flag.getId())
                .postId(flag.getPost().getId())
                .postContent(flag.getPost().getContent())
                .postAuthorUserId(flag.getPost().getUser().getId())
                .postAuthorName(flag.getPost().getUser().getFullName())
                .reportedByUserId(flag.getReportedBy().getId())
                .reportedByName(flag.getReportedBy().getFullName())
                .reason(flag.getReason())
                .status(flag.getStatus())
                .resolutionNotes(flag.getResolutionNotes())
                .createdAt(flag.getCreatedAt())
                .build();
    }
}
