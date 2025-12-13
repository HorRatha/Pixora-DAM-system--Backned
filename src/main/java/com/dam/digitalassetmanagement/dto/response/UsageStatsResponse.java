package com.dam.digitalassetmanagement.dto.response;

import com.dam.digitalassetmanagement.enums.UsageAction;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Asset usage statistics")
public class UsageStatsResponse {

    @Schema(description = "Total downloads")
    private Long totalDownloads;

    @Schema(description = "Total views")
    private Long totalViews;

    @Schema(description = "Total shares")
    private Long totalShares;

    @Schema(description = "Most recent action")
    private UsageAction lastAction;

    @Schema(description = "Last action timestamp")
    private LocalDateTime lastActionTime;
}