package com.dam.digitalassetmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_views")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_id")
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "anonymous_id")
    private String anonymousId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "view_duration")
    private Integer viewDuration = 0;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt = LocalDateTime.now();
}