package com.retyrment.controller;

import com.retyrment.dto.UserResponseDTO;
import com.retyrment.model.User;
import com.retyrment.model.UserFeatureAccess;
import com.retyrment.repository.ExpenseRepository;
import com.retyrment.repository.FamilyMemberRepository;
import com.retyrment.repository.GoalRepository;
import com.retyrment.repository.IncomeRepository;
import com.retyrment.repository.InsuranceRepository;
import com.retyrment.repository.InvestmentRepository;
import com.retyrment.repository.LoanRepository;
import com.retyrment.repository.UserRepository;
import com.retyrment.service.FeatureAccessService;
import com.retyrment.service.RoleExpiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final RoleExpiryService roleExpiryService;
    private final FeatureAccessService featureAccessService;
    private final IncomeRepository incomeRepository;
    private final InvestmentRepository investmentRepository;
    private final ExpenseRepository expenseRepository;
    private final InsuranceRepository insuranceRepository;
    private final LoanRepository loanRepository;
    private final GoalRepository goalRepository;
    private final FamilyMemberRepository familyMemberRepository;

    /**
     * Check if current user is admin
     */
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.isAdmin();
        }
        return false;
    }

    /**
     * Get all users (admin only)
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(user -> {
                    Map<String, Object> userMap = UserResponseDTO.fromUser(user, true).toMap();
                    Map<String, Object> recordSummary = new LinkedHashMap<>();
                    String userId = user.getId();
                    recordSummary.put("income", incomeRepository.countByUserId(userId));
                    recordSummary.put("investments", investmentRepository.countByUserId(userId));
                    recordSummary.put("expenses", expenseRepository.countByUserId(userId));
                    recordSummary.put("insurance", insuranceRepository.countByUserId(userId));
                    recordSummary.put("loans", loanRepository.countByUserId(userId));
                    recordSummary.put("goals", goalRepository.countByUserId(userId));
                    recordSummary.put("family", familyMemberRepository.countByUserId(userId));
                    userMap.put("recordSummary", recordSummary);
                    return userMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", users.size());
        response.put("users", users);
        
        // Stats
        long freeCount = users.stream().filter(u -> "FREE".equals(u.get("role"))).count();
        long proCount = users.stream().filter(u -> "PRO".equals(u.get("role"))).count();
        long adminCount = users.stream().filter(u -> "ADMIN".equals(u.get("role"))).count();
        
        // Count users with active trials
        long activeTrials = users.stream()
            .filter(u -> u.get("trial") != null)
            .filter(u -> Boolean.TRUE.equals(((Map<?, ?>) u.get("trial")).get("active")))
            .count();
        
        // Count users with temporary roles
        long temporaryRoles = users.stream()
            .filter(u -> u.get("roleInfo") != null)
            .filter(u -> Boolean.TRUE.equals(((Map<?, ?>) u.get("roleInfo")).get("temporary")))
            .count();
        
        // Count expiring roles in next 7 days
        long expiringIn7Days = roleExpiryService.countExpiringRoles(7);
        
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("free", freeCount);
        stats.put("pro", proCount);
        stats.put("admin", adminCount);
        stats.put("activeTrials", activeTrials);
        stats.put("temporaryRoles", temporaryRoles);
        stats.put("expiringIn7Days", expiringIn7Days);
        
        response.put("stats", stats);

        return ResponseEntity.ok(response);
    }

    /**
     * Update user role (admin only)
     * Supports time-limited role changes with optional duration
     * 
     * Request body:
     * - role: Required. FREE, PRO, or ADMIN
     * - durationDays: Optional. Number of days for temporary access (null = permanent)
     * - reason: Optional. Reason for role change (e.g., "License purchase", "Trial extension")
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request) {
        
        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        String newRole = (String) request.get("role");
        if (newRole == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role is required"));
        }

        User.UserRole role;
        try {
            role = User.UserRole.valueOf(newRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role. Must be FREE, PRO, or ADMIN"));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        
        // Prevent self-demotion from admin
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = null;
        if (auth.getPrincipal() instanceof User currentUser) {
            if (currentUser.getId().equals(userId) && role != User.UserRole.ADMIN) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot demote yourself from admin"));
            }
            adminEmail = currentUser.getEmail();
        }

        // Get duration and reason from request
        Integer durationDays = null;
        if (request.get("durationDays") != null) {
            durationDays = ((Number) request.get("durationDays")).intValue();
        }
        String reason = (String) request.get("reason");
        
        LocalDateTime now = LocalDateTime.now();
        User.UserRole currentRole = user.getRole();
        
        // If changing to PRO role, set subscription dates
        if (role == User.UserRole.PRO) {
            if (durationDays != null && durationDays > 0) {
                // Time-limited PRO subscription
                // If user already has an active subscription, extend from end date
                // Otherwise, start from now
                LocalDateTime extendFrom;
                if (user.getRole() == User.UserRole.PRO && user.isSubscriptionActive() && user.getSubscriptionEndDate() != null) {
                    extendFrom = user.getSubscriptionEndDate(); // Extend from current end date
                } else {
                    extendFrom = now; // Start new subscription from now
                    user.setSubscriptionStartDate(now);
                }
                user.setSubscriptionEndDate(extendFrom.plusDays(durationDays));
            } else {
                // For PRO, even "permanent" should have a default subscription period
                // Set to a long period (e.g., 10 years) or require explicit duration
                // For now, we'll require duration for PRO
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "PRO subscriptions must have a duration. Please specify durationDays."
                ));
            }
        } else if (role == User.UserRole.FREE) {
            // Clearing PRO subscription when downgrading to FREE
            user.setSubscriptionStartDate(null);
            user.setSubscriptionEndDate(null);
        }
        // ADMIN role doesn't need subscription dates
        
        // If this is a temporary role change (for roleExpiryDate mechanism)
        if (durationDays != null && durationDays > 0) {
            // Save original role if not already saved (to revert after expiry)
            if (user.getOriginalRole() == null || user.getRoleExpiryDate() == null || user.isRoleExpired()) {
                user.setOriginalRole(currentRole);
            }
            user.setRoleExpiryDate(now.plusDays(durationDays));
        } else {
            // Permanent role change - clear expiry
            user.setRoleExpiryDate(null);
            user.setOriginalRole(null);
        }
        
        user.setRole(role);
        user.setRoleChangeReason(reason);
        user.setRoleChangedAt(now);
        user.setRoleChangedBy(adminEmail);
        
        userRepository.save(user);

        String message = durationDays != null && durationDays > 0 
            ? String.format("User role updated to %s for %d days", role, durationDays)
            : String.format("User role updated to %s (permanent)", role);

        return ResponseEntity.ok(Map.of(
            "message", message,
            "user", UserResponseDTO.fromUser(user, true).toMap()
        ));
    }
    
    /**
     * Extend user's trial period (admin only)
     */
    @PutMapping("/users/{userId}/extend-trial")
    public ResponseEntity<?> extendTrial(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request) {
        
        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Integer additionalDays = request.get("days") != null 
            ? ((Number) request.get("days")).intValue() 
            : 7;
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();
        
        // Extend from current trial end or from now if trial expired
        LocalDateTime extendFrom = user.getTrialEndDate() != null && user.getTrialEndDate().isAfter(now) 
            ? user.getTrialEndDate() 
            : now;
        
        if (user.getTrialStartDate() == null) {
            user.setTrialStartDate(now);
        }
        user.setTrialEndDate(extendFrom.plusDays(additionalDays));
        
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of(
            "message", String.format("Trial extended by %d days", additionalDays),
            "trialEndDate", user.getTrialEndDate(),
            "user", UserResponseDTO.fromUser(user, true).toMap()
        ));
    }
    
    /**
     * Force check and revert expired roles (admin only)
     */
    @PostMapping("/roles/check-expired")
    public ResponseEntity<?> forceCheckExpiredRoles() {
        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        int revertedCount = roleExpiryService.forceCheckExpiredRoles();
        
        return ResponseEntity.ok(Map.of(
            "message", String.format("Checked and reverted %d expired roles", revertedCount),
            "revertedCount", revertedCount
        ));
    }
    
    /**
     * Remove user role expiry (make permanent) - admin only
     */
    @DeleteMapping("/users/{userId}/role-expiry")
    public ResponseEntity<?> removeRoleExpiry(@PathVariable String userId) {
        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        user.setRoleExpiryDate(null);
        user.setOriginalRole(null);
        user.setRoleChangeReason("Role made permanent by admin");
        user.setRoleChangedAt(LocalDateTime.now());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof User currentUser) {
            user.setRoleChangedBy(currentUser.getEmail());
        }
        
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of(
            "message", "Role expiry removed - role is now permanent",
            "user", UserResponseDTO.fromUser(user, true).toMap()
        ));
    }

    /**
     * Get user by ID (admin only)
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable String userId) {
        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(UserResponseDTO.fromUser(user, true)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search users by email (admin only)
     */
    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(@RequestParam String email) {
        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        List<Map<String, Object>> users = userRepository.findAll().stream()
                .filter(u -> u.getEmail().toLowerCase().contains(email.toLowerCase()))
                .map(user -> UserResponseDTO.fromUser(user, true).toMap())
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("users", users));
    }

    /**
     * Delete user (admin only)
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        // Prevent self-deletion
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof User currentUser) {
            if (currentUser.getId().equals(userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete yourself"));
            }
        }

        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }

        userRepository.deleteById(userId);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }


    /**
     * Get feature access for a user (admin only)
     */
    @GetMapping("/users/{userId}/features")
    public ResponseEntity<?> getUserFeatureAccess(@PathVariable String userId) {
        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserFeatureAccess access = featureAccessService.getUserFeatureAccess(userOpt.get());
        return ResponseEntity.ok(access);
    }

    /**
     * Update feature access for a user (admin only)
     */
    @PutMapping("/users/{userId}/features")
    public ResponseEntity<?> updateUserFeatureAccess(
            @PathVariable String userId,
            @RequestBody UserFeatureAccess featureAccess) {
        
        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        featureAccess.setUserId(userId);
        UserFeatureAccess updated = featureAccessService.updateFeatureAccess(userId, featureAccess);
        
        return ResponseEntity.ok(Map.of(
            "message", "Feature access updated successfully",
            "featureAccess", updated
        ));
    }
}
