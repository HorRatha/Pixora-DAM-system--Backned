package com.dam.digitalassetmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Collection create/update request")
public class CollectionRequest {

    @Schema(description = "Collection name", example = "Marketing Materials", required = true)
    @NotBlank(message = "Collection name is required")
    private String name;

    @Schema(description = "Collection description", example = "All marketing campaign assets")
    private String description;

    @Schema(description = "List of asset IDs to add to collection")
    private List<Long> assetIds;
}