package com.dam.digitalassetmanagement.entity;

import com.dam.digitalassetmanagement.enums.RequestStatus;
import com.dam.digitalassetmanagement.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ✅ FIXED: Changed to VARCHAR instead of ENUM (column name matches DB: user_current_role)
    @Column(name = "user_current_role", nullable = false, length = 50)
    private String userCurrentRole;

    // ✅ FIXED: Changed to VARCHAR instead of ENUM (column name matches DB: requested_role)
    @Column(name = "requested_role", nullable = false, length = 50)
    private String requestedRole;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    // ✅ FIXED: Changed to VARCHAR instead of ENUM (Hibernate @Enumerated causes issues with PostgreSQL ENUM)
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ Optional: Add validation helper methods
    public boolean isPending() {
        return "PENDING".equals(this.status);
    }

    public boolean isApproved() {
        return "APPROVED".equals(this.status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(this.status);
    }
}