package com.dam.digitalassetmanagement.entity;

import com.dam.digitalassetmanagement.enums.AssetStatus;
import com.dam.digitalassetmanagement.enums.AssetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long assetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ✅ FIXED: Add columnDefinition and JdbcTypeCode like status field
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "asset_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AssetType type;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ✅ This was already correct
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "asset_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private AssetStatus status = AssetStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<AssetMetadata> metadata = new HashSet<>();

    @ManyToMany(mappedBy = "assets")
    @Builder.Default
    private Set<AssetCollection> assetCollections = new HashSet<>();

    // ✅ Social Features Fields with @Builder.Default
    @Column(name = "total_reactions")
    @Builder.Default
    private Integer totalReactions = 0;

    @Column(name = "total_comments")
    @Builder.Default
    private Integer totalComments = 0;

    @Column(name = "total_views")
    @Builder.Default
    private Integer totalViews = 0;

    @Column(name = "unique_viewers")
    @Builder.Default
    private Integer uniqueViewers = 0;
}