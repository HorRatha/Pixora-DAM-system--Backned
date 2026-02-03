package com.dam.digitalassetmanagement.repository;

import com.dam.digitalassetmanagement.entity.RoleRequest;
import com.dam.digitalassetmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRequestRepository extends JpaRepository<RoleRequest, Long> {

    // ✅ FIXED: Changed from RequestStatus enum to String
    boolean existsByUserAndStatus(User user, String status);

    List<RoleRequest> findByUserOrderByCreatedAtDesc(User user);

    List<RoleRequest> findAllByOrderByCreatedAtDesc();

    // ✅ FIXED: Changed from RequestStatus enum to String
    List<RoleRequest> findByStatusOrderByCreatedAtDesc(String status);

    // Optional: Add these helper methods
    List<RoleRequest> findByStatus(String status);

    // ✅ FIXED: Changed from findByIdAndUser to findByRequestIdAndUser
    // because the @Id field is named 'requestId', not 'id'
    Optional<RoleRequest> findByRequestIdAndUser(Long requestId, User user);
}