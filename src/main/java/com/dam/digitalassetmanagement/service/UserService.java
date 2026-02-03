package com.dam.digitalassetmanagement.service;

import com.dam.digitalassetmanagement.dto.request.LoginRequest;
import com.dam.digitalassetmanagement.dto.request.ProfileUpdateRequest;
import com.dam.digitalassetmanagement.dto.request.ResetPasswordRequest;
import com.dam.digitalassetmanagement.dto.request.UserRegistrationRequest;
import com.dam.digitalassetmanagement.dto.response.AuthResponse;
import com.dam.digitalassetmanagement.dto.response.OtpResponse;
import com.dam.digitalassetmanagement.dto.response.UserResponse;
import com.dam.digitalassetmanagement.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserResponse registerUser(UserRegistrationRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getUserById(Long userId);
    UserResponse getUserByUsername(String username);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(Long userId, UserRegistrationRequest request);
    UserResponse updateUserProfile(Long userId, ProfileUpdateRequest request);
    UserResponse uploadProfilePicture(Long userId, MultipartFile file);
    void deleteUser(Long userId);
    User getCurrentUser();

    // ✅ NEW METHODS FOR FORGOT PASSWORD
    OtpResponse sendPasswordResetOtp(String email);
    boolean verifyOtp(String email, String otpCode);
    UserResponse resetPassword(ResetPasswordRequest request);
}