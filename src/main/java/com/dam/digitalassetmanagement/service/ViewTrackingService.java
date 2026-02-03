package com.dam.digitalassetmanagement.service;

import com.dam.digitalassetmanagement.dto.WebSocketMessage;
import com.dam.digitalassetmanagement.dto.request.ViewRequest;
import com.dam.digitalassetmanagement.entity.AssetView;
import com.dam.digitalassetmanagement.repository.AssetViewRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewTrackingService {

    private final AssetViewRepository viewRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Track a view (allows multiple views from same user like Facebook)
     */
    @Transactional
    public AssetView trackView(ViewRequest request, Long userId, HttpServletRequest httpRequest) {
        log.info("Tracking view for asset: {} by user: {}", request.getAssetId(), userId);

        AssetView view = new AssetView();
        view.setAssetId(request.getAssetId());
        view.setUserId(userId);
        view.setAnonymousId(request.getAnonymousId());
        view.setViewDuration(request.getViewDuration());
        view.setIpAddress(getClientIp(httpRequest));
        view.setUserAgent(httpRequest.getHeader("User-Agent"));
        view.setViewedAt(LocalDateTime.now());

        view = viewRepository.save(view);
        log.info("View tracked with ID: {}", view.getId());

        // Broadcast view stats update
        broadcastViewUpdate(request.getAssetId());

        return view;
    }

    /**
     * Get view statistics for an asset
     */
    public Map<String, Object> getViewStats(Long assetId) {
        Long totalViews = viewRepository.countByAssetId(assetId);
        Long uniqueViewers = viewRepository.countUniqueViewers(assetId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalViews", totalViews);
        stats.put("uniqueViewers", uniqueViewers);

        return stats;
    }

    /**
     * Get client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Broadcast view stats update via WebSocket
     */
    private void broadcastViewUpdate(Long assetId) {
        Map<String, Object> stats = getViewStats(assetId);

        WebSocketMessage message = new WebSocketMessage();
        message.setType("VIEW");
        message.setAssetId(assetId);
        message.setData(stats);
        message.setAction("UPDATE");

        messagingTemplate.convertAndSend("/topic/asset/" + assetId, message);
    }
}