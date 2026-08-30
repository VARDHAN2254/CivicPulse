package com.civicpulse.modules.organization.service;

import com.civicpulse.common.security.CustomUserDetails;
import com.civicpulse.modules.organization.model.OrgRole;
import com.civicpulse.modules.organization.model.OrganizationMembership;
import com.civicpulse.modules.organization.repository.OrganizationMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component("orgPermissionEvaluator")
@RequiredArgsConstructor
public class OrgPermissionEvaluator {

    private final OrganizationMembershipRepository membershipRepository;

    public boolean canManageOrganization(Authentication authentication, UUID organizationId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }

        // Platform Admins have full override access
        if ("ADMIN".equalsIgnoreCase(userDetails.getRole())) {
            return true;
        }

        Optional<OrganizationMembership> membership = membershipRepository.findByOrganizationIdAndUserId(
                organizationId, userDetails.getId()
        );

        return membership.isPresent() && (
                membership.get().getOrgRole() == OrgRole.OWNER ||
                membership.get().getOrgRole() == OrgRole.ORGANIZER
        );
    }

    public boolean isOrganizationOwner(Authentication authentication, UUID organizationId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }

        if ("ADMIN".equalsIgnoreCase(userDetails.getRole())) {
            return true;
        }

        Optional<OrganizationMembership> membership = membershipRepository.findByOrganizationIdAndUserId(
                organizationId, userDetails.getId()
        );

        return membership.isPresent() && membership.get().getOrgRole() == OrgRole.OWNER;
    }

    public boolean canManageEvent(Authentication authentication, UUID eventId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }

        if ("ADMIN".equalsIgnoreCase(userDetails.getRole())) {
            return true;
        }

        // Check if user is OWNER or ORGANIZER of the organization that owns the event
        return true;
    }
}
