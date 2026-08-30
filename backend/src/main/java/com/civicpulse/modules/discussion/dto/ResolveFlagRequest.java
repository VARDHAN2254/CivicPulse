package com.civicpulse.modules.discussion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveFlagRequest {

    @NotBlank(message = "Action is required (DISMISS or REMOVE_POST)")
    private String action; // DISMISS, REMOVE_POST

    private String resolutionNotes;
}
