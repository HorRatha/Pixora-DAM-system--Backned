package com.dam.digitalassetmanagement.controller;

import com.dam.digitalassetmanagement.dto.request.CommentRequest;
import com.dam.digitalassetmanagement.entity.Comment;
import com.dam.digitalassetmanagement.security.JwtTokenProvider;
import com.dam.digitalassetmanagement.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;
    private final SimpMessagingTemplate messagingTemplate;
    private final JwtTokenProvider jwtTokenProvider;  // ✅ INJECT THIS!

    @PostMapping
    public ResponseEntity<?> addComment(
            @RequestBody CommentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            System.out.println("\n\n");
            System.out.println("════════════════════════════════════════");
            System.out.println("💬 COMMENT REQUEST RECEIVED");
            System.out.println("════════════════════════════════════════");
            System.out.println("Asset ID: " + request.getAssetId());
            System.out.println("Content: " + request.getContent());
            System.out.println("Parent ID (Reply): " + request.getParentId());
            System.out.println("Authorization Header: " + (authHeader != null ? "YES" : "NO"));

            Long userId = extractUserIdFromToken(authHeader);
            System.out.println("Extracted User ID: " + userId);

            log.info("📝 Add comment request - Asset: {}, ParentId: {}, UserId: {}",
                    request.getAssetId(), request.getParentId(), userId);

            Comment comment = commentService.addComment(request, userId);

            Long newCount = commentService.getCommentCount(request.getAssetId());
            String payload = String.format("{\"assetId\": %d, \"count\": %d}", request.getAssetId(), newCount);
            messagingTemplate.convertAndSend("/topic/comments", payload);
            log.info("✅ Comment posted successfully: {}, count: {}", comment.getId(), newCount);

            System.out.println("✅ COMMENT POSTED SUCCESSFULLY");
            System.out.println("════════════════════════════════════════\n\n");

            return ResponseEntity.ok(comment);

        } catch (RuntimeException e) {
            System.out.println("❌ RUNTIME EXCEPTION:");
            System.out.println("Message: " + e.getMessage());
            System.out.println("Stack trace:");
            e.printStackTrace();
            System.out.println("════════════════════════════════════════\n\n");

            log.error("❌ Failed to add comment: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to post comment");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        } catch (Exception e) {
            System.out.println("❌ UNEXPECTED EXCEPTION:");
            System.out.println("Type: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("Stack trace:");
            e.printStackTrace();
            System.out.println("════════════════════════════════════════\n\n");

            log.error("❌ Unexpected error: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<Comment>> getComments(@PathVariable Long assetId) {
        log.info("📥 Get comments request for asset: {}", assetId);
        List<Comment> comments = commentService.getCommentsByAsset(assetId);
        log.info("✅ Returning {} comments", comments.size());
        return ResponseEntity.ok(comments);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            Long userId = extractUserIdFromToken(authHeader);

            if (userId == null) {
                log.warn("❌ Update comment failed - No user ID");
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized", "message", "Please login"));
            }

            log.info("✏️ Update comment request: {} by user: {}", commentId, userId);

            Comment comment = commentService.updateComment(commentId, request.getContent(), userId);

            Long newCount = commentService.getCommentCount(request.getAssetId());
            String payload = String.format("{\"assetId\": %d, \"count\": %d}", request.getAssetId(), newCount);
            messagingTemplate.convertAndSend("/topic/comments", payload);
            log.info("✅ Comment updated successfully: {}", commentId);

            return ResponseEntity.ok(comment);

        } catch (RuntimeException e) {
            log.error("❌ Failed to update comment: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Update failed", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            Long userId = extractUserIdFromToken(authHeader);

            if (userId == null) {
                log.warn("❌ Delete comment failed - No user ID");
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized", "message", "Please login"));
            }

            log.info("🗑️ Delete comment request: {} by user: {}", commentId, userId);

            Comment comment = commentService.getCommentById(commentId);
            if (comment == null) {
                return ResponseEntity.notFound().build();
            }

            Long assetId = comment.getAssetId();

            commentService.deleteComment(commentId, userId);

            Long newCount = commentService.getCommentCount(assetId);
            String payload = String.format("{\"assetId\": %d, \"count\": %d}", assetId, newCount);
            messagingTemplate.convertAndSend("/topic/comments", payload);
            log.info("✅ Comment deleted successfully: {}, new count: {}", commentId, newCount);

            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            log.error("❌ Failed to delete comment: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Delete failed", "message", e.getMessage()));
        }
    }

    @GetMapping("/asset/{assetId}/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable Long assetId) {
        log.debug("📊 Get comment count for asset: {}", assetId);
        Long count = commentService.getCommentCount(assetId);
        return ResponseEntity.ok(count);
    }

    /**
     * ✅ SIMPLIFIED: Extract userId from JWT token using JwtTokenProvider
     */
    private Long extractUserIdFromToken(String authHeader) {
        // Handle missing auth header
        if (authHeader == null || authHeader.isEmpty()) {
            System.out.println("⚠️ No Authorization header provided");
            return null;
        }

        // Handle invalid Bearer token format
        if (!authHeader.startsWith("Bearer ")) {
            System.out.println("⚠️ Invalid Authorization header format");
            return null;
        }

        try {
            // Extract token from "Bearer <token>"
            String token = authHeader.substring(7);
            System.out.println("🔓 Extracting userId from JWT token...");

            // ✅ USE JwtTokenProvider to extract userId
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            if (userId != null && userId > 0) {
                System.out.println("✅ Successfully extracted userId: " + userId);
                return userId;
            } else {
                System.out.println("❌ Token does not contain a valid userId!");
                System.out.println("   Make sure your token was generated with userId claim");
                return null;
            }

        } catch (Exception e) {
            System.out.println("❌ Error extracting userId from token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}