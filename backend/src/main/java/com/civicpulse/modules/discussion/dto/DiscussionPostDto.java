package com.civicpulse.modules.discussion.dto;

import com.civicpulse.modules.discussion.model.DiscussionPost;
import com.civicpulse.modules.discussion.model.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionPostDto {

    private UUID id;
    private UUID eventId;
    private UUID parentPostId;
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private String userRole;
    private String content;
    private boolean pinned;
    private PostStatus status;
    @Builder.Default
    private List<DiscussionPostDto> replies = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public static DiscussionPostDto fromEntity(DiscussionPost post) {
        if (post == null) return null;
        return DiscussionPostDto.builder()
                .id(post.getId())
                .eventId(post.getEvent().getId())
                .parentPostId(post.getParentPost() != null ? post.getParentPost().getId() : null)
                .userId(post.getUser().getId())
                .userFullName(post.getUser().getFullName())
                .userEmail(post.getUser().getEmail())
                .userRole(post.getUser().getRole().name())
                .content(post.getContent())
                .pinned(post.isPinned())
                .status(post.getStatus())
                .replies(post.getReplies() != null ? post.getReplies().stream().map(DiscussionPostDto::fromEntity).collect(Collectors.toList()) : new ArrayList<>())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
