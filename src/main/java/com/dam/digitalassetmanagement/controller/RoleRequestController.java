package com.dam.digitalassetmanagement.controller;

import com.dam.digitalassetmanagement.dto.request.ReviewRoleRequestDto;
import com.dam.digitalassetmanagement.dto.request.RoleRequestDto;
import com.dam.digitalassetmanagement.dto.response.RoleRequestResponse;
import com.dam.digitalassetmanagement.service.RoleRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/role-requests")
@RequiredArgsConstructor
@Tag(name = "Role Request Management", description = "Users can request role upgrades, admins can approve/reject")
public class RoleRequestController {

    private final RoleRequestService roleRequestService;

    // ============================================
    // USER ENDPOINTS
    // ============================================

    @PostMapping
    @Operation(
            summary = "Request a role upgrade",
            description = "Users can request UPLOADER, EDITOR, or ADMIN roles. Cannot request if already have pending request."
    )
    public ResponseEntity<RoleRequestResponse> createRoleRequest(
            @Valid @RequestBody RoleRequestDto request) {
        RoleRequestResponse response = roleRequestService.createRoleRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-requests")
    @Operation(
            summary = "Get my role requests",
            description = "View all your role requests and their status"
    )
    public ResponseEntity<List<RoleRequestResponse>> getMyRoleRequests() {
        List<RoleRequestResponse> requests = roleRequestService.getMyRoleRequests();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get role request by ID")
    public ResponseEntity<RoleRequestResponse> getRoleRequestById(@PathVariable Long requestId) {
        RoleRequestResponse request = roleRequestService.getRoleRequestById(requestId);
        return ResponseEntity.ok(request);
    }

    // ============================================
    // ADMIN ENDPOINTS
    // ============================================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all role requests (Admin only)",
            description = "View all role requests from all users"
    )
    public ResponseEntity<List<RoleRequestResponse>> getAllRoleRequests() {
        List<RoleRequestResponse> requests = roleRequestService.getAllRoleRequests();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get pending role requests (Admin only)",
            description = "View all pending role requests that need review"
    )
    public ResponseEntity<List<RoleRequestResponse>> getPendingRoleRequests() {
        List<RoleRequestResponse> requests = roleRequestService.getPendingRoleRequests();
        return ResponseEntity.ok(requests);
    }

    @PutMapping("/{requestId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Approve or reject a role request (Admin only)",
            description = "Admin can approve (automatically updates user role) or reject (must provide reason) a role request"
    )
    public ResponseEntity<RoleRequestResponse> reviewRoleRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ReviewRoleRequestDto review) {
        RoleRequestResponse response = roleRequestService.reviewRoleRequest(requestId, review);
        return ResponseEntity.ok(response);
    }
}