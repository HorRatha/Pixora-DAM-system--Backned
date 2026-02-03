package com.dam.digitalassetmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetStats {
    private Long assetId;
    private Integer totalReactions;
    private Integer totalComments;
    private Integer totalViews;
    private Integer uniqueViewers;
    private Boolean hasUserReacted;
    private Boolean hasUserCommented;
}