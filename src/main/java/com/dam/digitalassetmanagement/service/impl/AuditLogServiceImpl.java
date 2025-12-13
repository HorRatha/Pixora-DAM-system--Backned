package com.dam.digitalassetmanagement.service.impl;

import com.dam.digitalassetmanagement.entity.Asset;
import com.dam.digitalassetmanagement.entity.AuditLog;
import com.dam.digitalassetmanagement.entity.User;
import com.dam.digitalassetmanagement.repository.AuditLogRepository;
import com.dam.digitalassetmanagement.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void logAssetUpload(User user, Asset asset) {
        createLog(user, asset, "ASSET_UPLOAD",
                "User uploaded asset: " + asset.getTitle());
    }

    @Override
    @Transactional
    public void logAssetUpdate(User user, Asset asset) {
        createLog(user, asset, "ASSET_UPDATE",
                "User updated asset: " + asset.getTitle());
    }

    @Override
    @Transactional
    public void logAssetDeletion(User user, Asset asset) {
        createLog(user, asset, "ASSET_DELETE",
                "User deleted asset: " + asset.getTitle());
    }

    @Override
    @Transactional
    public void logAssetApproval(User user, Asset asset) {
        createLog(user, asset, "ASSET_APPROVE",
                "User approved asset: " + asset.getTitle());
    }

    @Override
    @Transactional
    public void logAssetRejection(User user, Asset asset, String reason) {
        createLog(user, asset, "ASSET_REJECT",
                "User rejected asset: " + asset.getTitle() + ". Reason: " + reason);
    }

    @Override
    @Transactional
    public void logAssetDownload(User user, Asset asset) {
        createLog(user, asset, "ASSET_DOWNLOAD",
                "User downloaded asset: " + asset.getTitle());
    }

    @Override
    public Page<AuditLog> getUserLogs(Long userId, Pageable pageable) {
        return auditLogRepository.findByUser_UserId(userId, pageable);
    }

    @Override
    public Page<AuditLog> getAssetLogs(Long assetId, Pageable pageable) {
        return auditLogRepository.findByAsset_AssetId(assetId, pageable);
    }

    /**
     * Helper method to create and save audit log entries
     */
    private void createLog(User user, Asset asset, String action, String description) {
        AuditLog log = AuditLog.builder()
                .user(user)
                .asset(asset)
                .action(action)
                .description(description)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }
}