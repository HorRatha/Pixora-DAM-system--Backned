package com.dam.digitalassetmanagement.search;

import com.dam.digitalassetmanagement.entity.Asset;
import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import com.dam.digitalassetmanagement.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetSearchServiceImpl implements AssetSearchService {

    private final AssetSearchRepository searchRepository;
    private final AssetRepository assetRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void indexAsset(Asset asset) {
        try {
            AssetDocument document = convertToDocument(asset);
            searchRepository.save(document);
            log.info("Asset indexed successfully: {}", asset.getAssetId());
        } catch (Exception e) {
            log.error("Error indexing asset: {}", asset.getAssetId(), e);
        }
    }

    @Override
    public void updateAssetIndex(Asset asset) {
        try {
            AssetDocument document = convertToDocument(asset);
            searchRepository.save(document);
            log.info("Asset index updated: {}", asset.getAssetId());
        } catch (Exception e) {
            log.error("Error updating asset index: {}", asset.getAssetId(), e);
        }
    }

    @Override
    public void deleteAssetIndex(Long assetId) {
        try {
            searchRepository.deleteById(assetId);
            log.info("Asset removed from index: {}", assetId);
        } catch (Exception e) {
            log.error("Error removing asset from index: {}", assetId, e);
        }
    }

    @Override
    public void reindexAllAssets() {
        try {
            log.info("Starting reindex of all assets...");

            // Clear existing index
            searchRepository.deleteAll();

            // Get all assets from database
            List<Asset> assets = assetRepository.findAll();

            // Convert and index
            List<AssetDocument> documents = assets.stream()
                    .map(this::convertToDocument)
                    .collect(Collectors.toList());

            searchRepository.saveAll(documents);

            log.info("Reindexing completed. Total assets indexed: {}", documents.size());

        } catch (Exception e) {
            log.error("Error during reindexing", e);
            throw new RuntimeException("Failed to reindex assets", e);
        }
    }

    @Override
    public Page<AssetDocument> searchByKeyword(String keyword, Pageable pageable) {
        try {
            return searchRepository.searchByKeyword(keyword, pageable);
        } catch (Exception e) {
            log.error("Error searching by keyword: {}", keyword, e);
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<AssetDocument> searchByTitle(String title, Pageable pageable) {
        try {
            return searchRepository.findByTitleContaining(title, pageable);
        } catch (Exception e) {
            log.error("Error searching by title: {}", title, e);
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<AssetDocument> searchByType(AssetType type, Pageable pageable) {
        try {
            return searchRepository.findByType(type, pageable);
        } catch (Exception e) {
            log.error("Error searching by type: {}", type, e);
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<AssetDocument> searchByStatus(AssetStatus status, Pageable pageable) {
        try {
            return searchRepository.findByStatus(status, pageable);
        } catch (Exception e) {
            log.error("Error searching by status: {}", status, e);
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<AssetDocument> searchByUser(Long userId, Pageable pageable) {
        try {
            return searchRepository.findByUserId(userId, pageable);
        } catch (Exception e) {
            log.error("Error searching by user: {}", userId, e);
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<AssetDocument> advancedSearch(String keyword, AssetType type,
                                              AssetStatus status, Long userId,
                                              Pageable pageable) {
        try {
            List<Criteria> criteriaList = new ArrayList<>();

            // Keyword search (title or description)
            if (keyword != null && !keyword.isEmpty()) {
                Criteria keywordCriteria = new Criteria("title").contains(keyword)
                        .or(new Criteria("description").contains(keyword))
                        .or(new Criteria("tags").contains(keyword));
                criteriaList.add(keywordCriteria);
            }

            // Type filter
            if (type != null) {
                criteriaList.add(new Criteria("type").is(type));
            }

            // Status filter
            if (status != null) {
                criteriaList.add(new Criteria("status").is(status));
            }

            // User filter
            if (userId != null) {
                criteriaList.add(new Criteria("userId").is(userId));
            }

            // Active assets only
            criteriaList.add(new Criteria("isActive").is(true));

            // If no criteria, return all active assets
            if (criteriaList.isEmpty()) {
                return searchRepository.findByIsActiveTrue(pageable);
            }

            // Build combined criteria
            Criteria criteria = criteriaList.get(0);
            for (int i = 1; i < criteriaList.size(); i++) {
                criteria = criteria.and(criteriaList.get(i));
            }

            CriteriaQuery query = new CriteriaQuery(criteria);
            query.setPageable(pageable);

            SearchHits<AssetDocument> searchHits = elasticsearchOperations.search(
                    query, AssetDocument.class);

            List<AssetDocument> content = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            return new PageImpl<>(content, pageable, searchHits.getTotalHits());

        } catch (Exception e) {
            log.error("Error in advanced search", e);
            return Page.empty(pageable);
        }
    }

    /**
     * Convert Asset entity to AssetDocument for Elasticsearch
     */
    private AssetDocument convertToDocument(Asset asset) {
        // Extract file extension from fileUrl
        String fileExtension = "";
        if (asset.getFileUrl() != null && asset.getFileUrl().contains(".")) {
            fileExtension = asset.getFileUrl().substring(asset.getFileUrl().lastIndexOf(".") + 1);
        }

        // Build tags from metadata (if needed)
        String tags = asset.getMetadata() != null ?
                asset.getMetadata().stream()
                        .map(m -> m.getKey() + ":" + m.getValue())
                        .collect(Collectors.joining(", ")) : "";

        return AssetDocument.builder()
                .assetId(asset.getAssetId())
                .title(asset.getTitle())
                .description(asset.getDescription())
                .type(asset.getType())
                .status(asset.getStatus())
                .fileUrl(asset.getFileUrl())
                .thumbnailUrl(asset.getThumbnailUrl())
                .userId(asset.getUser().getUserId())
                .username(asset.getUser().getUsername())
                .version(asset.getVersion())
                .isActive(asset.getIsActive())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .tags(tags)
                .fileExtension(fileExtension)
                .fileSize(0L) // Add fileSize to Asset entity if needed
                .build();
    }
}