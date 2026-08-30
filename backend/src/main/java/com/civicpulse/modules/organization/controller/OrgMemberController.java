package com.civicpulse.modules.organization.controller;

import com.civicpulse.common.response.ApiResponse;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.modules.organization.dto.AddMemberRequest;
import com.civicpulse.modules.organization.dto.OrgMemberDto;
import com.civicpulse.modules.organization.dto.UpdateMemberRoleRequest;
import com.civicpulse.modules.organization.service.OrgMembershipService;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/members")
@RequiredArgsConstructor
@Tag(name = "Organization Members", description = "Manage members and roles within an organization")
public class OrgMemberController {

    private final OrgMembershipService membershipService;

    @GetMapping
    @Operation(summary = "List members in an organization")
    public ResponseEntity<ApiResponse<PagedResponse<OrgMemberDto>>> getMembers(
            @PathVariable UUID orgId,
            @PageableDefault(size = 20, sort = "joinedAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PagedResponse<OrgMemberDto> members = membershipService.getOrganizationMembers(orgId, pageable);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @PostMapping
    @PreAuthorize("@orgPermissionEvaluator.canManageOrganization(authentication, #orgId)")
    @Operation(summary = "Add a new member to the organization by email (Owner/Organizer only)")
    public ResponseEntity<ApiResponse<OrgMemberDto>> addMember(
            @PathVariable UUID orgId,
            @Valid @RequestBody AddMemberRequest request
    ) {
        OrgMemberDto member = membershipService.addMemberToOrganization(orgId, request);
        return new ResponseEntity<>(ApiResponse.success("Member added successfully", member), HttpStatus.CREATED);
    }

    @PutMapping("/{targetUserId}/role")
    @PreAuthorize("@orgPermissionEvaluator.isOrganizationOwner(authentication, #orgId)")
    @Operation(summary = "Update member role in the organization (Owner only)")
    public ResponseEntity<ApiResponse<OrgMemberDto>> updateMemberRole(
            @PathVariable UUID orgId,
            @PathVariable UUID targetUserId,
            @Valid @RequestBody UpdateMemberRoleRequest request
    ) {
        OrgMemberDto member = membershipService.updateMemberRole(orgId, targetUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Member role updated", member));
    }

    @DeleteMapping("/{targetUserId}")
    @PreAuthorize("@orgPermissionEvaluator.canManageOrganization(authentication, #orgId)")
    @Operation(summary = "Remove a member from the organization (Owner/Organizer only)")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID orgId,
            @PathVariable UUID targetUserId
    ) {
        membershipService.removeMemberFromOrganization(orgId, targetUserId);
        return ResponseEntity.ok(ApiResponse.message("Member removed from organization"));
    }
}
