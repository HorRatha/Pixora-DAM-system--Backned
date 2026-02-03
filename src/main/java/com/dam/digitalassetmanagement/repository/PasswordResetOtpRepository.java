package com.dam.digitalassetmanagement.repository;

import com.dam.digitalassetmanagement.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findByEmailAndOtpCodeAndIsUsedFalse(String email, String otpCode);

    Optional<PasswordResetOtp> findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(String email);

    List<PasswordResetOtp> findByEmailAndIsUsedFalse(String email);

    @Modifying
    @Query("UPDATE PasswordResetOtp p SET p.isUsed = true WHERE p.email = :email AND p.isUsed = false")
    void invalidateAllOtpsByEmail(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM PasswordResetOtp p WHERE p.expiresAt < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);
}