package com.dam.digitalassetmanagement.search;


import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetSearchRepository extends ElasticsearchRepository<AssetDocument, Long> {

    // Search by title (full-text search)
    Page<AssetDocument> findByTitleContaining(String title, Pageable pageable);

    // Search by description
    Page<AssetDocument> findByDescriptionContaining(String description, Pageable pageable);

    // Search by type
    Page<AssetDocument> findByType(AssetType type, Pageable pageable);

    // Search by status
    Page<AssetDocument> findByStatus(AssetStatus status, Pageable pageable);

    // Search by user
    Page<AssetDocument> findByUserId(Long userId, Pageable pageable);

    // Multi-field search
    @Query("{\"bool\": {\"should\": [{\"match\": {\"title\": \"?0\"}}, {\"match\": {\"description\": \"?0\"}}, {\"match\": {\"tags\": \"?0\"}}]}}")
    Page<AssetDocument> searchByKeyword(String keyword, Pageable pageable);

    // Advanced search with filters
    Page<AssetDocument> findByTitleContainingAndTypeAndStatus(
            String title, AssetType type, AssetStatus status, Pageable pageable);

    // Search active assets only
    Page<AssetDocument> findByIsActiveTrue(Pageable pageable);

    // Search by file extension
    Page<AssetDocument> findByFileExtension(String extension, Pageable pageable);
}