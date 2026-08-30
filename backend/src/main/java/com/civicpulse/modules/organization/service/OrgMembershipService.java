package com.civicpulse.modules.organization.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ConflictException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.modules.organization.dto.AddMemberRequest;
import com.civicpulse.modules.organization.dto.OrgMemberDto;
import com.civicpulse.modules.organization.dto.UpdateMemberRoleRequest;
import com.civicpulse.modules.organization.model.OrgRole;
import com.civicpulse.modules.organization.model.Organization;
import com.civicpulse.modules.organization.model.OrganizationMembership;
import com.civicpulse.modules.organization.repository.OrganizationMembershipRepository;
import com.civicpulse.modules.organization.repository.OrganizationRepository;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgMembershipService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResponse<OrgMemberDto> getOrganizationMembers(UUID orgId, Pageable pageable) {
        Page<OrganizationMembership> membersPage = membershipRepository.findByOrganizationIdWithUser(orgId, pageable);
        return PagedResponse.from(membersPage.map(OrgMemberDto::fromEntity));
    }

    @Transactional
    public OrgMemberDto addMemberToOrganization(UUID orgId, AddMemberRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("No registered user found with email: " + request.getEmail()));

        if (membershipRepository.existsByOrganizationIdAndUserId(orgId, user.getId())) {
            throw new ConflictException("User is already a member of this organization.");
        }

        OrganizationMembership membership = OrganizationMembership.builder()
                .organization(org)
                .user(user)
                .orgRole(request.getRole() != null ? request.getRole() : OrgRole.MEMBER)
                .build();

        OrganizationMembership saved = membershipRepository.save(membership);
        log.info("Added user: {} to organization: {} with role: {}", user.getEmail(), org.getName(), saved.getOrgRole());
        return OrgMemberDto.fromEntity(saved);
    }

    @Transactional
    public OrgMemberDto updateMemberRole(UUID orgId, UUID targetUserId, UpdateMemberRoleRequest request) {
        OrganizationMembership membership = membershipRepository.findByOrganizationIdAndUserId(orgId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        membership.setOrgRole(request.getRole());
        OrganizationMembership updated = membershipRepository.save(membership);
        log.info("Updated role for user: {} in organization: {} to: {}", targetUserId, orgId, request.getRole());
        return OrgMemberDto.fromEntity(updated);
    }

    @Transactional
    public void removeMemberFromOrganization(UUID orgId, UUID targetUserId) {
        OrganizationMembership membership = membershipRepository.findByOrganizationIdAndUserId(orgId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        membershipRepository.delete(membership);
        log.info("Removed user: {} from organization: {}", targetUserId, orgId);
    }
}
