package com.dam.digitalassetmanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequestResponse {
    @JsonProperty("request_id")
    private Long requestId;

    @JsonProperty("user_id")
    private Long userId;

    private String username;

    // ✅ FIXED: Changed from UserRole enum to String
    @JsonProperty("current_role")
    private String currentRole;

    // ✅ FIXED: Changed from UserRole enum to String
    @JsonProperty("requested_role")
    private String requestedRole;

    private String reason;

    // ✅ FIXED: Changed from RequestStatus enum to String
    private String status;

    @JsonProperty("review_comment")
    private String reviewComment;

    @JsonProperty("reviewed_by_username")
    private String reviewedByUsername;

    @JsonProperty("reviewed_at")
    private LocalDateTime reviewedAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}