package com.dam.digitalassetmanagement.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * ✅ Broadcast reaction update to all connected clients
     * Called when a user likes/unlikes an asset
     */
    public void broadcastReactionUpdate(long assetId, int reactionCount) {
        String payload = String.format("{\"assetId\": %d, \"count\": %d}", assetId, reactionCount);
        log.debug("📡 Broadcasting reaction update: {}", payload);

        messagingTemplate.convertAndSend("/topic/reactions", payload);
    }

    /**
     * ✅ Broadcast comment update to all connected clients
     * Called when a comment is posted or deleted
     */
    public void broadcastCommentUpdate(long assetId, int commentCount) {
        String payload = String.format("{\"assetId\": %d, \"count\": %d}", assetId, commentCount);
        log.debug("📡 Broadcasting comment update: {}", payload);

        messagingTemplate.convertAndSend("/topic/comments", payload);
    }

    /**
     * ✅ Broadcast view update to all connected clients
     * Called when an asset is viewed
     */
    public void broadcastViewUpdate(long assetId, int viewCount) {
        String payload = String.format("{\"assetId\": %d, \"count\": %d}", assetId, viewCount);
        log.debug("📡 Broadcasting view update: {}", payload);

        messagingTemplate.convertAndSend("/topic/views", payload);
    }
}