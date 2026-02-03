package com.dam.digitalassetmanagement.dto.request;

import lombok.Data;

@Data
public class ViewRequest {
    private Long assetId;
    private Integer viewDuration; // Optional
    private String anonymousId; // Optional for anonymous users
}