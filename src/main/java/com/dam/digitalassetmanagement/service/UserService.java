package com.dam.digitalassetmanagement.service;

import com.dam.digitalassetmanagement.dto.request.LoginRequest;
import com.dam.digitalassetmanagement.dto.request.UserRegistrationRequest;
import com.dam.digitalassetmanagement.dto.response.AuthResponse;
import com.dam.digitalassetmanagement.dto.response.UserResponse;
import com.dam.digitalassetmanagement.entity.User;

import java.util.List;

public interface UserService {
    UserResponse registerUser(UserRegistrationRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getUserById(Long userId);
    UserResponse getUserByUsername(String username);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(Long userId, UserRegistrationRequest request);
    void deleteUser(Long userId);
    User getCurrentUser();
}