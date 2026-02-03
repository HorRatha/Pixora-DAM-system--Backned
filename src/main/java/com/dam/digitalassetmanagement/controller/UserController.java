package com.dam.digitalassetmanagement.controller;

import com.dam.digitalassetmanagement.dto.request.LoginRequest;
import com.dam.digitalassetmanagement.dto.request.ProfileUpdateRequest;
import com.dam.digitalassetmanagement.dto.request.UserRegistrationRequest;
import com.dam.digitalassetmanagement.dto.response.AuthResponse;
import com.dam.digitalassetmanagement.dto.response.UserResponse;
import com.dam.digitalassetmanagement.service.FileStorageService;
import com.dam.digitalassetmanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User registration, authentication, and profile management")
public class UserController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        UserResponse user = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request) {

        UserResponse user = userService.getUserByUsername(userDetails.getUsername());
        UserResponse updated = userService.updateUserProfile(user.getUserId(), request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload profile picture",
            description = "Upload a profile picture for the current user. Accepts image files (jpg, png, gif) up to 5MB."
    )
    public ResponseEntity<UserResponse> uploadProfilePicture(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(
                    description = "Profile picture file (jpg, png, gif - max 5MB)",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file) {

        UserResponse user = userService.getUserByUsername(userDetails.getUsername());
        UserResponse updated = userService.uploadProfilePicture(user.getUserId(), file);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/profile-picture/{filename}")
    @Operation(summary = "Get profile picture", description = "Retrieve a profile picture by filename")
    public ResponseEntity<byte[]> getProfilePicture(
            @Parameter(description = "Profile picture filename", required = true)
            @PathVariable String filename) {

        byte[] image = fileStorageService.loadProfilePicture(filename);

        // Determine content type based on file extension
        String contentType = MediaType.IMAGE_JPEG_VALUE;
        if (filename.toLowerCase().endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        } else if (filename.toLowerCase().endsWith(".gif")) {
            contentType = MediaType.IMAGE_GIF_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(image);
    }

    @DeleteMapping("/me/profile-picture")
    @Operation(summary = "Delete profile picture", description = "Delete the current user's profile picture")
    public ResponseEntity<Map<String, String>> deleteProfilePicture(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserResponse user = userService.getUserByUsername(userDetails.getUsername());

        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            fileStorageService.deleteProfilePicture(user.getProfilePictureUrl());

            // Update user to remove profile picture URL
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            userService.updateUserProfile(user.getUserId(), request);

            return ResponseEntity.ok(Map.of("message", "Profile picture deleted successfully"));
        }

        return ResponseEntity.ok(Map.of("message", "No profile picture to delete"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users (Admin only)")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID (Admin only)")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user (Admin only)")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserRegistrationRequest request) {

        UserResponse user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user role (Admin only)")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long userId,
            // ✅ FIXED: Changed from UserRole enum to String
            // Valid values: "ADMIN", "UPLOADER", "EDITOR", "VIEWER"
            @RequestParam String role) {

        UserResponse currentUser = userService.getUserById(userId);
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername(currentUser.getUsername());
        request.setEmail(currentUser.getEmail());
        request.setRole(role.toUpperCase());

        UserResponse user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user (Admin only)")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }
}