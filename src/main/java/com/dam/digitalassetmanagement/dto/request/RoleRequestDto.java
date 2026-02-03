package com.dam.digitalassetmanagement.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequestDto {
    // ✅ FIXED: Changed from UserRole enum to String
    private String requestedRole;
    private String reason;
}