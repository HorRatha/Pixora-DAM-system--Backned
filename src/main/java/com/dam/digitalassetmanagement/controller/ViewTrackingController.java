package com.dam.digitalassetmanagement.controller;

import com.dam.digitalassetmanagement.dto.request.ViewRequest;
import com.dam.digitalassetmanagement.entity.AssetView;
import com.dam.digitalassetmanagement.service.ViewTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ViewTrackingController {

    private final ViewTrackingService viewTrackingService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * ✅ FIXED: Record a view for an asset
     * POST /api/assets/{assetId}/view
     */
    @PostMapping("/api/assets/{assetId}/view")
    public ResponseEntity<Void> recordView(
            @PathVariable Long assetId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromToken(authHeader);

        log.info("🎥 Record view request for asset: {}", assetId);

        // Create view request
        ViewRequest request = new ViewRequest();
        request.setAssetId(assetId);

        // Track the view
        AssetView view = viewTrackingService.trackView(request, userId, httpRequest);
        log.debug("✅ View tracked for asset: {}", assetId);

        // ✅ CRITICAL: Broadcast real-time update with proper JSON format
        // Get stats which contains the view count
        Map<String, Object> stats = viewTrackingService.getViewStats(assetId);
        Long viewCount = stats != null ? ((Number) stats.getOrDefault("totalViews", 0L)).longValue() : 0L;
        String payload = String.format("{\"assetId\": %d, \"count\": %d}", assetId, viewCount);
        messagingTemplate.convertAndSend("/topic/views", payload);
        log.info("✅ Broadcast view update for asset {}: count {}", assetId, viewCount);

        return ResponseEntity.ok().build();
    }

    /**
     * Get view count for an asset
     * GET /api/assets/{assetId}/views/count
     */
    @GetMapping("/api/assets/{assetId}/views/count")
    public ResponseEntity<Long> getViewCount(@PathVariable Long assetId) {
        Map<String, Object> stats = viewTrackingService.getViewStats(assetId);
        Long count = stats != null ? ((Number) stats.getOrDefault("totalViews", 0L)).longValue() : 0L;
        log.debug("📊 View count for asset {}: {}", assetId, count);
        return ResponseEntity.ok(count);
    }

    /**
     * Get view statistics for an asset
     * GET /api/views/asset/{assetId}/stats
     */
    @GetMapping("/api/views/asset/{assetId}/stats")
    public ResponseEntity<Map<String, Object>> getViewStats(@PathVariable Long assetId) {
        Map<String, Object> stats = viewTrackingService.getViewStats(assetId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Track a view (legacy endpoint - kept for backward compatibility)
     * POST /api/views
     */
    @PostMapping("/api/views")
    public ResponseEntity<AssetView> trackView(
            @RequestBody ViewRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromToken(authHeader);

        log.info("Track view request for asset: {}", request.getAssetId());

        AssetView view = viewTrackingService.trackView(request, userId, httpRequest);

        // ✅ CRITICAL: Broadcast real-time update with proper JSON format
        Map<String, Object> stats = viewTrackingService.getViewStats(request.getAssetId());
        Long viewCount = stats != null ? ((Number) stats.getOrDefault("totalViews", 0L)).longValue() : 0L;
        String payload = String.format("{\"assetId\": %d, \"count\": %d}", request.getAssetId(), viewCount);
        messagingTemplate.convertAndSend("/topic/views", payload);
        log.info("✅ Broadcast view update for asset {}: count {}", request.getAssetId(), viewCount);

        return ResponseEntity.ok(view);
    }

    /**
     * Extract user ID from JWT token
     */
    private Long getUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // TODO: Implement JWT parsing
                // For now, return null to allow anonymous view tracking
                return null;
            } catch (Exception e) {
                log.warn("Invalid token: {}", e.getMessage());
            }
        }
        return null;
    }
}