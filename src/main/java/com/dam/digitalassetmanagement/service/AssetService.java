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
}