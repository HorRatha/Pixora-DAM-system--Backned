package com.dam.digitalassetmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // ✅ CRITICAL: MUST BE STRING, NOT @Enumerated(UserRole)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String role = "VIEWER";

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "profile_picture_url", length = 255)
    private String profilePictureUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Asset> assets = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<AssetCollection> assetCollections = new HashSet<>();

    // ✅ Helper methods for role checking
    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }

    public boolean isEditor() {
        return "EDITOR".equals(this.role);
    }

    public boolean isUploader() {
        return "UPLOADER".equals(this.role);
    }

    public boolean isViewer() {
        return "VIEWER".equals(this.role);
    }
}