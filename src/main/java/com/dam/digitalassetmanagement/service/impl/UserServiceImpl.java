package com.dam.digitalassetmanagement.service.impl;

import com.dam.digitalassetmanagement.dto.request.ResetPasswordRequest;
import com.dam.digitalassetmanagement.dto.response.OtpResponse;
import com.dam.digitalassetmanagement.repository.UserRepository;
import com.dam.digitalassetmanagement.security.JwtTokenProvider;
import com.dam.digitalassetmanagement.dto.request.LoginRequest;
import com.dam.digitalassetmanagement.dto.request.ProfileUpdateRequest;
import com.dam.digitalassetmanagement.dto.request.UserRegistrationRequest;
import com.dam.digitalassetmanagement.dto.response.AuthResponse;
import com.dam.digitalassetmanagement.dto.response.UserResponse;
import com.dam.digitalassetmanagement.entity.User;
import com.dam.digitalassetmanagement.exception.CustomExceptions;
import com.dam.digitalassetmanagement.service.FileStorageService;
import com.dam.digitalassetmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final FileStorageService fileStorageService;
    private final OtpService otpService;
    private final EmailService emailService;

    @Override
    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new CustomExceptions.DuplicateResourceException(
                    "Username already exists: " + request.getUsername());
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomExceptions.DuplicateResourceException(
                    "Email already exists: " + request.getEmail());
        }

        // Convert role to String and normalize to uppercase
        String role = request.getRole();
        if (role == null || role.trim().isEmpty()) {
            role = "VIEWER";
        } else {
            role = role.toUpperCase();
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        System.out.println("\n════════════════════════════════════════");
        System.out.println("🔑 LOGIN REQUEST");
        System.out.println("════════════════════════════════════════");
        System.out.println("Username: " + request.getUsername());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            System.out.println("✅ Authentication successful");

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Fetch User from database
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                            "User not found: " + request.getUsername()));

            System.out.println("✅ User found in database:");
            System.out.println("   User ID: " + user.getUserId());
            System.out.println("   Username: " + user.getUsername());
            System.out.println("   Email: " + user.getEmail());
            System.out.println("   Role: " + user.getRole());

            // Extract roles from authentication
            String roles = authentication.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .collect(Collectors.joining(","));

            System.out.println("   Roles: " + roles);

            // Generate token WITH userId
            System.out.println("\n🔐 Generating JWT token with userId...");
            String token = jwtTokenProvider.generateTokenWithUserId(
                    user.getUsername(),
                    user.getUserId(),
                    roles
            );

            System.out.println("✅ Token generated successfully!");

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            System.out.println("✅ Last login updated");
            System.out.println("\n✅ LOGIN SUCCESSFUL");
            System.out.println("════════════════════════════════════════\n");

            return new AuthResponse(token, mapToUserResponse(user));

        } catch (Exception e) {
            System.out.println("\n❌ LOGIN FAILED");
            System.out.println("Exception Type: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
            e.printStackTrace();
            System.out.println("════════════════════════════════════════\n");
            throw e;
        }
    }

    @Override
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "User not found with id: " + userId));
        return mapToUserResponse(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "User not found: " + username));
        return mapToUserResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "User not found with id: " + userId));

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if email is already taken by another user
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new CustomExceptions.DuplicateResourceException(
                        "Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        // Update password if current password is provided and valid
        if (request.getCurrentPassword() != null && !request.getCurrentPassword().isEmpty()) {
            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new CustomExceptions.ValidationException("Current password is incorrect");
            }

            // Update to new password if provided
            if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
                user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            }
        }

        // Update full name if provided
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        // Update phone if provided
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse uploadProfilePicture(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "User not found with id: " + userId));

        // Delete old profile picture if exists
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            try {
                fileStorageService.deleteProfilePicture(user.getProfilePictureUrl());
            } catch (Exception e) {
                System.err.println("Failed to delete old profile picture: " + e.getMessage());
            }
        }

        // Store new profile picture
        String profilePictureUrl = fileStorageService.storeProfilePicture(file, userId);
        user.setProfilePictureUrl(profilePictureUrl);

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UserRegistrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "User not found with id: " + userId));

        // Check email uniqueness if changing
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new CustomExceptions.DuplicateResourceException(
                    "Email already exists: " + request.getEmail());
        }

        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // Convert role to String and normalize to uppercase
        String role = request.getRole();
        if (role != null && !role.trim().isEmpty()) {
            user.setRole(role.toUpperCase());
        }

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "User not found with id: " + userId));

        // Delete profile picture if exists
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            try {
                fileStorageService.deleteProfilePicture(user.getProfilePictureUrl());
            } catch (Exception e) {
                System.err.println("Failed to delete profile picture: " + e.getMessage());
            }
        }

        userRepository.deleteById(userId);
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "Current user not found: " + username));
    }

    // ========================================
    // FORGOT PASSWORD / OTP METHODS
    // ========================================

    @Override
    @Transactional
    public OtpResponse sendPasswordResetOtp(String email) {
        // Generate and send OTP
        otpService.generateAndSendOtp(email);

        return OtpResponse.builder()
                .message("OTP has been sent to your email")
                .email(email)
                .expiresInMinutes(otpService.getOtpExpirationMinutes())
                .build();
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String otpCode) {
        return otpService.verifyOtp(email, otpCode);
    }

    @Override
    @Transactional
    public UserResponse resetPassword(ResetPasswordRequest request) {
        // Validate OTP
        otpService.validateOtpForReset(request.getEmail(), request.getOtpCode());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "User not found with email: " + request.getEmail()));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        User updatedUser = userRepository.save(user);

        // Send password changed notification
        emailService.sendPasswordChangedNotification(user.getEmail(), user.getUsername());

        return mapToUserResponse(updatedUser);
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