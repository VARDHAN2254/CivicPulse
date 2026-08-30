package com.civicpulse.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailChangeRequest {

    @NotBlank(message = "New email address is required")
    @Email(message = "Invalid email address format")
    private String newEmail;

    @NotBlank(message = "Current password is required for verification")
    private String currentPassword;
}
