package com.civicpulse.modules.organization.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ConflictException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.modules.organization.dto.CreateOrganizationRequest;
import com.civicpulse.modules.organization.dto.OrganizationDto;
import com.civicpulse.modules.organization.dto.UpdateOrganizationRequest;
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

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Transactional
    public OrganizationDto createOrganization(UUID creatorUserId, CreateOrganizationRequest request) {
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String baseSlug = toSlug(request.getName());
        String slug = baseSlug;
        int counter = 1;
        while (organizationRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        Organization org = Organization.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .website(request.getWebsite())
                .contactEmail(request.getContactEmail())
                .verified(false)
                .build();

        Organization savedOrg = organizationRepository.save(org);

        // Creator automatically becomes OWNER
        OrganizationMembership membership = OrganizationMembership.builder()
                .organization(savedOrg)
                .user(creator)
                .orgRole(OrgRole.OWNER)
                .build();
        membershipRepository.save(membership);

        log.info("Created organization: {} (slug: {}) by user: {}", savedOrg.getName(), savedOrg.getSlug(), creator.getEmail());

        OrganizationDto dto = OrganizationDto.fromEntity(savedOrg);
        dto.setMemberCount(1);
        dto.setCurrentUserRole(OrgRole.OWNER);
        return dto;
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrganizationDto> getPublicOrganizations(String query, Pageable pageable) {
        Page<Organization> orgPage;
        if (query != null && !query.isBlank()) {
            orgPage = organizationRepository.searchOrganizations(query.trim(), pageable);
        } else {
            orgPage = organizationRepository.findAll(pageable);
        }

        Page<OrganizationDto> dtoPage = orgPage.map(org -> {
            OrganizationDto dto = OrganizationDto.fromEntity(org);
            dto.setMemberCount(membershipRepository.countByOrganizationId(org.getId()));
            return dto;
        });

        return PagedResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public OrganizationDto getOrganizationBySlugOrId(String identifier, UUID currentUserId) {
        Organization org;
        try {
            UUID id = UUID.fromString(identifier);
            org = organizationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + identifier));
        } catch (IllegalArgumentException e) {
            org = organizationRepository.findBySlug(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found with slug: " + identifier));
        }

        OrganizationDto dto = OrganizationDto.fromEntity(org);
        dto.setMemberCount(membershipRepository.countByOrganizationId(org.getId()));

        if (currentUserId != null) {
            membershipRepository.findByOrganizationIdAndUserId(org.getId(), currentUserId)
                    .ifPresent(m -> dto.setCurrentUserRole(m.getOrgRole()));
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<OrganizationDto> getUserOrganizations(UUID userId) {
        List<OrganizationMembership> memberships = membershipRepository.findByUserIdWithOrg(userId);

        return memberships.stream().map(m -> {
            OrganizationDto dto = OrganizationDto.fromEntity(m.getOrganization());
            dto.setMemberCount(membershipRepository.countByOrganizationId(m.getOrganization().getId()));
            dto.setCurrentUserRole(m.getOrgRole());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public OrganizationDto updateOrganization(UUID orgId, UpdateOrganizationRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        org.setName(request.getName().trim());
        org.setDescription(request.getDescription());
        org.setLogoUrl(request.getLogoUrl());
        org.setWebsite(request.getWebsite());
        org.setContactEmail(request.getContactEmail());

        Organization updated = organizationRepository.save(org);
        OrganizationDto dto = OrganizationDto.fromEntity(updated);
        dto.setMemberCount(membershipRepository.countByOrganizationId(updated.getId()));
        return dto;
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
