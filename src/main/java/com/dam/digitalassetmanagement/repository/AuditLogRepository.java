package com.dam.digitalassetmanagement.repository;

import com.dam.digitalassetmanagement.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUser_UserId(Long userId, Pageable pageable);

    Page<AuditLog> findByAsset_AssetId(Long assetId, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.timestamp >= :startDate ORDER BY al.timestamp DESC")
    List<AuditLog> findRecentLogs(LocalDateTime startDate, Pageable pageable);

    Page<AuditLog> findByActionContainingIgnoreCase(String action, Pageable pageable);
}