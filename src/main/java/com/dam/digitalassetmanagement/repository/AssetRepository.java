package com.dam.digitalassetmanagement.repository;

import com.dam.digitalassetmanagement.entity.Asset;
import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    Page<Asset> findByStatus(AssetStatus status, Pageable pageable);

    Page<Asset> findByType(AssetType type, Pageable pageable);

    Page<Asset> findByStatusAndType(AssetStatus status, AssetType type, Pageable pageable);

    Page<Asset> findByUser_UserId(Long userId, Pageable pageable);

    Page<Asset> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title, String description, Pageable pageable);

    @Query("SELECT a FROM Asset a WHERE a.status = :status AND a.createdAt >= :startDate")
    List<Asset> findRecentByStatus(@Param("status") AssetStatus status,
                                   @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(a) FROM Asset a WHERE a.user.userId = :userId AND a.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") AssetStatus status);

    @Query("SELECT a FROM Asset a WHERE a.isActive = true ORDER BY a.createdAt DESC")
    List<Asset> findTop10ByOrderByCreatedAtDesc(Pageable pageable);
}