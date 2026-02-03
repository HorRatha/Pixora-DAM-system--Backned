package com.dam.digitalassetmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String type; // REACTION, COMMENT, VIEW
    private Long assetId;
    private Object data; // Reaction, Comment, or AssetStats
    private String action; // ADD, REMOVE, UPDATE
}