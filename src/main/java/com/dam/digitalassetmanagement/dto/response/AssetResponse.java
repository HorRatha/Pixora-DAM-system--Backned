package com.dam.digitalassetmanagement.dto.response;

import com.dam.digitalassetmanagement.entity.Asset;
import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Asset information response")
public class AssetResponse {

    @Schema(description = "Asset ID", example = "1")
    private Long assetId;

    @Schema(description = "Asset title", example = "Company Logo")
    private String title;

    @Schema(description = "Asset description")
    private String description;

    @Schema(description = "Asset type", example = "IMAGE")
    private AssetType type;

    @Schema(description = "File URL")
    private String fileUrl;

    @Schema(description = "Thumbnail URL")
    private String thumbnailUrl;

    @Schema(description = "File size in bytes", example = "1024000")
    private Long fileSize;

    @Schema(description = "MIME type", example = "image/png")
    private String mimeType;

    @Schema(description = "Version number", example = "1")
    private Integer version;

    @Schema(description = "Active status", example = "true")
    private Boolean isActive;

    @Schema(description = "Approval status", example = "APPROVED")
    private AssetStatus status;

    @Schema(description = "Upload timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "Uploader information")
    private UserResponse uploader;

    @Schema(description = "Asset metadata")
    private Map<String, String> metadata;

    @Schema(description = "Number of downloads", example = "42")
    private Long downloadCount;

    public static AssetResponse fromEntity(Asset asset) {
        return null;
    }
}