package com.civicpulse.modules.organization.dto;

import com.civicpulse.modules.organization.model.OrgRole;
import com.civicpulse.modules.organization.model.OrganizationMembership;
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
public class OrgMemberDto {

    private UUID id;
    private UUID userId;
    private String email;
    private String fullName;
    private String avatarUrl;
    private OrgRole role;
    private Instant joinedAt;

    public static OrgMemberDto fromEntity(OrganizationMembership membership) {
        if (membership == null) return null;
        return OrgMemberDto.builder()
                .id(membership.getId())
                .userId(membership.getUser().getId())
                .email(membership.getUser().getEmail())
                .fullName(membership.getUser().getFullName())
                .avatarUrl(membership.getUser().getAvatarUrl())
                .role(membership.getOrgRole())
                .joinedAt(membership.getJoinedAt())
                .build();
    }
}
