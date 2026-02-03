package com.dam.digitalassetmanagement.controller;

import com.dam.digitalassetmanagement.dto.request.ForgotPasswordRequest;
import com.dam.digitalassetmanagement.dto.request.ResetPasswordRequest;
import com.dam.digitalassetmanagement.dto.request.VerifyOtpRequest;
import com.dam.digitalassetmanagement.dto.response.OtpResponse;
import com.dam.digitalassetmanagement.dto.response.UserResponse;
import com.dam.digitalassetmanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
@Tag(name = "Password Reset", description = "Password reset and OTP verification endpoints")
public class PasswordResetController {

    private final UserService userService;

    @PostMapping("/forgot")
    @Operation(summary = "Request password reset OTP", description = "Send OTP to user's email for password reset")
    public ResponseEntity<OtpResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        OtpResponse response = userService.sendPasswordResetOtp(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP code", description = "Verify the OTP code sent to user's email")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean isValid = userService.verifyOtp(request.getEmail(), request.getOtpCode());

        Map<String, Object> response = new HashMap<>();
        response.put("success", isValid);
        response.put("message", "OTP verified successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset password", description = "Reset password using verified OTP")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        UserResponse userResponse = userService.resetPassword(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password reset successfully");
        response.put("user", userResponse);

        return ResponseEntity.ok(response);
    }
}