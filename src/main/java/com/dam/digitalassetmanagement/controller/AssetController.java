package com.dam.digitalassetmanagement.controller;

import com.dam.digitalassetmanagement.dto.request.AssetUpdateRequest;
import com.dam.digitalassetmanagement.dto.request.AssetUploadRequest;
import com.dam.digitalassetmanagement.dto.response.AssetResponse;
import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import com.dam.digitalassetmanagement.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Tag(name = "Asset Management", description = "Upload, manage, and download digital assets")
public class AssetController {

    private final AssetService assetService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('UPLOADER', 'EDITOR', 'ADMIN')")
    @Operation(summary = "Upload new asset")
    public ResponseEntity<AssetResponse> uploadAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("type") AssetType type,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        AssetUploadRequest request = new AssetUploadRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setType(type);

        AssetResponse asset = assetService.uploadAsset(file, request);
        return ResponseEntity.ok(asset);
    }

    @GetMapping
    @Operation(summary = "Get all assets with pagination and filters")
    public ResponseEntity<Page<AssetResponse>> getAllAssets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) AssetType type,
            @RequestParam(required = false) AssetStatus status) {

        Page<AssetResponse> assets;
        if (type != null) {
            assets = assetService.getAssetsByType(type, PageRequest.of(page, size));
        } else if (status != null) {
            assets = assetService.getAssetsByStatus(status, PageRequest.of(page, size));
        } else {
            assets = assetService.getAllAssets(PageRequest.of(page, size));
        }

        return ResponseEntity.ok(assets);
    }

    @GetMapping("/{assetId}")
    @Operation(summary = "Get asset by ID")
    public ResponseEntity<AssetResponse> getAssetById(@PathVariable Long assetId) {
        AssetResponse asset = assetService.getAssetById(assetId);
        return ResponseEntity.ok(asset);
    }

    @GetMapping("/my-assets")
    @Operation(summary = "Get assets by current user")
    public ResponseEntity<Page<AssetResponse>> getMyAssets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AssetResponse> assets = assetService.getAssetsByCurrentUser(
                PageRequest.of(page, size)
        );
        return ResponseEntity.ok(assets);
    }

    @PutMapping("/{assetId}")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Update asset metadata")
    public ResponseEntity<AssetResponse> updateAsset(
            @PathVariable Long assetId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description) {

        AssetUpdateRequest request = new AssetUpdateRequest();
        request.setTitle(title);
        request.setDescription(description);

        AssetResponse asset = assetService.updateAsset(assetId, request);
        return ResponseEntity.ok(asset);
    }

    @PutMapping("/{assetId}/approve")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Approve asset")
    public ResponseEntity<AssetResponse> approveAsset(
            @PathVariable Long assetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        AssetResponse asset = assetService.approveAsset(assetId);
        return ResponseEntity.ok(asset);
    }

    @PutMapping("/{assetId}/reject")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    @Operation(summary = "Reject asset")
    public ResponseEntity<AssetResponse> rejectAsset(
            @PathVariable Long assetId,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails) {

        AssetResponse asset = assetService.rejectAsset(assetId, reason);
        return ResponseEntity.ok(asset);
    }

    @GetMapping("/{assetId}/download")
    @Operation(summary = "Download asset file")
    public ResponseEntity<Resource> downloadAsset(
            @PathVariable Long assetId,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        byte[] data = assetService.downloadAsset(assetId);
        AssetResponse asset = assetService.getAssetById(assetId);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + asset.getTitle() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(resource);
    }

    @DeleteMapping("/{assetId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Delete asset (Admin only)")
    public ResponseEntity<Map<String, String>> deleteAsset(@PathVariable Long assetId) {
        assetService.deleteAsset(assetId);
        return ResponseEntity.ok(Map.of("message", "Asset deleted successfully"));
    }
}