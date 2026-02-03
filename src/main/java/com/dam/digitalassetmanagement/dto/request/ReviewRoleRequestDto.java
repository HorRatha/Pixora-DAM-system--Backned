package com.dam.digitalassetmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRoleRequestDto {

    @NotBlank(message = "Action is required (APPROVE or REJECT)")
    private String action; // "APPROVE" or "REJECT"

    private String reviewComment; // Admin's reason
}