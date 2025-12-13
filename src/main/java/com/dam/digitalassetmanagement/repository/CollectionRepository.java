package com.dam.digitalassetmanagement.repository;

import com.dam.digitalassetmanagement.entity.AssetCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionRepository extends JpaRepository<AssetCollection, Long> {

    Page<AssetCollection> findByUser_UserId(Long userId, Pageable pageable);

    boolean existsByNameAndUser_UserId(String name, Long userId);
}