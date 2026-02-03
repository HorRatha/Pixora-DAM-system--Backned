package com.dam.digitalassetmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Profile update request")
public class ProfileUpdateRequest {

    @Schema(description = "Email address", example = "john@example.com")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Full name", example = "John Doe")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Schema(description = "Phone number", example = "+1234567890")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;

    @Schema(description = "Current password for password change")
    private String currentPassword;

    @Schema(description = "New password")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;
}