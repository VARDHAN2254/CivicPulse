package com.civicpulse.modules.organization.controller;

import com.civicpulse.common.response.ApiResponse;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.common.security.CustomUserDetails;
import com.civicpulse.modules.organization.dto.CreateOrganizationRequest;
import com.civicpulse.modules.organization.dto.OrganizationDto;
import com.civicpulse.modules.organization.dto.UpdateOrganizationRequest;
import com.civicpulse.modules.organization.service.OrganizationService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Endpoints for community organizations and chapters")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @Operation(summary = "Get public directory of community organizations")
    public ResponseEntity<ApiResponse<PagedResponse<OrganizationDto>>> getPublicOrganizations(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 12, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PagedResponse<OrganizationDto> orgs = organizationService.getPublicOrganizations(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(orgs));
    }

    @GetMapping("/{slugOrId}")
    @Operation(summary = "Get organization details by slug or UUID")
    public ResponseEntity<ApiResponse<OrganizationDto>> getOrganization(
            @PathVariable String slugOrId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID userId = currentUser != null ? currentUser.getId() : null;
        OrganizationDto org = organizationService.getOrganizationBySlugOrId(slugOrId, userId);
        return ResponseEntity.ok(ApiResponse.success(org));
    }

    @GetMapping("/my")
    @Operation(summary = "Get organizations where the authenticated user is a member/organizer")
    public ResponseEntity<ApiResponse<List<OrganizationDto>>> getMyOrganizations(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        List<OrganizationDto> orgs = organizationService.getUserOrganizations(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(orgs));
    }

    @PostMapping
    @Operation(summary = "Create a new organization (Creator becomes Owner)")
    public ResponseEntity<ApiResponse<OrganizationDto>> createOrganization(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody CreateOrganizationRequest request
    ) {
        OrganizationDto org = organizationService.createOrganization(currentUser.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Organization created successfully", org), HttpStatus.CREATED);
    }

    @PutMapping("/{orgId}")
    @PreAuthorize("@orgPermissionEvaluator.canManageOrganization(authentication, #orgId)")
    @Operation(summary = "Update organization details (Requires Owner or Organizer role)")
    public ResponseEntity<ApiResponse<OrganizationDto>> updateOrganization(
            @PathVariable UUID orgId,
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        OrganizationDto org = organizationService.updateOrganization(orgId, request);
        return ResponseEntity.ok(ApiResponse.success("Organization updated successfully", org));
    }
}
