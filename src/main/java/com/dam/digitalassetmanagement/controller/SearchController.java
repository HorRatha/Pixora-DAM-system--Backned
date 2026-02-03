package com.dam.digitalassetmanagement.controller;


import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import com.dam.digitalassetmanagement.search.AssetDocument;
import com.dam.digitalassetmanagement.search.AssetSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search", description = "Elasticsearch-powered asset search")
public class SearchController {

    private final AssetSearchService assetSearchService;
    private final ElasticsearchOperations elasticsearchOperations;

    @GetMapping
    @Operation(summary = "Search assets by keyword")
    public ResponseEntity<Page<AssetDocument>> searchAssets(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AssetDocument> results = assetSearchService.searchByKeyword(
                keyword, PageRequest.of(page, size));
        return ResponseEntity.ok(results);
    }

    @GetMapping("/advanced")
    @Operation(summary = "Advanced search with filters")
    public ResponseEntity<Page<AssetDocument>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AssetType type,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AssetDocument> results = assetSearchService.advancedSearch(
                keyword, type, status, userId, PageRequest.of(page, size));
        return ResponseEntity.ok(results);
    }

    @GetMapping("/by-title")
    @Operation(summary = "Search assets by title")
    public ResponseEntity<Page<AssetDocument>> searchByTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AssetDocument> results = assetSearchService.searchByTitle(
                title, PageRequest.of(page, size));
        return ResponseEntity.ok(results);
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reindex all assets (Admin only)")
    public ResponseEntity<Map<String, String>> reindexAllAssets() {
        assetSearchService.reindexAllAssets();
        return ResponseEntity.ok(Map.of("message", "Reindexing started successfully"));
    }

    /**
     * Delete and recreate Elasticsearch index, then reindex all assets
     * This fixes the date format issue
     */
    @PostMapping("/rebuild-index")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rebuild Elasticsearch index from scratch (Admin only)")
    public ResponseEntity<Map<String, Object>> rebuildIndex() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== Starting Elasticsearch index rebuild ===");

            // Step 1: Get index operations for AssetDocument
            IndexOperations indexOps = elasticsearchOperations.indexOps(AssetDocument.class);

            // Step 2: Delete the existing index
            if (indexOps.exists()) {
                log.info("Deleting existing index...");
                boolean deleted = indexOps.delete();
                log.info("Index deletion result: {}", deleted);
                response.put("indexDeleted", deleted);
            } else {
                log.info("Index does not exist, skipping deletion");
                response.put("indexDeleted", false);
            }

            // Step 3: Create a new index with correct mappings
            log.info("Creating new index with correct mappings...");
            boolean created = indexOps.create();
            log.info("Index creation result: {}", created);
            response.put("indexCreated", created);

            // Step 4: Put mapping
            log.info("Putting index mapping...");
            indexOps.putMapping(indexOps.createMapping());
            log.info("Mapping created successfully");
            response.put("mappingCreated", true);

            // Step 5: Reindex all assets with detailed logging
            log.info("Starting to reindex all assets...");
            try {
                assetSearchService.reindexAllAssets();
                log.info("Reindexing completed successfully");
                response.put("reindexed", true);
                response.put("status", "success");
                response.put("message", "Elasticsearch index rebuilt and reindexed successfully!");

                return ResponseEntity.ok(response);

            } catch (Exception reindexError) {
                log.error("Error during reindexing: {}", reindexError.getMessage(), reindexError);
                response.put("reindexed", false);
                response.put("status", "partial_success");
                response.put("message", "Index was recreated but reindexing failed: " + reindexError.getMessage());
                response.put("error", reindexError.getMessage());

                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            log.error("=== Error rebuilding index ===", e);
            response.put("status", "error");
            response.put("message", "Failed to rebuild index: " + e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());

            return ResponseEntity.status(500).body(response);
        }
    }
}