package com.civicpulse.modules.discussion.controller;

import com.civicpulse.common.response.ApiResponse;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.common.security.CustomUserDetails;
import com.civicpulse.modules.discussion.dto.CreatePostRequest;
import com.civicpulse.modules.discussion.dto.DiscussionPostDto;
import com.civicpulse.modules.discussion.dto.ReportPostRequest;
import com.civicpulse.modules.discussion.service.DiscussionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/discussions")
@RequiredArgsConstructor
@Tag(name = "Community Discussions", description = "Endpoints for community discussion threads and nested replies")
public class DiscussionController {

    private final DiscussionService discussionService;

    @GetMapping
    @Operation(summary = "Get discussion board posts for an event with nested replies")
    public ResponseEntity<ApiResponse<PagedResponse<DiscussionPostDto>>> getDiscussion(
            @PathVariable UUID eventId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<DiscussionPostDto> posts = discussionService.getEventDiscussion(eventId, pageable);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @PostMapping
    @Operation(summary = "Post a comment or reply on event discussion board")
    public ResponseEntity<ApiResponse<DiscussionPostDto>> createPost(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID eventId,
            @Valid @RequestBody CreatePostRequest request
    ) {
        DiscussionPostDto post = discussionService.createPost(currentUser.getId(), eventId, request);
        return new ResponseEntity<>(ApiResponse.success("Post published", post), HttpStatus.CREATED);
    }

    @PatchMapping("/{postId}/pin")
    @PreAuthorize("@orgPermissionEvaluator.canManageEvent(authentication, #eventId)")
    @Operation(summary = "Pin/unpin discussion post (Organizer only)")
    public ResponseEntity<ApiResponse<DiscussionPostDto>> togglePin(
            @PathVariable UUID eventId,
            @PathVariable UUID postId
    ) {
        DiscussionPostDto post = discussionService.togglePin(postId);
        return ResponseEntity.ok(ApiResponse.success("Pin state updated", post));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete discussion post")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID eventId,
            @PathVariable UUID postId
    ) {
        discussionService.deletePost(currentUser.getId(), postId);
        return ResponseEntity.ok(ApiResponse.message("Post deleted"));
    }

    @PostMapping("/{postId}/report")
    @Operation(summary = "Report a post for moderator review")
    public ResponseEntity<ApiResponse<Void>> reportPost(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID eventId,
            @PathVariable UUID postId,
            @Valid @RequestBody ReportPostRequest request
    ) {
        discussionService.reportPost(currentUser.getId(), postId, request);
        return ResponseEntity.ok(ApiResponse.message("Report submitted to moderation team"));
    }
}
