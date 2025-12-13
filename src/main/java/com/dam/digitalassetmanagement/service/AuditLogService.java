package com.dam.digitalassetmanagement.service;

import com.dam.digitalassetmanagement.entity.Asset;
import com.dam.digitalassetmanagement.entity.AuditLog;
import com.dam.digitalassetmanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    void logAssetUpload(User user, Asset asset);
    void logAssetUpdate(User user, Asset asset);
    void logAssetDeletion(User user, Asset asset);
    void logAssetApproval(User user, Asset asset);
    void logAssetRejection(User user, Asset asset, String reason);
    void logAssetDownload(User user, Asset asset);
    Page<AuditLog> getUserLogs(Long userId, Pageable pageable);
    Page<AuditLog> getAssetLogs(Long assetId, Pageable pageable);
}