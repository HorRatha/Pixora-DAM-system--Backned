package com.dam.digitalassetmanagement.dto.request;

import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Advanced search request")
public class SearchRequest {

    @Schema(description = "Search query", example = "logo")
    private String query;

    @Schema(description = "Asset type filter")
    private AssetType type;

    @Schema(description = "Asset status filter")
    private AssetStatus status;

    @Schema(description = "Search by metadata")
    private Map<String, String> metadata;

    @Schema(description = "Uploaded by user ID")
    private Long userId;

    @Schema(description = "Page number", example = "0")
    private Integer page = 0;

    @Schema(description = "Page size", example = "20")
    private Integer size = 20;
}