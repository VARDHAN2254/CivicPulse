package com.civicpulse.modules.event.controller;

import com.civicpulse.common.response.ApiResponse;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.common.security.CustomUserDetails;
import com.civicpulse.modules.event.dto.ChangeStatusRequest;
import com.civicpulse.modules.event.dto.CreateEventRequest;
import com.civicpulse.modules.event.dto.EventDto;
import com.civicpulse.modules.event.dto.EventSearchCriteria;
import com.civicpulse.modules.event.dto.UpdateEventRequest;
import com.civicpulse.modules.event.model.EventStatus;
import com.civicpulse.modules.event.service.EventService;
import com.civicpulse.modules.event.service.EventSearchService;
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
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Endpoints for event creation, lifecycle, and detail retrieval")
public class EventController {

    private final EventService eventService;
    private final EventSearchService eventSearchService;

    @GetMapping
    @Operation(summary = "Search and discover upcoming public community events with multi-faceted filtering")
    public ResponseEntity<ApiResponse<PagedResponse<EventDto>>> discoverEvents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String locationType,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean availableOnly,
            @RequestParam(required = false, defaultValue = "startTime") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            @PageableDefault(size = 12) Pageable pageable
    ) {
        EventSearchCriteria criteria = EventSearchCriteria.builder()
                .query(query)
                .category(category)
                .organizationId(organizationId)
                .locationType(locationType != null ? com.civicpulse.modules.event.model.LocationType.valueOf(locationType.toUpperCase()) : null)
                .city(city)
                .availableOnly(availableOnly)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PagedResponse<EventDto> events = eventSearchService.searchEvents(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/{slugOrId}")
    @Operation(summary = "Get event details by slug or UUID")
    public ResponseEntity<ApiResponse<EventDto>> getEvent(@PathVariable String slugOrId) {
        EventDto event = eventService.getEventBySlugOrId(slugOrId);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @GetMapping("/manage")
    @Operation(summary = "List all events manageable by the authenticated organizer")
    public ResponseEntity<ApiResponse<PagedResponse<EventDto>>> getManageableEvents(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 12, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<EventDto> events = eventService.getManageableEvents(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/organization/{orgId}")
    @Operation(summary = "Get all events hosted by a specific organization")
    public ResponseEntity<ApiResponse<PagedResponse<EventDto>>> getOrganizationEvents(
            @PathVariable UUID orgId,
            @PageableDefault(size = 12, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<EventDto> events = eventService.getOrganizationEvents(orgId, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @PostMapping
    @PreAuthorize("@orgPermissionEvaluator.canManageOrganization(authentication, #request.organizationId)")
    @Operation(summary = "Create a new event in an organization")
    public ResponseEntity<ApiResponse<EventDto>> createEvent(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody CreateEventRequest request
    ) {
        EventDto event = eventService.createEvent(currentUser.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Event created successfully", event), HttpStatus.CREATED);
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("@orgPermissionEvaluator.canManageEvent(authentication, #eventId)")
    @Operation(summary = "Update event details (Requires organizer role)")
    public ResponseEntity<ApiResponse<EventDto>> updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        EventDto event = eventService.updateEvent(eventId, request);
        return ResponseEntity.ok(ApiResponse.success("Event updated successfully", event));
    }

    @PatchMapping("/{eventId}/status")
    @PreAuthorize("@orgPermissionEvaluator.canManageEvent(authentication, #eventId)")
    @Operation(summary = "Change event lifecycle state (PUBLISH, CLOSE REGISTRATION, CANCEL)")
    public ResponseEntity<ApiResponse<EventDto>> changeStatus(
            @PathVariable UUID eventId,
            @Valid @RequestBody ChangeStatusRequest request
    ) {
        EventDto event = eventService.changeStatus(eventId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Event status updated to " + request.getStatus(), event));
    }

    @PostMapping("/{eventId}/publish")
    @PreAuthorize("@orgPermissionEvaluator.canManageEvent(authentication, #eventId)")
    @Operation(summary = "Publish draft event and open registrations")
    public ResponseEntity<ApiResponse<EventDto>> publishEvent(@PathVariable UUID eventId) {
        EventDto event = eventService.changeStatus(eventId, EventStatus.PUBLISHED);
        return ResponseEntity.ok(ApiResponse.success("Event published successfully", event));
    }

    @PostMapping("/{eventId}/cancel")
    @PreAuthorize("@orgPermissionEvaluator.canManageEvent(authentication, #eventId)")
    @Operation(summary = "Cancel event")
    public ResponseEntity<ApiResponse<EventDto>> cancelEvent(@PathVariable UUID eventId) {
        EventDto event = eventService.changeStatus(eventId, EventStatus.CANCELLED);
        return ResponseEntity.ok(ApiResponse.success("Event has been cancelled", event));
    }
}
