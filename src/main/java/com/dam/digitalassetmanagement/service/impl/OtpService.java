package com.dam.digitalassetmanagement.service.impl;

import com.dam.digitalassetmanagement.entity.PasswordResetOtp;
import com.dam.digitalassetmanagement.entity.User;
import com.dam.digitalassetmanagement.exception.CustomExceptions;
import com.dam.digitalassetmanagement.repository.PasswordResetOtpRepository;
import com.dam.digitalassetmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final PasswordResetOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${otp.expiration-minutes:10}")
    private int otpExpirationMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${otp.max-attempts:3}")
    private int maxOtpAttempts;

    /**
     * Generate and send OTP to user's email
     */
    @Transactional
    public void generateAndSendOtp(String email) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException(
                        "No account found with email: " + email));

        // Check rate limiting (max 3 OTPs per hour)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentOtpCount = otpRepository.countByEmailAndCreatedAtAfter(email, oneHourAgo);

        if (recentOtpCount >= maxOtpAttempts) {
            throw new CustomExceptions.BadRequestException(
                    "Too many OTP requests. Please try again after 1 hour.");
        }

        // Invalidate all previous OTPs for this email
        otpRepository.invalidateAllOtpsByEmail(email);

        // Generate OTP
        String otpCode = generateOtpCode();

        // Create and save OTP entity
        PasswordResetOtp otp = PasswordResetOtp.builder()
                .userId(user.getUserId())
                .email(email)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .isUsed(false)
                .build();

        otpRepository.save(otp);

        // Send OTP via email
        emailService.sendOtpEmail(email, otpCode, user.getUsername());

        log.info("OTP generated and sent to email: {}", email);
    }

    /**
     * Verify OTP code
     */
    @Transactional
    public boolean verifyOtp(String email, String otpCode) {
        PasswordResetOtp otp = otpRepository.findByEmailAndOtpCodeAndIsUsedFalse(email, otpCode)
                .orElseThrow(() -> new CustomExceptions.ValidationException(
                        "Invalid OTP code"));

        // Check if OTP is expired
        if (otp.isExpired()) {
            throw new CustomExceptions.ValidationException(
                    "OTP has expired. Please request a new one.");
        }

        // Mark OTP as verified (but not used yet, will be used during password reset)
        otp.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(otp);

        log.info("OTP verified successfully for email: {}", email);
        return true;
    }

    /**
     * Validate OTP for password reset
     */
    @Transactional
    public void validateOtpForReset(String email, String otpCode) {
        PasswordResetOtp otp = otpRepository.findByEmailAndOtpCodeAndIsUsedFalse(email, otpCode)
                .orElseThrow(() -> new CustomExceptions.ValidationException(
                        "Invalid or expired OTP code"));

        // Check if OTP is expired
        if (otp.isExpired()) {
            throw new CustomExceptions.ValidationException(
                    "OTP has expired. Please request a new one.");
        }

        // Check if OTP was verified
        if (otp.getVerifiedAt() == null) {
            throw new CustomExceptions.ValidationException(
                    "OTP must be verified first");
        }

        // Mark OTP as used
        otp.setIsUsed(true);
        otpRepository.save(otp);

        log.info("OTP validated for password reset: {}", email);
    }

    /**
     * Generate random OTP code
     */
    private String generateOtpCode() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }

        return otp.toString();
    }

    /**
     * Scheduled task to clean up expired OTPs (runs daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpRepository.deleteExpiredOtps(now);
        log.info("Cleaned up expired OTPs");
    }

    /**
     * Get OTP expiration time in minutes
     */
    public int getOtpExpirationMinutes() {
        return otpExpirationMinutes;
    }
}