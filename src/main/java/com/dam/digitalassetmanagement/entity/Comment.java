package com.dam.digitalassetmanagement.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_asset_id", columnList = "asset_id"),
        @Index(name = "idx_parent_id", columnList = "parent_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "anonymous_id", length = 255)
    private String anonymousId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Column(name = "is_edited", nullable = false)
    private Boolean isEdited = false;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // ✅ Nested replies (populated at runtime)
    @Transient
    private List<Comment> replies = new ArrayList<>();

    // ✅ Reply count (calculated recursively)
    @Transient
    private Integer replyCount = 0;

    // ✅ Mark if commenter is the post owner
    @Transient
    private Boolean isAuthor = false;

    // ✅ ADDED: Helper method to check if this is a reply
    @Transient
    @JsonIgnore
    public boolean isReply() {
        return parentId != null;
    }

    // ✅ ADDED: Helper method to check if comment has replies
    @Transient
    @JsonIgnore
    public boolean hasReplies() {
        return replies != null && !replies.isEmpty();
    }

    // ✅ ADDED: Lifecycle callbacks for automatic timestamp management
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isEdited == null) {
            isEdited = false;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ✅ OPTIONAL: Override toString to prevent circular reference issues
    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", assetId=" + assetId +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", content='" + (content != null && content.length() > 50 ?
                content.substring(0, 50) + "..." : content) + '\'' +
                ", parentId=" + parentId +
                ", isDeleted=" + isDeleted +
                ", replyCount=" + replyCount +
                ", isAuthor=" + isAuthor +
                '}';
    }
}