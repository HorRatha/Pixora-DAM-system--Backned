package com.dam.digitalassetmanagement.service.impl;

import com.dam.digitalassetmanagement.dto.request.AssetUpdateRequest;
import com.dam.digitalassetmanagement.dto.request.AssetUploadRequest;
import com.dam.digitalassetmanagement.dto.response.AssetResponse;
import com.dam.digitalassetmanagement.dto.response.UserResponse;
import com.dam.digitalassetmanagement.entity.Asset;
import com.dam.digitalassetmanagement.entity.User;
import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import com.dam.digitalassetmanagement.exception.CustomExceptions;
import com.dam.digitalassetmanagement.repository.AssetRepository;
import com.dam.digitalassetmanagement.search.AssetSearchService; // ⭐ ADDED
import com.dam.digitalassetmanagement.service.AssetService;
import com.dam.digitalassetmanagement.service.UserService;
import com.dam.digitalassetmanagement.util.WatermarkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final UserService userService;
    private final WatermarkUtil watermarkUtil;
    private final AssetSearchService assetSearchService; // ⭐ ADDED

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.watermark-enabled:true}")
    private boolean watermarkEnabled;

    @Override
    @Transactional
    public AssetResponse uploadAsset(MultipartFile file, AssetUploadRequest request) throws IOException {
        User currentUser = userService.getCurrentUser();

        // Validate file
        if (file.isEmpty()) {
            throw new CustomExceptions.BadRequestException("File is empty");
        }

        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDir);
        Path thumbnailPath = Paths.get(uploadDir, "thumbnails");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        if (!Files.exists(thumbnailPath)) {
            Files.createDirectories(thumbnailPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        String thumbnailFilename = "thumb_" + uniqueFilename.replaceAll("\\.[^.]+$", ".jpg"); // ✅ Always use .jpg for thumbnails

        // Save file temporarily first
        Path tempFilePath = uploadPath.resolve("temp_" + uniqueFilename);
        Files.copy(file.getInputStream(), tempFilePath);
        File tempFile = tempFilePath.toFile();

        String finalFileUrl;
        String thumbnailUrl = null;

        try {
            // ✅ UPDATED: Handle IMAGE files
            if (request.getType() == AssetType.IMAGE && watermarkEnabled) {
                log.info("Applying watermark to image: {}", uniqueFilename);

                // Add watermark to the main image
                Path watermarkedFilePath = uploadPath.resolve(uniqueFilename);
                File watermarkedFile = watermarkUtil.addWatermark(tempFile, watermarkedFilePath.toString());
                finalFileUrl = watermarkedFile.getAbsolutePath();

                // Create watermarked thumbnail
                Path thumbnailFilePath = thumbnailPath.resolve(thumbnailFilename);
                watermarkUtil.createWatermarkedThumbnail(tempFile, thumbnailFilePath.toString());
                thumbnailUrl = thumbnailFilePath.toString();

                log.info("Watermark applied successfully");
            }
            // ✅ ADDED: Handle VIDEO files - create placeholder thumbnail
            else if (request.getType() == AssetType.VIDEO) {
                log.info("Processing video file: {}", uniqueFilename);

                // Save the video file
                Path finalFilePath = uploadPath.resolve(uniqueFilename);
                Files.move(tempFilePath, finalFilePath);
                finalFileUrl = finalFilePath.toString();

                // Create video thumbnail placeholder
                Path thumbnailFilePath = thumbnailPath.resolve(thumbnailFilename);
                createVideoThumbnailPlaceholder(thumbnailFilePath.toString());
                thumbnailUrl = thumbnailFilePath.toString();

                log.info("Video thumbnail created successfully");
            }
            // ✅ UNCHANGED: Handle other asset types
            else {
                log.info("Skipping watermark for type: {}", request.getType());
                Path finalFilePath = uploadPath.resolve(uniqueFilename);
                Files.move(tempFilePath, finalFilePath);
                finalFileUrl = finalFilePath.toString();

                // Create thumbnail for images without watermark
                if (request.getType() == AssetType.IMAGE) {
                    Path thumbnailFilePath = thumbnailPath.resolve(thumbnailFilename);
                    watermarkUtil.createThumbnail(finalFilePath.toFile(), thumbnailFilePath.toString());
                    thumbnailUrl = thumbnailFilePath.toString();
                }
            }

        } catch (IOException e) {
            log.error("Error processing file: {}", e.getMessage());
            Files.deleteIfExists(tempFilePath);
            throw new CustomExceptions.BadRequestException("Failed to process file: " + e.getMessage());
        } finally {
            Files.deleteIfExists(tempFilePath);
        }

        // Save with thumbnail URL
        Asset asset = Asset.builder()
                .user(currentUser)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .fileUrl(finalFileUrl)
                .thumbnailUrl(thumbnailUrl)
                .status(AssetStatus.PENDING)
                .version(1)
                .isActive(true)
                .build();

        Asset savedAsset = assetRepository.save(asset);

        // ⭐ AUTO-INDEX TO ELASTICSEARCH
        try {
            assetSearchService.indexAsset(savedAsset);
            log.info("Asset indexed to Elasticsearch: {}", savedAsset.getAssetId());
        } catch (Exception e) {
            log.error("Failed to index asset to Elasticsearch: {}", savedAsset.getAssetId(), e);
            // Don't fail the upload if indexing fails
        }

        return mapToAssetResponse(savedAsset);
    }

    // ✅ NEW: Create video thumbnail placeholder
    private void createVideoThumbnailPlaceholder(String outputPath) throws IOException {
        int width = 640;
        int height = 360;

        BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background gradient (purple to blue)
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(88, 86, 214),
                width, height, new Color(41, 98, 255)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        // Draw play button circle
        int iconSize = 100;
        int iconX = (width - iconSize) / 2;
        int iconY = (height - iconSize) / 2;

        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillOval(iconX, iconY, iconSize, iconSize);

        // Draw play triangle
        g2d.setColor(new Color(88, 86, 214));
        int[] xPoints = {iconX + iconSize / 3, iconX + iconSize / 3, iconX + iconSize * 2 / 3};
        int[] yPoints = {iconY + iconSize / 4, iconY + iconSize * 3 / 4, iconY + iconSize / 2};
        g2d.fillPolygon(xPoints, yPoints, 3);

        // Add "VIDEO" text
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        String text = "VIDEO";
        FontMetrics metrics = g2d.getFontMetrics();
        int textX = (width - metrics.stringWidth(text)) / 2;
        int textY = height - 25;

        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.drawString(text, textX + 2, textY + 2);
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, textX, textY);

        g2d.dispose();

        File outputFile = new File(outputPath);
        ImageIO.write(thumbnail, "jpg", outputFile);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponse getAssetById(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));
        return mapToAssetResponse(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponse> getAllAssets(Pageable pageable) {
        return assetRepository.findAll(pageable)
                .map(this::mapToAssetResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponse> getAssetsByType(AssetType type, Pageable pageable) {
        return assetRepository.findByType(type, pageable)
                .map(this::mapToAssetResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponse> getAssetsByStatus(AssetStatus status, Pageable pageable) {
        return assetRepository.findByStatus(status, pageable)
                .map(this::mapToAssetResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponse> searchAssets(String query, Pageable pageable) {
        return assetRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        query, query, pageable)
                .map(this::mapToAssetResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponse> getAssetsByCurrentUser(PageRequest pageRequest) {
        User currentUser = userService.getCurrentUser();
        return assetRepository.findByUser_UserId(currentUser.getUserId(), pageRequest)
                .map(this::mapToAssetResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponse> getApprovedAssets(Pageable pageable) {
        return assetRepository.findByStatus(AssetStatus.APPROVED, pageable)
                .map(this::mapToAssetResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponse> getApprovedAssetsByType(AssetType type, Pageable pageable) {
        return assetRepository.findByStatusAndType(AssetStatus.APPROVED, type, pageable)
                .map(this::mapToAssetResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponse getApprovedAssetById(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));

        if (asset.getStatus() != AssetStatus.APPROVED) {
            throw new CustomExceptions.UnauthorizedException(
                    "This asset is not publicly available");
        }

        return mapToAssetResponse(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadApprovedAsset(Long assetId) throws IOException {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));

        if (asset.getStatus() != AssetStatus.APPROVED) {
            throw new CustomExceptions.UnauthorizedException(
                    "This asset is not publicly available");
        }

        Path filePath = Paths.get(asset.getFileUrl());
        if (!Files.exists(filePath)) {
            throw new CustomExceptions.ResourceNotFoundException(
                    "File not found: " + asset.getFileUrl());
        }
        return Files.readAllBytes(filePath);
    }

    // ✅ NEW: Get all assets that are NOT pending (approved + rejected)
    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponse> getNonPendingAssets(Pageable pageable) {
        return assetRepository.findByStatusNot(AssetStatus.PENDING, pageable)
                .map(this::mapToAssetResponse);
    }

    // ✅ NEW: Get all assets that are approved OR pending
    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponse> getApprovedAndPendingAssets(Pageable pageable) {
        List<AssetStatus> statuses = Arrays.asList(AssetStatus.APPROVED, AssetStatus.PENDING);
        return assetRepository.findByStatusIn(statuses, pageable)
                .map(this::mapToAssetResponse);
    }

    @Override
    @Transactional
    public AssetResponse updateAsset(Long assetId, AssetUpdateRequest request) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));

        User currentUser = userService.getCurrentUser();

        if (!asset.getUser().getUserId().equals(currentUser.getUserId()) &&
                !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new CustomExceptions.UnauthorizedException(
                    "You don't have permission to update this asset");
        }

        if (request.getTitle() != null) {
            asset.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            asset.setDescription(request.getDescription());
        }

        Asset updatedAsset = assetRepository.save(asset);

        // ⭐ AUTO-UPDATE ELASTICSEARCH INDEX
        try {
            assetSearchService.updateAssetIndex(updatedAsset);
            log.info("Asset index updated in Elasticsearch: {}", updatedAsset.getAssetId());
        } catch (Exception e) {
            log.error("Failed to update asset index in Elasticsearch: {}", updatedAsset.getAssetId(), e);
        }

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

        // ⭐ AUTO-UPDATE ELASTICSEARCH INDEX
        try {
            assetSearchService.updateAssetIndex(savedAsset);
            log.info("Approved asset index updated in Elasticsearch: {}", savedAsset.getAssetId());
        } catch (Exception e) {
            log.error("Failed to update approved asset index in Elasticsearch: {}", savedAsset.getAssetId(), e);
        }

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

        // ⭐ AUTO-UPDATE ELASTICSEARCH INDEX
        try {
            assetSearchService.updateAssetIndex(savedAsset);
            log.info("Rejected asset index updated in Elasticsearch: {}", savedAsset.getAssetId());
        } catch (Exception e) {
            log.error("Failed to update rejected asset index in Elasticsearch: {}", savedAsset.getAssetId(), e);
        }

        return mapToAssetResponse(savedAsset);
    }

    @Override
    @Transactional
    public void deleteAsset(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));

        User currentUser = userService.getCurrentUser();

        if (!asset.getUser().getUserId().equals(currentUser.getUserId()) &&
                !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new CustomExceptions.UnauthorizedException(
                    "You don't have permission to delete this asset");
        }

        asset.setIsActive(false);
        assetRepository.save(asset);

        // ⭐ AUTO-DELETE FROM ELASTICSEARCH (or update to mark as inactive)
        try {
            // Option 1: Delete from index completely
            // assetSearchService.deleteAssetIndex(assetId);

            // Option 2: Update index to mark as inactive (recommended to keep search history)
            assetSearchService.updateAssetIndex(asset);
            log.info("Asset marked as inactive in Elasticsearch: {}", assetId);
        } catch (Exception e) {
            log.error("Failed to update asset in Elasticsearch: {}", assetId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadAsset(Long assetId) throws IOException {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));

        Path filePath = Paths.get(asset.getFileUrl());
        if (!Files.exists(filePath)) {
            throw new CustomExceptions.ResourceNotFoundException(
                    "File not found: " + asset.getFileUrl());
        }
        return Files.readAllBytes(filePath);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getThumbnail(Long assetId) throws IOException {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Asset not found with id: " + assetId));

        if (asset.getStatus() != AssetStatus.APPROVED) {
            throw new CustomExceptions.UnauthorizedException(
                    "This asset is not publicly available");
        }

        // Check if thumbnail exists
        if (asset.getThumbnailUrl() != null && !asset.getThumbnailUrl().isEmpty()) {
            Path thumbnailPath = Paths.get(asset.getThumbnailUrl());
            if (Files.exists(thumbnailPath)) {
                log.debug("Loading thumbnail from: {}", thumbnailPath);
                return Files.readAllBytes(thumbnailPath);
            } else {
                log.warn("Thumbnail file not found: {}, will generate on-the-fly", thumbnailPath);
            }
        }

        // ✅ FIXED: Generate thumbnail on-the-fly for videos without thumbnails
        if (asset.getType() == AssetType.VIDEO) {
            log.info("Generating video thumbnail on-the-fly for asset: {}", assetId);

            // Create temp thumbnail file
            Path thumbnailDir = Paths.get(uploadDir, "thumbnails");
            if (!Files.exists(thumbnailDir)) {
                Files.createDirectories(thumbnailDir);
            }

            String tempThumbnailPath = thumbnailDir.resolve("temp_video_" + assetId + ".jpg").toString();
            createVideoThumbnailPlaceholder(tempThumbnailPath);

            // Read and return the generated thumbnail
            byte[] thumbnailData = Files.readAllBytes(Paths.get(tempThumbnailPath));

            // Optionally save this thumbnail to database for future use
            try {
                String permanentThumbnailPath = thumbnailDir.resolve("thumb_video_" + assetId + ".jpg").toString();
                Files.move(Paths.get(tempThumbnailPath), Paths.get(permanentThumbnailPath));

                // Update asset with thumbnail path
                asset.setThumbnailUrl(permanentThumbnailPath);
                assetRepository.save(asset);

                log.info("Video thumbnail saved permanently for asset: {}", assetId);
            } catch (Exception e) {
                log.warn("Could not save thumbnail permanently: {}", e.getMessage());
            }

            return thumbnailData;
        }

        // Fallback: return the original image if no thumbnail (only for images)
        if (asset.getType() == AssetType.IMAGE) {
            Path filePath = Paths.get(asset.getFileUrl());
            if (!Files.exists(filePath)) {
                throw new CustomExceptions.ResourceNotFoundException(
                        "File not found: " + asset.getFileUrl());
            }
            return Files.readAllBytes(filePath);
        }

        // For audio or other types without thumbnails
        throw new CustomExceptions.ResourceNotFoundException(
                "Thumbnail not available for this asset type");
    }

    private AssetResponse mapToAssetResponse(Asset asset) {
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
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .profilePictureUrl(user.getProfilePictureUrl())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}