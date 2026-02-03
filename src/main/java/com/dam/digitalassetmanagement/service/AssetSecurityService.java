package com.dam.digitalassetmanagement.service;


import com.dam.digitalassetmanagement.entity.Asset;
import com.dam.digitalassetmanagement.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Security service to check if a user can modify an asset
 * Used in @PreAuthorize annotations in controllers
 */
@Service("assetSecurityService")
@RequiredArgsConstructor
public class AssetSecurityService {

    private final AssetRepository assetRepository;

    /**
     * Check if the current user owns the asset
     * @param assetId The asset ID
     * @param authentication The current authentication object
     * @return true if user owns the asset, false otherwise
     */
    public boolean isOwner(Long assetId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Asset asset = assetRepository.findById(assetId).orElse(null);
        if (asset == null) {
            return false;
        }

        String currentUsername = authentication.getName();
        String assetOwnerUsername = asset.getUser().getUsername();

        return currentUsername.equals(assetOwnerUsername);
    }

    /**
     * Check if the current user can modify (edit/delete) an asset
     * User can modify if:
     * 1. They own the asset, OR
     * 2. They have ADMIN role
     */
    public boolean canModify(Long assetId, Authentication authentication) {
        return isOwner(assetId, authentication) || hasRole(authentication, "ADMIN");
    }

    /**
     * Helper method to check if user has a specific role
     */
    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority ->
                        grantedAuthority.getAuthority().equals("ROLE_" + role));
    }
}