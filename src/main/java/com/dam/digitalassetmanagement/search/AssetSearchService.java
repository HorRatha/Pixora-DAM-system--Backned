package com.dam.digitalassetmanagement.search;


import com.dam.digitalassetmanagement.entity.Asset;
import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssetSearchService {

    // Index operations
    void indexAsset(Asset asset);
    void updateAssetIndex(Asset asset);
    void deleteAssetIndex(Long assetId);
    void reindexAllAssets();

    // Search operations
    Page<AssetDocument> searchByKeyword(String keyword, Pageable pageable);
    Page<AssetDocument> searchByTitle(String title, Pageable pageable);
    Page<AssetDocument> searchByType(AssetType type, Pageable pageable);
    Page<AssetDocument> searchByStatus(AssetStatus status, Pageable pageable);
    Page<AssetDocument> searchByUser(Long userId, Pageable pageable);

    // Advanced search
    Page<AssetDocument> advancedSearch(
            String keyword,
            AssetType type,
            AssetStatus status,
            Long userId,
            Pageable pageable
    );
}