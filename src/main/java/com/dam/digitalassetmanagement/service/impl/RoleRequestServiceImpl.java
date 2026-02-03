package com.dam.digitalassetmanagement.service.impl;

import com.dam.digitalassetmanagement.dto.request.ReviewRoleRequestDto;
import com.dam.digitalassetmanagement.dto.request.RoleRequestDto;
import com.dam.digitalassetmanagement.dto.response.RoleRequestResponse;
import com.dam.digitalassetmanagement.entity.RoleRequest;
import com.dam.digitalassetmanagement.entity.User;
import com.dam.digitalassetmanagement.repository.RoleRequestRepository;
import com.dam.digitalassetmanagement.repository.UserRepository;
import com.dam.digitalassetmanagement.service.RoleRequestService;
import com.dam.digitalassetmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleRequestServiceImpl implements RoleRequestService {

    private final RoleRequestRepository roleRequestRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    @Transactional
    public RoleRequestResponse createRoleRequest(RoleRequestDto request) {
        User currentUser = userService.getCurrentUser();

        // ✅ FIXED: requestedRole is now String, use toUpperCase() directly
        String requestedRole = request.getRequestedRole().toUpperCase();

        // Validation: Can't request same role
        // ✅ FIXED: Both are now Strings, use equalsIgnoreCase()
        if (currentUser.getRole().equalsIgnoreCase(requestedRole)) {
            throw new IllegalArgumentException("You already have the " + requestedRole + " role");
        }

        // Validation: Can't request VIEWER role (downgrade not allowed)
        if ("VIEWER".equalsIgnoreCase(requestedRole)) {
            throw new IllegalArgumentException("Cannot request VIEWER role");
        }

        // ✅ FIXED: status is now String "PENDING"
        // Validation: Check if user already has a pending request
        if (roleRequestRepository.existsByUserAndStatus(currentUser, "PENDING")) {
            throw new IllegalArgumentException("You already have a pending role request");
        }

        // Create new role request
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setUser(currentUser);
        // ✅ FIXED: setUserCurrentRole (matches new entity field name)
        roleRequest.setUserCurrentRole(currentUser.getRole());
        roleRequest.setRequestedRole(requestedRole);
        roleRequest.setReason(request.getReason());
        // ✅ FIXED: status is now String "PENDING"
        roleRequest.setStatus("PENDING");

        RoleRequest saved = roleRequestRepository.save(roleRequest);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleRequestResponse> getMyRoleRequests() {
        User currentUser = userService.getCurrentUser();
        List<RoleRequest> requests = roleRequestRepository.findByUserOrderByCreatedAtDesc(currentUser);
        return requests.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleRequestResponse getRoleRequestById(Long requestId) {
        RoleRequest request = roleRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Role request not found"));
        return mapToResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleRequestResponse> getAllRoleRequests() {
        List<RoleRequest> requests = roleRequestRepository.findAllByOrderByCreatedAtDesc();
        return requests.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleRequestResponse> getPendingRoleRequests() {
        // ✅ FIXED: status is now String "PENDING"
        List<RoleRequest> requests = roleRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING");
        return requests.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoleRequestResponse reviewRoleRequest(Long requestId, ReviewRoleRequestDto review) {
        User admin = userService.getCurrentUser();

        // Validation: Only admins can review
        // ✅ FIXED: Use isAdmin() helper method (String comparison)
        if (!admin.isAdmin()) {
            throw new IllegalArgumentException("Only admins can review role requests");
        }

        RoleRequest request = roleRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Role request not found"));

        // ✅ FIXED: status is now String "PENDING"
        // Validation: Can't review already reviewed requests
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("This request has already been reviewed");
        }

        // Process review
        String action = review.getAction().toUpperCase();

        if ("APPROVE".equals(action)) {
            // ✅ FIXED: status is now String "APPROVED"
            request.setStatus("APPROVED");

            // Update user's role
            User user = request.getUser();
            // ✅ FIXED: requestedRole is now String, no enum conversion needed
            user.setRole(request.getRequestedRole());
            userRepository.save(user);

        } else if ("REJECT".equals(action)) {
            // ✅ FIXED: status is now String "REJECTED"
            request.setStatus("REJECTED");

            // Validation: Rejection must have a reason
            if (review.getReviewComment() == null || review.getReviewComment().trim().isEmpty()) {
                throw new IllegalArgumentException("Review comment is required when rejecting a request");
            }

        } else {
            throw new IllegalArgumentException("Invalid action. Use 'APPROVE' or 'REJECT'");
        }

        request.setReviewedBy(admin);
        request.setReviewComment(review.getReviewComment());
        request.setReviewedAt(LocalDateTime.now());

        RoleRequest updated = roleRequestRepository.save(request);
        return mapToResponse(updated);
    }

    // Helper method to map entity to response
    private RoleRequestResponse mapToResponse(RoleRequest request) {
        return RoleRequestResponse.builder()
                .requestId(request.getRequestId())
                .userId(request.getUser().getUserId())
                .username(request.getUser().getUsername())
                // ✅ FIXED: userCurrentRole is now String
                .currentRole(request.getUserCurrentRole())
                // ✅ FIXED: requestedRole is now String
                .requestedRole(request.getRequestedRole())
                .reason(request.getReason())
                // ✅ FIXED: status is now String
                .status(request.getStatus())
                .reviewComment(request.getReviewComment())
                .reviewedByUsername(request.getReviewedBy() != null ? request.getReviewedBy().getUsername() : null)
                .reviewedAt(request.getReviewedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}