package com.dam.digitalassetmanagement.repository;

import com.dam.digitalassetmanagement.entity.AssetMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetMetadataRepository extends JpaRepository<AssetMetadata, Long> {

    List<AssetMetadata> findByAsset_AssetId(Long assetId);

    List<AssetMetadata> findByKey(String key);

    @Query("SELECT am FROM AssetMetadata am WHERE am.key = :key AND am.value = :value")
    List<AssetMetadata> findByKeyAndValue(String key, String value);

    void deleteByAsset_AssetId(Long assetId);
}