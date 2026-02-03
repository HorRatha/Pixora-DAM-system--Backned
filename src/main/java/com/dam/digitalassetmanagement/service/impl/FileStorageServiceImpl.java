package com.dam.digitalassetmanagement.service.impl;

import com.dam.digitalassetmanagement.exception.CustomExceptions;
import com.dam.digitalassetmanagement.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final Path profilePictureStorageLocation;

    public FileStorageServiceImpl(@Value("${file.upload-dir:uploads/profiles}") String uploadDir) {
        this.profilePictureStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.profilePictureStorageLocation);
            log.info("Profile picture storage directory created at: {}", this.profilePictureStorageLocation);
        } catch (Exception ex) {
            throw new CustomExceptions.FileStorageException("Could not create upload directory", ex);
        }
    }

    @Override
    public String storeProfilePicture(MultipartFile file, Long userId) {
        // Validate file
        if (file.isEmpty()) {
            throw new CustomExceptions.FileStorageException("Cannot store empty file");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomExceptions.FileStorageException("Only image files are allowed");
        }

        // Validate file size (e.g., max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new CustomExceptions.FileStorageException("File size exceeds maximum limit of 5MB");
        }

        try {
            // Create unique filename
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = "user_" + userId + "_" + UUID.randomUUID() + fileExtension;

            // Store file
            Path targetLocation = this.profilePictureStorageLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("Profile picture stored successfully: {}", newFilename);
            return "/api/users/profile-picture/" + newFilename;

        } catch (IOException ex) {
            throw new CustomExceptions.FileStorageException("Could not store profile picture", ex);
        }
    }

    @Override
    public void deleteProfilePicture(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path filePath = this.profilePictureStorageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
            log.info("Profile picture deleted: {}", filename);
        } catch (IOException ex) {
            log.error("Could not delete profile picture: {}", fileUrl, ex);
        }
    }

    @Override
    public byte[] loadProfilePicture(String filename) {
        try {
            Path filePath = this.profilePictureStorageLocation.resolve(filename).normalize();
            if (!Files.exists(filePath)) {
                throw new CustomExceptions.FileStorageException("File not found: " + filename);
            }
            return Files.readAllBytes(filePath);
        } catch (IOException ex) {
            throw new CustomExceptions.FileStorageException("Could not load profile picture: " + filename, ex);
        }
    }
}