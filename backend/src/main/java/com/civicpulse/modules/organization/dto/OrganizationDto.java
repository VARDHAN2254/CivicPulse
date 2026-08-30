package com.civicpulse.modules.organization.dto;

import com.civicpulse.modules.organization.model.OrgRole;
import com.civicpulse.modules.organization.model.Organization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String logoUrl;
    private String website;
    private String contactEmail;
    private boolean verified;
    private long memberCount;
    private OrgRole currentUserRole;
    private Instant createdAt;

    public static OrganizationDto fromEntity(Organization org) {
        if (org == null) return null;
        return OrganizationDto.builder()
                .id(org.getId())
                .name(org.getName())
                .slug(org.getSlug())
                .description(org.getDescription())
                .logoUrl(org.getLogoUrl())
                .website(org.getWebsite())
                .contactEmail(org.getContactEmail())
                .verified(org.isVerified())
                .createdAt(org.getCreatedAt())
                .build();
    }
}
