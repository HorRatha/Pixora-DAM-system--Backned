package com.dam.digitalassetmanagement.service;

import com.dam.digitalassetmanagement.dto.request.CollectionRequest;
import com.dam.digitalassetmanagement.dto.response.CollectionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CollectionService {
    CollectionResponse createCollection(CollectionRequest request);
    CollectionResponse getCollectionById(Long collectionId);
    Page<CollectionResponse> getAllCollections(Pageable pageable);
    Page<CollectionResponse> getMyCollections(Pageable pageable);
    CollectionResponse updateCollection(Long collectionId, CollectionRequest request);
    CollectionResponse addAssetToCollection(Long collectionId, Long assetId);
    CollectionResponse removeAssetFromCollection(Long collectionId, Long assetId);
    void deleteCollection(Long collectionId);
}