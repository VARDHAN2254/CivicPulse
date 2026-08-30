package com.civicpulse.modules.organization.dto;

import com.civicpulse.modules.organization.model.OrgRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRoleRequest {

    @NotNull(message = "Role is required")
    private OrgRole role;
}
