package com.dam.digitalassetmanagement.entity;

import com.dam.digitalassetmanagement.enums.UsageAction;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "assetusage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Long usageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UsageAction action;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime timestamp;
}