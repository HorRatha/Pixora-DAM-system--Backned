package com.dam.digitalassetmanagement.service;

import com.dam.digitalassetmanagement.dto.request.AssetUpdateRequest;
import com.dam.digitalassetmanagement.dto.request.AssetUploadRequest;
import com.dam.digitalassetmanagement.dto.response.AssetResponse;
import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AssetService {
    // Existing methods
    AssetResponse uploadAsset(MultipartFile file, AssetUploadRequest request) throws IOException;
    AssetResponse getAssetById(Long assetId);
    Page<AssetResponse> getAllAssets(Pageable pageable);
    Page<AssetResponse> getAssetsByType(AssetType type, Pageable pageable);
    Page<AssetResponse> getAssetsByStatus(AssetStatus status, Pageable pageable);
    Page<AssetResponse> searchAssets(String query, Pageable pageable);
    AssetResponse updateAsset(Long assetId, AssetUpdateRequest request);
    AssetResponse approveAsset(Long assetId);
    AssetResponse rejectAsset(Long assetId, String reason);
    void deleteAsset(Long assetId);
    byte[] downloadAsset(Long assetId) throws IOException;
    Page<AssetResponse> getAssetsByCurrentUser(PageRequest of);

    // ✅ Public/Approved asset methods
    Page<AssetResponse> getApprovedAssets(Pageable pageable);
    Page<AssetResponse> getApprovedAssetsByType(AssetType type, Pageable pageable);
    AssetResponse getApprovedAssetById(Long assetId);
    byte[] downloadApprovedAsset(Long assetId) throws IOException;

    // ✅ Thumbnail method
    byte[] getThumbnail(Long assetId) throws IOException;

    // ✅ NEW: Get all assets that are NOT pending (approved + rejected)
    Page<AssetResponse> getNonPendingAssets(Pageable pageable);

    // ✅ NEW: Get all assets that are approved OR pending
    Page<AssetResponse> getApprovedAndPendingAssets(Pageable pageable);
}