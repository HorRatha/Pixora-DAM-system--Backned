package com.dam.digitalassetmanagement.dto.request;


import com.dam.digitalassetmanagement.enums.AssetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Asset upload request")
public class AssetUploadRequest {

    @Schema(description = "Asset title", example = "Company Logo", required = true)
    @NotBlank(message = "Title is required")
    private String title;

    @Schema(description = "Asset description", example = "Official company logo 2024")
    private String description;

    @Schema(description = "Asset type", example = "IMAGE", required = true)
    @NotNull(message = "Asset type is required")
    private AssetType type;

    @Schema(description = "Custom metadata key-value pairs")
    private Map<String, String> metadata;
}