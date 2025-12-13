package com.dam.digitalassetmanagement.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.dam.digitalassetmanagement.dto.request.AssetUpdateRequest;
import com.dam.digitalassetmanagement.dto.request.AssetUploadRequest;
import com.dam.digitalassetmanagement.dto.response.AssetResponse;
import com.dam.digitalassetmanagement.dto.response.UserResponse;
import com.dam.digitalassetmanagement.entity.Asset;
import com.dam.digitalassetmanagement.entity.AssetMetadata;
import com.dam.digitalassetmanagement.entity.User;
import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import com.dam.digitalassetmanagement.exception.CustomExceptions;
import com.dam.digitalassetmanagement.repository.AssetMetadataRepository;
import com.dam.digitalassetmanagement.repository.AssetRepository;
import com.dam.digitalassetmanagement.service.AssetService;
import com.dam.digitalassetmanagement.service.AuditLogService;
import com.dam.digitalassetmanagement.service.UserService;
import com.dam.digitalassetmanagement.util.FileUtil;
import com.dam.digitalassetmanagement.util.WatermarkUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final AssetMetadataRepository metadataRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final AmazonS3 amazonS3;
    private final FileUtil fileUtil;
    private final WatermarkUtil watermarkUtil;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    @Transactional
    public AssetResponse uploadAsset(MultipartFile file, AssetUploadRequest request) throws IOException {
        User currentUser = userService.getCurrentUser();

        // Validate file
        if (file.isEmpty()) {
            throw new CustomExceptions.BadRequestException("File is empty");
        }

        // Detect MIME type
        String mimeType = fileUtil.detectMimeType(file);
        AssetType detectedType = fileUtil.determineAssetType(mimeType);

        // Validate type matches request
        if (!detectedType.equals(request.getType())) {
            throw new CustomExceptions.BadRequestException(
                    "File type mismatch. Expected: " + request.getType() + ", Found: " + detectedType);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String uniqueFilename = fileUtil.generateUniqueFileName(originalFilename);

        // Upload to S3
        String fileUrl = uploadToS3(file, uniqueFilename);

        // Create thumbnail if image
        String thumbnailUrl = null;
        if (fileUtil.isImageFile(mimeType)) {
            thumbnailUrl = createAndUploadThumbnail(file, uniqueFilename);
        }

        // Create asset entity
        Asset asset = Asset.builder()
                .user(currentUser)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .fileUrl(fileUrl)
                .thumbnailUrl(thumbnailUrl)
                .status(AssetStatus.PENDING)
                .build();

        Asset savedAsset = assetRepository.save(asset);

        // Save metadata
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            saveMetadata(savedAsset, request.getMetadata());
        }

        // Audit log
        auditLogService.logAssetUpload(currentUser, savedAsset);

        return mapToAssetResponse(savedAsset);
    }

    @Override
    public AssetResponse getAssetById(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));
        return mapToAssetResponse(asset);
    }

    @Override
    public Page<AssetResponse> getAllAssets(Pageable pageable) {
        return assetRepository.findAll(pageable)
                .map(this::mapToAssetResponse);
    }

    @Override
    public Page<AssetResponse> getAssetsByType(AssetType type, Pageable pageable) {
        return assetRepository.findByType(type, pageable)
                .map(this::mapToAssetResponse);
    }

    @Override
    public Page<AssetResponse> getAssetsByStatus(AssetStatus status, Pageable pageable) {
        return assetRepository.findByStatus(status, pageable)
                .map(this::mapToAssetResponse);
    }

    @Override
    public Page<AssetResponse> searchAssets(String query, Pageable pageable) {
        return assetRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        query, query, pageable)
                .map(this::mapToAssetResponse);
    }

    @Override
    public Page<AssetResponse> getAssetsByCurrentUser(PageRequest pageRequest) {
        User currentUser = userService.getCurrentUser();
        return assetRepository.findByUser_UserId(currentUser.getUserId(), pageRequest)
                .map(this::mapToAssetResponse);
    }

    @Override
    @Transactional
    public AssetResponse updateAsset(Long assetId, AssetUpdateRequest request) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));

        User currentUser = userService.getCurrentUser();

        // Check permission
        if (!asset.getUser().getUserId().equals(currentUser.getUserId()) &&
                !currentUser.getRole().name().equals("ADMIN")) {
            throw new CustomExceptions.UnauthorizedException(
                    "You don't have permission to update this asset");
        }

        asset.setTitle(request.getTitle());
        asset.setDescription(request.getDescription());

        // Update metadata
        if (request.getMetadata() != null) {
            metadataRepository.deleteByAsset_AssetId(assetId);
            saveMetadata(asset, request.getMetadata());
        }

        Asset updatedAsset = assetRepository.save(asset);

        // Audit log
        auditLogService.logAssetUpdate(currentUser, updatedAsset);

        return mapToAssetResponse(updatedAsset);
    }

    @Override
    @Transactional
    public AssetResponse approveAsset(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));

        asset.setStatus(AssetStatus.APPROVED);
        Asset savedAsset = assetRepository.save(asset);

        User currentUser = userService.getCurrentUser();
        auditLogService.logAssetApproval(currentUser, savedAsset);

        return mapToAssetResponse(savedAsset);
    }

    @Override
    @Transactional
    public AssetResponse rejectAsset(Long assetId, String reason) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));

        asset.setStatus(AssetStatus.REJECTED);
        Asset savedAsset = assetRepository.save(asset);

        User currentUser = userService.getCurrentUser();
        auditLogService.logAssetRejection(currentUser, savedAsset, reason);

        return mapToAssetResponse(savedAsset);
    }

    @Override
    @Transactional
    public void deleteAsset(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));

        // Soft delete
        asset.setIsActive(false);
        assetRepository.save(asset);

        User currentUser = userService.getCurrentUser();
        auditLogService.logAssetDeletion(currentUser, asset);
    }

    @Override
    public byte[] downloadAsset(Long assetId) throws IOException {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));

        // Download from S3
        String key = extractKeyFromUrl(asset.getFileUrl());
        S3Object s3Object = amazonS3.getObject(bucketName, key);
        InputStream inputStream = s3Object.getObjectContent();

        return inputStream.readAllBytes();
    }

    // Helper methods

    private String uploadToS3(MultipartFile file, String filename) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        PutObjectRequest putObjectRequest = new PutObjectRequest(
                bucketName,
                "assets/" + filename,
                file.getInputStream(),
                metadata
        );

        amazonS3.putObject(putObjectRequest);
        return amazonS3.getUrl(bucketName, "assets/" + filename).toString();
    }

    private String createAndUploadThumbnail(MultipartFile file, String originalFilename) throws IOException {
        // Create temp file
        File tempFile = File.createTempFile("temp", fileUtil.getFileExtension(originalFilename));
        file.transferTo(tempFile);

        // Create thumbnail
        String thumbnailFilename = "thumb_" + originalFilename;
        File thumbnailFile = watermarkUtil.createWatermarkedThumbnail(
                tempFile,
                uploadDir + "/" + thumbnailFilename
        );

        // Upload thumbnail to S3
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(thumbnailFile.length());
        metadata.setContentType("image/png");

        PutObjectRequest putObjectRequest = new PutObjectRequest(
                bucketName,
                "thumbnails/" + thumbnailFilename,
                thumbnailFile
        );

        amazonS3.putObject(putObjectRequest);

        // Clean up temp files
        tempFile.delete();
        thumbnailFile.delete();

        return amazonS3.getUrl(bucketName, "thumbnails/" + thumbnailFilename).toString();
    }

    private void saveMetadata(Asset asset, Map<String, String> metadataMap) {
        metadataMap.forEach((key, value) -> {
            AssetMetadata metadata = AssetMetadata.builder()
                    .asset(asset)
                    .key(key)
                    .value(value)
                    .build();
            metadataRepository.save(metadata);
        });
    }

    private String extractKeyFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    private AssetResponse mapToAssetResponse(Asset asset) {
        // Get metadata
        Map<String, String> metadata = asset.getMetadata().stream()
                .collect(Collectors.toMap(
                        AssetMetadata::getKey,
                        AssetMetadata::getValue
                ));

        return AssetResponse.builder()
                .assetId(asset.getAssetId())
                .title(asset.getTitle())
                .description(asset.getDescription())
                .type(asset.getType())
                .fileUrl(asset.getFileUrl())
                .thumbnailUrl(asset.getThumbnailUrl())
                .version(asset.getVersion())
                .isActive(asset.getIsActive())
                .status(asset.getStatus())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .uploader(mapToUserResponse(asset.getUser()))
                .metadata(metadata)
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