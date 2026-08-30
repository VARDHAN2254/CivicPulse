package com.civicpulse.modules.notification.controller;

import com.civicpulse.common.response.ApiResponse;
import com.civicpulse.common.security.CustomUserDetails;
import com.civicpulse.modules.notification.dto.AnnouncementDto;
import com.civicpulse.modules.notification.dto.CreateAnnouncementRequest;
import com.civicpulse.modules.notification.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/announcements")
@RequiredArgsConstructor
@Tag(name = "Event Announcements", description = "Endpoints for organizer live announcements and broadcasts")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    @Operation(summary = "Get all announcements posted for an event")
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> getEventAnnouncements(
            @PathVariable UUID eventId
    ) {
        List<AnnouncementDto> announcements = announcementService.getEventAnnouncements(eventId);
        return ResponseEntity.ok(ApiResponse.success(announcements));
    }

    @PostMapping
    @PreAuthorize("@orgPermissionEvaluator.canManageEvent(authentication, #eventId)")
    @Operation(summary = "Post an urgent organizer announcement (Broadcasts via WebSocket and Notifications)")
    public ResponseEntity<ApiResponse<AnnouncementDto>> createAnnouncement(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateAnnouncementRequest request
    ) {
        AnnouncementDto announcement = announcementService.createAnnouncement(currentUser.getId(), eventId, request);
        return new ResponseEntity<>(ApiResponse.success("Announcement broadcasted successfully", announcement), HttpStatus.CREATED);
    }
}
