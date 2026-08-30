package com.civicpulse.modules.discussion.controller;

import com.civicpulse.common.response.ApiResponse;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.common.security.CustomUserDetails;
import com.civicpulse.modules.discussion.dto.ModerationFlagDto;
import com.civicpulse.modules.discussion.dto.ResolveFlagRequest;
import com.civicpulse.modules.discussion.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@Tag(name = "Moderation Console", description = "Endpoints for platform moderators and admins to triage content flags")
public class ModerationController {

    private final ModerationService moderationService;

    @GetMapping("/flags")
    @Operation(summary = "Get pending reported content flags (Moderator/Admin only)")
    public ResponseEntity<ApiResponse<PagedResponse<ModerationFlagDto>>> getPendingFlags(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<ModerationFlagDto> flags = moderationService.getPendingFlags(pageable);
        return ResponseEntity.ok(ApiResponse.success(flags));
    }

    @PostMapping("/flags/{flagId}/resolve")
    @Operation(summary = "Resolve moderation flag (DISMISS or REMOVE_POST)")
    public ResponseEntity<ApiResponse<Void>> resolveFlag(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID flagId,
            @Valid @RequestBody ResolveFlagRequest request
    ) {
        moderationService.resolveFlag(currentUser.getId(), flagId, request);
        return ResponseEntity.ok(ApiResponse.message("Flag resolved successfully as " + request.getAction()));
    }
}
