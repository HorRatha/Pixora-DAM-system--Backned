package com.dam.digitalassetmanagement.service;

import com.dam.digitalassetmanagement.dto.request.ReviewRoleRequestDto;
import com.dam.digitalassetmanagement.dto.request.RoleRequestDto;
import com.dam.digitalassetmanagement.dto.response.RoleRequestResponse;

import java.util.List;

public interface RoleRequestService {

    // User actions
    RoleRequestResponse createRoleRequest(RoleRequestDto request);
    List<RoleRequestResponse> getMyRoleRequests();
    RoleRequestResponse getRoleRequestById(Long requestId);

    // Admin actions
    List<RoleRequestResponse> getAllRoleRequests();
    List<RoleRequestResponse> getPendingRoleRequests();
    RoleRequestResponse reviewRoleRequest(Long requestId, ReviewRoleRequestDto review);
}