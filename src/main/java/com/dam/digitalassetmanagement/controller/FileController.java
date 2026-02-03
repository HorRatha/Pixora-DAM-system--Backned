package com.dam.digitalassetmanagement.controller;

import com.dam.digitalassetmanagement.exception.CustomExceptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads")
@Slf4j
@CrossOrigin(origins = "*") // Allow Android app to access
public class FileController {

    @Value("${app.upload-dir}")
    private String uploadDir;

    /**
     * Serve files from /uploads/**
     *
     * Examples:
     * - /uploads/abc123.png → Serves main image
     * - /uploads/thumbnails/thumb_abc123.png → Serves thumbnail
     */
    @GetMapping("/**")
    public ResponseEntity<Resource> serveFile(@RequestParam(required = false) String path) {
        try {
            // Extract the full path after /uploads/
            String requestPath = path != null ? path : "";

            // Resolve the file path
            Path filePath = Paths.get(uploadDir).resolve(requestPath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.error("File not found or not readable: {}", filePath);
                throw new CustomExceptions.ResourceNotFoundException("File not found: " + requestPath);
            }

            // Detect content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            log.info("Serving file: {} (type: {})", filePath, contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("Malformed URL: {}", e.getMessage());
            throw new CustomExceptions.BadRequestException("Invalid file path");
        } catch (IOException e) {
            log.error("Error reading file: {}", e.getMessage());
            throw new CustomExceptions.ResourceNotFoundException("File not found");
        }
    }
}