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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public void reindexAllAssets() {
        try {
            log.info("Starting reindex of all assets...");

            // ⭐ NOTE: Don't use deleteAll() here, it doesn't work properly
            // The index should be deleted via SearchController.rebuildIndex()

            // Get all assets from database
            List<Asset> assets = assetRepository.findAll();
            log.info("Found {} assets to reindex", assets.size());

            if (assets.isEmpty()) {
                log.warn("No assets found in database to reindex");
                return;
            }

            // Convert and index with error handling
            List<AssetDocument> documents = new ArrayList<>();
            int successCount = 0;
            int errorCount = 0;

            for (Asset asset : assets) {
                try {
                    AssetDocument document = convertToDocument(asset);
                    documents.add(document);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.error("Error converting asset {} to document: {}",
                            asset.getAssetId(), e.getMessage(), e);
                }
            }

            log.info("Successfully converted {} assets, {} errors", successCount, errorCount);

            if (!documents.isEmpty()) {
                searchRepository.saveAll(documents);
                log.info("Reindexing completed. Total assets indexed: {}", documents.size());
            } else {
                log.error("No documents to save - all conversions failed");
                throw new RuntimeException("Failed to convert any assets to documents");
            }

        } catch (Exception e) {
            log.error("Error during reindexing", e);
            throw new RuntimeException("Failed to reindex assets: " + e.getMessage(), e);
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
            log.info("Advanced search - keyword: {}, type: {}, status: {}, userId: {}",
                    keyword, type, status, userId);

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
                criteriaList.add(new Criteria("type").is(type.name()));
            }

            // Status filter
            if (status != null) {
                criteriaList.add(new Criteria("status").is(status.name()));
            }

            // User filter
            if (userId != null) {
                criteriaList.add(new Criteria("userId").is(userId));
            }

            // Active assets only
            criteriaList.add(new Criteria("isActive").is(true));

            // If no criteria, return all active assets
            if (criteriaList.isEmpty() || (criteriaList.size() == 1 && keyword == null && type == null && status == null && userId == null)) {
                log.info("No specific criteria, returning all active assets");
                return searchRepository.findByIsActiveTrue(pageable);
            }

            // Build combined criteria
            Criteria criteria = criteriaList.get(0);
            for (int i = 1; i < criteriaList.size(); i++) {
                criteria = criteria.and(criteriaList.get(i));
            }

            CriteriaQuery query = new CriteriaQuery(criteria);
            query.setPageable(pageable);

            log.info("Executing Elasticsearch query with criteria");
            SearchHits<AssetDocument> searchHits = elasticsearchOperations.search(
                    query, AssetDocument.class);

            List<AssetDocument> content = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            log.info("Found {} results", content.size());
            return new PageImpl<>(content, pageable, searchHits.getTotalHits());

        } catch (Exception e) {
            log.error("Error in advanced search - Details: {}", e.getMessage(), e);
            return Page.empty(pageable);
        }
    }

    /**
     * Convert Asset entity to AssetDocument for Elasticsearch
     */
    private AssetDocument convertToDocument(Asset asset) {
        try {
            // Extract file extension from fileUrl
            String fileExtension = "";
            if (asset.getFileUrl() != null && asset.getFileUrl().contains(".")) {
                fileExtension = asset.getFileUrl().substring(asset.getFileUrl().lastIndexOf(".") + 1);
            }

            // Safe metadata handling with null check
            String tags = "";
            try {
                if (asset.getMetadata() != null && !asset.getMetadata().isEmpty()) {
                    tags = asset.getMetadata().stream()
                            .map(m -> m.getKey() + ":" + m.getValue())
                            .collect(Collectors.joining(", "));
                }
            } catch (Exception e) {
                log.warn("Could not load metadata for asset {}, using empty tags. Error: {}",
                        asset.getAssetId(), e.getMessage());
            }

            // Safe user handling
            Long userId = null;
            String username = "";
            try {
                if (asset.getUser() != null) {
                    userId = asset.getUser().getUserId();
                    username = asset.getUser().getUsername();
                }
            } catch (Exception e) {
                log.warn("Could not load user for asset {}, using null user. Error: {}",
                        asset.getAssetId(), e.getMessage());
            }

            return AssetDocument.builder()
                    .assetId(asset.getAssetId())
                    .title(asset.getTitle())
                    .description(asset.getDescription())
                    .type(asset.getType())
                    .status(asset.getStatus())
                    .fileUrl(asset.getFileUrl())
                    .thumbnailUrl(asset.getThumbnailUrl())
                    .userId(userId)
                    .username(username)
                    .version(asset.getVersion())
                    .isActive(asset.getIsActive())
                    .createdAt(asset.getCreatedAt())
                    .updatedAt(asset.getUpdatedAt())
                    .tags(tags)
                    .fileExtension(fileExtension)
                    .fileSize(0L)
                    .build();

        } catch (Exception e) {
            log.error("Error converting asset {} to document: {}", asset.getAssetId(), e.getMessage(), e);
            throw new RuntimeException("Failed to convert asset " + asset.getAssetId(), e);
        }
    }
}