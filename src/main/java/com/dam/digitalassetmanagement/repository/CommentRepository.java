package com.dam.digitalassetmanagement.repository;

import com.dam.digitalassetmanagement.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Get all comments for an asset (excluding deleted)
    List<Comment> findByAssetIdAndIsDeletedFalseOrderByCreatedAtDesc(Long assetId);

    // Get top-level comments (no parent)
    List<Comment> findByAssetIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(Long assetId);

    // Get replies to a comment
    List<Comment> findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentId);

    // Count comments for an asset
    Long countByAssetIdAndIsDeletedFalse(Long assetId);

    // Count replies to a comment
    Long countByParentIdAndIsDeletedFalse(Long parentId);

    // Find user's comments on an asset
    List<Comment> findByAssetIdAndUserIdAndIsDeletedFalse(Long assetId, Long userId);

    // Check if user has commented
    boolean existsByAssetIdAndUserIdAndIsDeletedFalse(Long assetId, Long userId);
}