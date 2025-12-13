package com.dam.digitalassetmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Collection information response")
public class CollectionResponse {

    @Schema(description = "Collection ID", example = "1")
    private Long collectionId;

    @Schema(description = "Collection name", example = "Marketing Materials")
    private String name;

    @Schema(description = "Collection description")
    private String description;

    @Schema(description = "Owner information")
    private UserResponse owner;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "Number of assets", example = "15")
    private Integer assetCount;

    @Schema(description = "Assets in collection")
    private List<AssetResponse> assets;
}