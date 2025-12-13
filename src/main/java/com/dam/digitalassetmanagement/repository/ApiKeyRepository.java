package com.dam.digitalassetmanagement.repository;

import com.dam.digitalassetmanagement.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByApiKey(String apiKey);

    List<ApiKey> findByUser_UserId(Long userId);

    boolean existsByApiKey(String apiKey);
}