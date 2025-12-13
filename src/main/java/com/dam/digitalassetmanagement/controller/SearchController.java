package com.dam.digitalassetmanagement.controller;


import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import com.dam.digitalassetmanagement.search.AssetDocument;
import com.dam.digitalassetmanagement.search.AssetSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Elasticsearch-powered asset search")
public class SearchController {

    private final AssetSearchService assetSearchService;

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
}