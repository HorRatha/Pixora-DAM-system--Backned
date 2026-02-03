package com.dam.digitalassetmanagement.controller;

import com.dam.digitalassetmanagement.dto.request.ReactionRequest;
import com.dam.digitalassetmanagement.entity.Reaction;
import com.dam.digitalassetmanagement.service.ReactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reactions")
@RequiredArgsConstructor
@Slf4j
public class ReactionController {

    private final ReactionService reactionService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Toggle reaction (add/remove)
     * POST /api/reactions/toggle
     */
    @PostMapping("/toggle")
    public ResponseEntity<Reaction> toggleReaction(
            @RequestBody ReactionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long userId = getUserIdFromToken(authHeader);

        log.info("Toggle reaction request for asset: {}", request.getAssetId());

        Reaction reaction = reactionService.toggleReaction(request, userId);

        // ✅ FIXED: Broadcast real-time update with PROPER JSON STRING FORMAT
        Long newCount = reactionService.getReactionCount(request.getAssetId());
        String payload = String.format("{\"assetId\": %d, \"count\": %d}", request.getAssetId(), newCount);
        messagingTemplate.convertAndSend("/topic/reactions", payload);
        log.info("✅ Broadcast reaction update for asset {}: count {}", request.getAssetId(), newCount);
        log.debug("📡 Reaction payload sent: {}", payload);

        return ResponseEntity.ok(reaction);
    }

    /**
     * Get all reactions for an asset
     * GET /api/reactions/asset/{assetId}
     */
    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<Reaction>> getReactions(@PathVariable Long assetId) {
        List<Reaction> reactions = reactionService.getReactionsByAsset(assetId);
        return ResponseEntity.ok(reactions);
    }

    /**
     * Get reaction count for an asset
     * GET /api/reactions/asset/{assetId}/count
     */
    @GetMapping("/asset/{assetId}/count")
    public ResponseEntity<Long> getReactionCount(@PathVariable Long assetId) {
        Long count = reactionService.getReactionCount(assetId);
        return ResponseEntity.ok(count);
    }

    /**
     * Check if user has reacted
     * GET /api/reactions/asset/{assetId}/has-reacted
     */
    @GetMapping("/asset/{assetId}/has-reacted")
    public ResponseEntity<Boolean> hasUserReacted(
            @PathVariable Long assetId,
            @RequestParam(required = false) String anonymousId,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long userId = getUserIdFromToken(authHeader);
        boolean hasReacted = reactionService.hasUserReacted(assetId, userId, anonymousId);
        return ResponseEntity.ok(hasReacted);
    }

    /**
     * Extract user ID from JWT token
     */
    private Long getUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // TODO: Implement JWT parsing
                // For now, return null to allow anonymous reactions
                return null;
            } catch (Exception e) {
                log.warn("Invalid token: {}", e.getMessage());
            }
        }
        return null;
    }
}