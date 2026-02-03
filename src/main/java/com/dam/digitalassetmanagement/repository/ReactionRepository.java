package com.dam.digitalassetmanagement.repository;

import com.dam.digitalassetmanagement.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    // Find reaction by user
    Optional<Reaction> findByAssetIdAndUserId(Long assetId, Long userId);

    // Find reaction by anonymous user
    Optional<Reaction> findByAssetIdAndAnonymousId(Long assetId, String anonymousId);

    // Get all reactions for an asset
    List<Reaction> findByAssetIdOrderByCreatedAtDesc(Long assetId);

    // Count reactions for an asset
    Long countByAssetId(Long assetId);

    // Count reactions by type
    @Query("SELECT COUNT(r) FROM Reaction r WHERE r.assetId = :assetId AND r.reactionType = :type")
    Long countByAssetIdAndType(@Param("assetId") Long assetId, @Param("type") String type);

    // Check if user has reacted
    boolean existsByAssetIdAndUserId(Long assetId, Long userId);

    // Check if anonymous user has reacted
    boolean existsByAssetIdAndAnonymousId(Long assetId, String anonymousId);
}