package com.dam.digitalassetmanagement.util;

import com.dam.digitalassetmanagement.enums.AssetType;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileUtil {

    private static final Tika tika = new Tika();

    public String generateUniqueFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + extension;
    }

    public String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public String detectMimeType(MultipartFile file) throws IOException {
        return tika.detect(file.getInputStream());
    }

    public AssetType determineAssetType(String mimeType) {
        if (mimeType.startsWith("image/")) {
            return AssetType.IMAGE;
        } else if (mimeType.startsWith("video/")) {
            return AssetType.VIDEO;
        } else if (mimeType.startsWith("audio/")) {
            return AssetType.AUDIO;
        }
        throw new IllegalArgumentException("Unsupported file type: " + mimeType);
    }

    public void createDirectoryIfNotExists(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    public boolean isImageFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public boolean isVideoFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    public boolean isAudioFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("audio/");
    }

    public String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}