package com.dam.digitalassetmanagement.service;

import com.dam.digitalassetmanagement.dto.WebSocketMessage;
import com.dam.digitalassetmanagement.dto.request.ReactionRequest;
import com.dam.digitalassetmanagement.entity.Reaction;
import com.dam.digitalassetmanagement.repository.ReactionRepository;
import com.dam.digitalassetmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Toggle reaction (add if doesn't exist, remove if exists)
     */
    @Transactional
    public Reaction toggleReaction(ReactionRequest request, Long userId) {
        log.info("Toggling reaction for asset: {} by user: {}", request.getAssetId(), userId);

        Optional<Reaction> existingReaction;

        if (userId != null) {
            existingReaction = reactionRepository.findByAssetIdAndUserId(request.getAssetId(), userId);
        } else {
            existingReaction = reactionRepository.findByAssetIdAndAnonymousId(
                    request.getAssetId(), request.getAnonymousId()
            );
        }

        Reaction reaction;
        String action;

        if (existingReaction.isPresent()) {
            // Remove reaction
            reactionRepository.delete(existingReaction.get());
            reaction = existingReaction.get();
            action = "REMOVE";
            log.info("Removed reaction for asset: {}", request.getAssetId());
        } else {
            // Add reaction
            reaction = new Reaction();
            reaction.setAssetId(request.getAssetId());
            reaction.setUserId(userId);
            reaction.setAnonymousId(request.getAnonymousId());
            reaction.setReactionType(request.getReactionType());
            reaction.setCreatedAt(LocalDateTime.now());

            // Get username if user is logged in
            if (userId != null) {
                var user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    reaction.setUsername(user.getUsername());
                }
            }

            reaction = reactionRepository.save(reaction);
            action = "ADD";
            log.info("Added reaction for asset: {}", request.getAssetId());
        }

        // Broadcast to WebSocket
        broadcastReactionUpdate(reaction, action);

        return reaction;
    }

    /**
     * Get all reactions for an asset
     */
    public List<Reaction> getReactionsByAsset(Long assetId) {
        return reactionRepository.findByAssetIdOrderByCreatedAtDesc(assetId);
    }

    /**
     * Get reaction count for an asset
     */
    public Long getReactionCount(Long assetId) {
        return reactionRepository.countByAssetId(assetId);
    }

    /**
     * Check if user has reacted to asset
     */
    public boolean hasUserReacted(Long assetId, Long userId, String anonymousId) {
        if (userId != null) {
            return reactionRepository.existsByAssetIdAndUserId(assetId, userId);
        } else if (anonymousId != null) {
            return reactionRepository.existsByAssetIdAndAnonymousId(assetId, anonymousId);
        }
        return false;
    }

    /**
     * Broadcast reaction update via WebSocket
     * ✅ FIXED: Now broadcasts to /topic/reactions with proper message format
     */
    private void broadcastReactionUpdate(Reaction reaction, String action) {
        Long assetId = reaction.getAssetId();
        Long reactionCount = getReactionCount(assetId);

        // ✅ FIXED: Create message that matches Android subscription
        Map<String, Object> broadcastMessage = new HashMap<>();
        broadcastMessage.put("assetId", assetId);
        broadcastMessage.put("count", reactionCount);
        broadcastMessage.put("action", action);
        broadcastMessage.put("reactionId", reaction.getId());
        broadcastMessage.put("reactionType", reaction.getReactionType());
        broadcastMessage.put("username", reaction.getUsername());

        // ✅ FIXED: Broadcast to /topic/reactions instead of /topic/asset/{assetId}
        messagingTemplate.convertAndSend("/topic/reactions", broadcastMessage);

        log.info("✅ Broadcast reaction update for asset {}: count {}", assetId, reactionCount);
    }
}