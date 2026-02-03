package com.dam.digitalassetmanagement.dto.request;

import lombok.Data;

@Data
public class CommentRequest {
    private Long assetId;
    private String content;
    private Long parentId; // For replies
    private String anonymousId; // Optional for anonymous users
    private String username; // Required for anonymous users
}