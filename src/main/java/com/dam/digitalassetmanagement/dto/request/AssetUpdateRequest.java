package com.dam.digitalassetmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Asset update request")
public class AssetUpdateRequest {

    @Schema(description = "Asset title", example = "Updated Logo")
    @NotBlank(message = "Title is required")
    private String title;

    @Schema(description = "Asset description")
    private String description;

    @Schema(description = "Custom metadata")
    private Map<String, String> metadata;
}