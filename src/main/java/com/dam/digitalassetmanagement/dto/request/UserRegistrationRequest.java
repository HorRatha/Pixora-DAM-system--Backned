package com.dam.digitalassetmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration request")
public class UserRegistrationRequest {

    @Schema(description = "Username", example = "john_doe")
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Schema(description = "Email address", example = "john@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Password", example = "SecurePass123!")
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // ✅ FIXED: Changed from UserRole enum to String
    // Valid values: "ADMIN", "UPLOADER", "EDITOR", "VIEWER"
    @Schema(description = "User role", example = "VIEWER", allowableValues = {"ADMIN", "UPLOADER", "EDITOR", "VIEWER"})
    private String role = "VIEWER";
}