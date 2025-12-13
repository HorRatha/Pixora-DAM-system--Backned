package com.dam.digitalassetmanagement.controller;

import com.dam.digitalassetmanagement.dto.request.CollectionRequest;
import com.dam.digitalassetmanagement.dto.response.CollectionResponse;
import com.dam.digitalassetmanagement.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
@Tag(name = "Collection Management", description = "Organize assets into collections")
public class CollectionController {

    private final CollectionService collectionService;

    @PostMapping
    @Operation(summary = "Create new collection")
    public ResponseEntity<CollectionResponse> createCollection(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal UserDetails userDetails) {

        CollectionRequest request = new CollectionRequest();
        request.setName(name);
        request.setDescription(description);

        CollectionResponse collection = collectionService.createCollection(request);
        return ResponseEntity.ok(collection);
    }

    @GetMapping
    @Operation(summary = "Get all collections")
    public ResponseEntity<Page<CollectionResponse>> getAllCollections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<CollectionResponse> collections = collectionService.getAllCollections(PageRequest.of(page, size));
        return ResponseEntity.ok(collections);
    }

    @GetMapping("/{collectionId}")
    @Operation(summary = "Get collection by ID")
    public ResponseEntity<CollectionResponse> getCollectionById(@PathVariable Long collectionId) {
        CollectionResponse collection = collectionService.getCollectionById(collectionId);
        return ResponseEntity.ok(collection);
    }

    @GetMapping("/my-collections")
    @Operation(summary = "Get collections by current user")
    public ResponseEntity<Page<CollectionResponse>> getMyCollections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<CollectionResponse> collections = collectionService.getMyCollections(
                PageRequest.of(page, size)
        );
        return ResponseEntity.ok(collections);
    }

    @PostMapping("/{collectionId}/assets/{assetId}")
    @Operation(summary = "Add asset to collection")
    public ResponseEntity<CollectionResponse> addAssetToCollection(
            @PathVariable Long collectionId,
            @PathVariable Long assetId) {

        CollectionResponse collection = collectionService.addAssetToCollection(collectionId, assetId);
        return ResponseEntity.ok(collection);
    }

    @DeleteMapping("/{collectionId}/assets/{assetId}")
    @Operation(summary = "Remove asset from collection")
    public ResponseEntity<CollectionResponse> removeAssetFromCollection(
            @PathVariable Long collectionId,
            @PathVariable Long assetId) {

        CollectionResponse collection = collectionService.removeAssetFromCollection(collectionId, assetId);
        return ResponseEntity.ok(collection);
    }

    @PutMapping("/{collectionId}")
    @Operation(summary = "Update collection")
    public ResponseEntity<CollectionResponse> updateCollection(
            @PathVariable Long collectionId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description) {

        CollectionRequest request = new CollectionRequest();
        request.setName(name);
        request.setDescription(description);

        CollectionResponse collection = collectionService.updateCollection(collectionId, request);
        return ResponseEntity.ok(collection);
    }

    @DeleteMapping("/{collectionId}")
    @Operation(summary = "Delete collection")
    public ResponseEntity<Map<String, String>> deleteCollection(@PathVariable Long collectionId) {
        collectionService.deleteCollection(collectionId);
        return ResponseEntity.ok(Map.of("message", "Collection deleted successfully"));
    }
}