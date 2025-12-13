package com.dam.digitalassetmanagement.service.impl;

import com.dam.digitalassetmanagement.dto.request.CollectionRequest;
import com.dam.digitalassetmanagement.dto.response.AssetResponse;
import com.dam.digitalassetmanagement.dto.response.CollectionResponse;
import com.dam.digitalassetmanagement.dto.response.UserResponse;
import com.dam.digitalassetmanagement.entity.Asset;
import com.dam.digitalassetmanagement.entity.AssetCollection;
import com.dam.digitalassetmanagement.entity.User;
import com.dam.digitalassetmanagement.exception.CustomExceptions;
import com.dam.digitalassetmanagement.repository.AssetRepository;
import com.dam.digitalassetmanagement.repository.CollectionRepository;
import com.dam.digitalassetmanagement.service.CollectionService;
import com.dam.digitalassetmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository collectionRepository;
    private final AssetRepository assetRepository;
    private final UserService userService;

    @Override
    @Transactional
    public CollectionResponse createCollection(CollectionRequest request) {
        User currentUser = userService.getCurrentUser();

        // Check if collection name already exists for this user
        if (collectionRepository.existsByNameAndUser_UserId(request.getName(), currentUser.getUserId())) {
            throw new CustomExceptions.DuplicateResourceException(
                    "Collection with name '" + request.getName() + "' already exists");
        }

        AssetCollection collection = AssetCollection.builder()
                .user(currentUser)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        AssetCollection savedCollection = collectionRepository.save(collection);

        // Add assets if provided
        if (request.getAssetIds() != null && !request.getAssetIds().isEmpty()) {
            for (Long assetId : request.getAssetIds()) {
                addAssetToCollectionInternal(savedCollection, assetId);
            }
        }

        return mapToCollectionResponse(savedCollection);
    }

    @Override
    public CollectionResponse getCollectionById(Long collectionId) {
        AssetCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Collection not found with id: " + collectionId));
        return mapToCollectionResponse(collection);
    }

    @Override
    public Page<CollectionResponse> getAllCollections(Pageable pageable) {
        return collectionRepository.findAll(pageable)
                .map(this::mapToCollectionResponse);
    }

    @Override
    public Page<CollectionResponse> getMyCollections(Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        return collectionRepository.findByUser_UserId(currentUser.getUserId(), pageable)
                .map(this::mapToCollectionResponse);
    }

    @Override
    @Transactional
    public CollectionResponse updateCollection(Long collectionId, CollectionRequest request) {
        AssetCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Collection not found with id: " + collectionId));

        User currentUser = userService.getCurrentUser();

        // Check permission
        if (!collection.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new CustomExceptions.UnauthorizedException(
                    "You don't have permission to update this collection");
        }

        collection.setName(request.getName());
        collection.setDescription(request.getDescription());

        AssetCollection updatedCollection = collectionRepository.save(collection);
        return mapToCollectionResponse(updatedCollection);
    }

    @Override
    @Transactional
    public CollectionResponse addAssetToCollection(Long collectionId, Long assetId) {
        AssetCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Collection not found with id: " + collectionId));

        addAssetToCollectionInternal(collection, assetId);

        return mapToCollectionResponse(collection);
    }

    @Override
    @Transactional
    public CollectionResponse removeAssetFromCollection(Long collectionId, Long assetId) {
        AssetCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Collection not found with id: " + collectionId));

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));

        collection.getAssets().remove(asset);
        collectionRepository.save(collection);

        return mapToCollectionResponse(collection);
    }

    @Override
    @Transactional
    public void deleteCollection(Long collectionId) {
        AssetCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Collection not found with id: " + collectionId));

        User currentUser = userService.getCurrentUser();

        // Check permission
        if (!collection.getUser().getUserId().equals(currentUser.getUserId()) &&
                !currentUser.getRole().name().equals("ADMIN")) {
            throw new CustomExceptions.UnauthorizedException(
                    "You don't have permission to delete this collection");
        }

        collectionRepository.delete(collection);
    }

    // Helper methods

    private void addAssetToCollectionInternal(AssetCollection collection, Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));

        collection.getAssets().add(asset);
        collectionRepository.save(collection);
    }

    private CollectionResponse mapToCollectionResponse(AssetCollection collection) {
        List<AssetResponse> assets = collection.getAssets().stream()
                .map(this::mapToAssetResponse)
                .collect(Collectors.toList());

        return CollectionResponse.builder()
                .collectionId(collection.getCollectionId())
                .name(collection.getName())
                .description(collection.getDescription())
                .owner(mapToUserResponse(collection.getUser()))
                .createdAt(collection.getCreatedAt())
                .updatedAt(collection.getUpdatedAt())
                .assetCount(assets.size())
                .assets(assets)
                .build();
    }

    private AssetResponse mapToAssetResponse(Asset asset) {
        return AssetResponse.builder()
                .assetId(asset.getAssetId())
                .title(asset.getTitle())
                .description(asset.getDescription())
                .type(asset.getType())
                .fileUrl(asset.getFileUrl())
                .thumbnailUrl(asset.getThumbnailUrl())
                .status(asset.getStatus())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}