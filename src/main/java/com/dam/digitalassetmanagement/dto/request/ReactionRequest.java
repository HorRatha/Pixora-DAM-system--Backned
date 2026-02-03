package com.dam.digitalassetmanagement.dto.request;

import lombok.Data;

@Data
public class ReactionRequest {
    private Long assetId;
    private String reactionType = "HEART";
    private String anonymousId; // Optional for anonymous users
}