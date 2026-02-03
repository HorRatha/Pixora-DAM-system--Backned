package com.dam.digitalassetmanagement.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeProfilePicture(MultipartFile file, Long userId);
    void deleteProfilePicture(String fileUrl);
    byte[] loadProfilePicture(String filename);
}