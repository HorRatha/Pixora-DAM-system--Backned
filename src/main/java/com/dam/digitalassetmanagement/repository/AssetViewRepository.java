package com.dam.digitalassetmanagement.repository;

import com.dam.digitalassetmanagement.entity.AssetView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssetViewRepository extends JpaRepository<AssetView, Long> {

    // Count total views for an asset
    Long countByAssetId(Long assetId);

    // Count unique viewers (registered users + anonymous)
    @Query("SELECT COUNT(DISTINCT COALESCE(CAST(v.userId AS string), v.anonymousId)) " +
            "FROM AssetView v WHERE v.assetId = :assetId")
    Long countUniqueViewers(@Param("assetId") Long assetId);

    // Get all views for an asset
    List<AssetView> findByAssetIdOrderByViewedAtDesc(Long assetId);

    // Get user's views for an asset
    List<AssetView> findByAssetIdAndUserId(Long assetId, Long userId);

    // Get views within date range
    @Query("SELECT v FROM AssetView v WHERE v.assetId = :assetId " +
            "AND v.viewedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY v.viewedAt DESC")
    List<AssetView> findViewsByDateRange(
            @Param("assetId") Long assetId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Get most viewed assets
    @Query("SELECT v.assetId, COUNT(v) as viewCount FROM AssetView v " +
            "GROUP BY v.assetId ORDER BY viewCount DESC")
    List<Object[]> findMostViewedAssets();
}