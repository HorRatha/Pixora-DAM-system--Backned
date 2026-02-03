package com.dam.digitalassetmanagement.repository;

import com.dam.digitalassetmanagement.entity.Asset;
import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    // ✅ Basic query methods with @EntityGraph to prevent lazy loading issues
    @EntityGraph(attributePaths = {"user"})
    Page<Asset> findByStatus(AssetStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Asset> findByType(AssetType type, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Asset> findByStatusAndType(AssetStatus status, AssetType type, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Asset> findByUser_UserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Asset> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title, String description, Pageable pageable);

    // ✅ Custom query methods
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT a FROM Asset a WHERE a.status = :status AND a.createdAt >= :startDate")
    List<Asset> findRecentByStatus(@Param("status") AssetStatus status,
                                   @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(a) FROM Asset a WHERE a.user.userId = :userId AND a.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") AssetStatus status);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT a FROM Asset a WHERE a.isActive = true ORDER BY a.createdAt DESC")
    List<Asset> findTop10ByOrderByCreatedAtDesc(Pageable pageable);

    // ✅ Helper methods with @EntityGraph
    @EntityGraph(attributePaths = {"user"})
    Optional<Asset> findById(Long id);

    @EntityGraph(attributePaths = {"user"})
    Page<Asset> findAll(Pageable pageable);

    Long countByUser_UserId(Long userId);

    // ✅ NEW: Find assets that are NOT pending (APPROVED or REJECTED)
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT a FROM Asset a WHERE a.status != :pendingStatus")
    Page<Asset> findByStatusNot(@Param("pendingStatus") AssetStatus pendingStatus, Pageable pageable);

    // ✅ NEW: Find assets that are APPROVED or PENDING
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT a FROM Asset a WHERE a.status IN :statuses")
    Page<Asset> findByStatusIn(@Param("statuses") List<AssetStatus> statuses, Pageable pageable);

    // ✅ **NEW METHOD ADDED FOR ELASTICSEARCH REINDEXING** - Eagerly fetch metadata and user
    @Query("SELECT DISTINCT a FROM Asset a LEFT JOIN FETCH a.metadata LEFT JOIN FETCH a.user")
    List<Asset> findAllWithMetadata();
}