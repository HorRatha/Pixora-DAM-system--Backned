package com.dam.digitalassetmanagement.repository;

import com.dam.digitalassetmanagement.entity.AssetUsage;
import com.dam.digitalassetmanagement.enums.UsageAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssetUsageRepository extends JpaRepository<AssetUsage, Long> {

    Page<AssetUsage> findByAsset_AssetId(Long assetId, Pageable pageable);

    Page<AssetUsage> findByUser_UserId(Long userId, Pageable pageable);

    @Query("SELECT COUNT(au) FROM AssetUsage au WHERE au.asset.assetId = :assetId AND au.action = :action")
    Long countByAssetIdAndAction(@Param("assetId") Long assetId, @Param("action") UsageAction action);

    @Query("SELECT au.asset.assetId, COUNT(au) as usageCount FROM AssetUsage au " +
            "WHERE au.action = 'DOWNLOAD' AND au.timestamp >= :startDate " +
            "GROUP BY au.asset.assetId ORDER BY usageCount DESC")
    List<Object[]> findMostDownloadedAssets(@Param("startDate") LocalDateTime startDate, Pageable pageable);
}