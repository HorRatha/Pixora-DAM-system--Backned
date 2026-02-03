package com.dam.digitalassetmanagement.service;

import com.dam.digitalassetmanagement.dto.WebSocketMessage;
import com.dam.digitalassetmanagement.dto.request.CommentRequest;
import com.dam.digitalassetmanagement.entity.Comment;
import com.dam.digitalassetmanagement.entity.Asset;
import com.dam.digitalassetmanagement.repository.CommentRepository;
import com.dam.digitalassetmanagement.repository.UserRepository;
import com.dam.digitalassetmanagement.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Add a new comment
     */
    @Transactional
    public Comment addComment(CommentRequest request, Long userId) {
        System.out.println("\n\n");
        System.out.println("════════════════════════════════════════");
        System.out.println("📝 COMMENT SERVICE - addComment()");
        System.out.println("════════════════════════════════════════");
        System.out.println("Asset ID: " + request.getAssetId());
        System.out.println("User ID: " + userId);
        System.out.println("Parent ID: " + request.getParentId());
        System.out.println("Content: " + request.getContent());

        try {
            log.info("📝 Adding comment for asset: {} by user: {}, parentId: {}",
                    request.getAssetId(), userId, request.getParentId());

            // ✅ FIXED: Validate if user is allowed to reply
            if (request.getParentId() != null) {
                System.out.println("\n🔐 This is a REPLY - validating authorization...");
                validateReplyAuthorization(request.getAssetId(), request.getParentId(), userId);
                System.out.println("✅ Reply authorization passed!");
            }

            System.out.println("\n📌 Creating comment object...");
            Comment comment = new Comment();
            comment.setAssetId(request.getAssetId());
            comment.setUserId(userId);

            // ✅ FIXED: Only set anonymousId if userId is NULL
            if (userId == null) {
                comment.setAnonymousId(request.getAnonymousId());
                System.out.println("✅ Anonymous comment - AnonymousId set: " + request.getAnonymousId());
            } else {
                comment.setAnonymousId(null);  // ✅ Clear it when user is logged in!
                System.out.println("✅ Logged-in comment - AnonymousId cleared (userId: " + userId + ")");
            }

            comment.setContent(request.getContent());
            comment.setParentId(request.getParentId());
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());
            comment.setIsEdited(false);
            comment.setIsDeleted(false);

            // Set username before saving
            String username = "Anonymous";
            if (userId != null) {
                var user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    username = user.getUsername();
                    System.out.println("✅ Found user: " + username);
                } else {
                    System.out.println("⚠️ User not found in DB, using Anonymous");
                }
            } else if (request.getUsername() != null) {
                username = request.getUsername();
                System.out.println("✅ Using provided username: " + username);
            }
            comment.setUsername(username);

            // Save and broadcast
            System.out.println("\n💾 Saving comment to database...");
            comment = commentRepository.save(comment);
            System.out.println("✅ Comment saved with ID: " + comment.getId());
            log.info("✅ Comment added with ID: {}", comment.getId());

            // Mark if commenter is post owner
            markAuthor(comment);

            // Broadcast to WebSocket
            broadcastCommentUpdate(comment, "ADD");

            System.out.println("✅ COMMENT CREATED SUCCESSFULLY");
            System.out.println("════════════════════════════════════════\n\n");

            return comment;

        } catch (Exception e) {
            System.out.println("\n❌ ERROR IN addComment():");
            System.out.println("Exception Type: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("Stack trace:");
            e.printStackTrace();
            System.out.println("════════════════════════════════════════\n\n");
            throw new RuntimeException("Comment creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ FIXED: Validate reply authorization - Only POST OWNER can reply
     */
    private void validateReplyAuthorization(Long assetId, Long parentCommentId, Long userId) {
        System.out.println("\n");
        System.out.println("════════════════════════════════════════");
        System.out.println("🔐 VALIDATING REPLY AUTHORIZATION");
        System.out.println("════════════════════════════════════════");
        System.out.println("Asset ID: " + assetId);
        System.out.println("Parent Comment ID: " + parentCommentId);
        System.out.println("User ID: " + userId);

        try {
            // Check 1: User must be logged in
            if (userId == null || userId <= 0) {
                System.out.println("❌ FAIL: User not logged in (userId=" + userId + ")");
                log.warn("❌ Reply blocked - User not logged in");
                throw new RuntimeException("Please login to reply to comments");
            }
            System.out.println("✅ Step 1 PASS: User is logged in");

            // Check 2: Asset must exist
            System.out.println("🔍 Step 2: Checking if asset exists...");
            Asset asset = assetRepository.findById(assetId)
                    .orElseThrow(() -> {
                        System.out.println("❌ FAIL: Asset not found");
                        return new RuntimeException("Asset not found");
                    });
            System.out.println("✅ Step 2 PASS: Asset exists");

            // Check 3: Parent comment must exist
            System.out.println("🔍 Step 3: Checking if parent comment exists...");
            Comment parentComment = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> {
                        System.out.println("❌ FAIL: Parent comment not found");
                        return new RuntimeException("Parent comment not found");
                    });
            System.out.println("✅ Step 3 PASS: Parent comment exists");

            // Check 4: User MUST be the POST OWNER
            System.out.println("🔍 Step 4: Checking if user is the POST OWNER...");
            if (asset.getUser() == null) {
                System.out.println("❌ FAIL: Asset has no owner");
                throw new RuntimeException("Asset has no owner");
            }

            Long postOwnerId = asset.getUser().getUserId();
            System.out.println("  Post Owner ID: " + postOwnerId);
            System.out.println("  Current User ID: " + userId);

            // ✅ CRITICAL: This check was missing!
            if (!postOwnerId.equals(userId)) {
                System.out.println("❌ FAIL: Only post owner can reply!");
                System.out.println("  Expected: " + postOwnerId);
                System.out.println("  Got: " + userId);
                System.out.println("════════════════════════════════════════\n");
                log.warn("❌ Reply blocked - User {} is not the post owner {}", userId, postOwnerId);
                throw new RuntimeException("Only the post owner can reply to comments");
            }

            System.out.println("✅ Step 4 PASS: User IS the post owner");
            System.out.println("✅ ALL CHECKS PASSED - Authorization successful!");
            System.out.println("════════════════════════════════════════\n");
            log.info("✅ Reply authorization passed for user {} on asset {}", userId, assetId);

        } catch (RuntimeException e) {
            System.out.println("❌ Authorization FAILED: " + e.getMessage());
            System.out.println("════════════════════════════════════════\n");
            log.error("❌ Reply authorization failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            System.out.println("❌ Unexpected error: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
            e.printStackTrace();
            System.out.println("════════════════════════════════════════\n");
            log.error("❌ Unexpected error in authorization: {}", e.getMessage(), e);
            throw new RuntimeException("Authorization error: " + e.getMessage());
        }
    }

    /**
     * Get all comments for an asset with nested replies
     */
    public List<Comment> getCommentsByAsset(Long assetId) {
        log.info("📥 Loading comments for asset: {}", assetId);

        // Get top-level comments
        List<Comment> topLevelComments = commentRepository
                .findByAssetIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(assetId);

        log.info("✅ Found {} top-level comments", topLevelComments.size());

        // Load replies for each comment
        for (Comment comment : topLevelComments) {
            markAuthor(comment);
            loadReplies(comment);
        }

        return topLevelComments;
    }

    /**
     * Recursively load replies for a comment
     */
    private void loadReplies(Comment comment) {
        List<Comment> replies = commentRepository
                .findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(comment.getId());

        comment.setReplies(replies);

        // Count all nested replies recursively
        int totalReplyCount = replies.size();

        // Recursively load replies for nested comments and count them
        for (Comment reply : replies) {
            markAuthor(reply);
            loadReplies(reply);
            totalReplyCount += reply.getReplyCount();
        }

        comment.setReplyCount(totalReplyCount);

        if (totalReplyCount > 0) {
            log.debug("💬 Comment {} has {} replies", comment.getId(), totalReplyCount);
        }
    }

    /**
     * Mark if commenter is the post owner
     */
    private void markAuthor(Comment comment) {
        if (comment.getUserId() == null) {
            comment.setIsAuthor(false);
            return;
        }

        try {
            // Get the asset/post
            Asset asset = assetRepository.findById(comment.getAssetId()).orElse(null);

            if (asset != null && asset.getUser() != null) {
                // ✅ Compare with asset.getUser().getUserId()
                boolean isAuthor = comment.getUserId().equals(asset.getUser().getUserId());
                comment.setIsAuthor(isAuthor);

                if (isAuthor) {
                    log.debug("👤 Comment {} is by post author", comment.getId());
                }
            } else {
                comment.setIsAuthor(false);
            }
        } catch (Exception e) {
            log.error("❌ Error marking author for comment {}: {}", comment.getId(), e.getMessage());
            comment.setIsAuthor(false);
        }
    }

    /**
     * Update a comment
     */
    @Transactional
    public Comment updateComment(Long commentId, String newContent, Long userId) {
        log.info("✏️ Updating comment: {} by user: {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Check if user owns the comment
        if (!comment.getUserId().equals(userId)) {
            log.warn("❌ Update failed - User {} is not the comment author", userId);
            throw new RuntimeException("Unauthorized: You can only edit your own comments");
        }

        comment.setContent(newContent);
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setIsEdited(true);

        comment = commentRepository.save(comment);
        log.info("✅ Comment {} updated", commentId);

        // Mark author before broadcasting
        markAuthor(comment);

        // Broadcast update
        broadcastCommentUpdate(comment, "UPDATE");

        return comment;
    }

    /**
     * Delete a comment (soft delete)
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        log.info("🗑️ Deleting comment: {} by user: {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Check if user owns the comment
        if (!comment.getUserId().equals(userId)) {
            log.warn("❌ Delete failed - User {} is not the comment author", userId);
            throw new RuntimeException("Unauthorized: You can only delete your own comments");
        }

        comment.setIsDeleted(true);
        comment.setContent("[Deleted]");
        commentRepository.save(comment);

        log.info("✅ Comment {} deleted by user {}", commentId, userId);

        // Broadcast deletion
        broadcastCommentUpdate(comment, "DELETE");
    }

    /**
     * Get comment count for an asset (excluding deleted)
     */
    public Long getCommentCount(Long assetId) {
        Long count = commentRepository.countByAssetIdAndIsDeletedFalse(assetId);
        log.debug("📊 Comment count for asset {}: {}", assetId, count);
        return count;
    }

    /**
     * Get comment by ID
     */
    public Comment getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));
        markAuthor(comment);
        return comment;
    }

    /**
     * Broadcast comment update via WebSocket
     */
    private void broadcastCommentUpdate(Comment comment, String action) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("COMMENT");
        message.setAssetId(comment.getAssetId());
        message.setData(comment);
        message.setAction(action);

        messagingTemplate.convertAndSend("/topic/asset/" + comment.getAssetId(), message);
        log.debug("📡 Broadcasted {} for comment {}", action, comment.getId());
    }
}