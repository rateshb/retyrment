package com.retyrment.controller;

import com.retyrment.model.User;
import com.retyrment.service.UserDataDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user/data")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserDataController {

    private final UserDataDeletionService userDataDeletionService;

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof User user) {
                return user.getId();
            } else if (principal instanceof String userIdString) {
                // For cases where principal is just the userId string (e.g., from JwtAuthenticationFilter)
                return userIdString;
            }
        }
        throw new IllegalStateException("User not authenticated or userId not found in principal");
    }

    /**
     * Get summary of user's data (counts by category)
     * GET /api/user/data/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDataSummary() {
        try {
            String userId = getCurrentUserId();
            
            Map<String, Object> summary = userDataDeletionService.getUserDataSummary(userId);
            return ResponseEntity.ok(summary);
            
        } catch (RuntimeException e) {
            log.error("Error getting data summary", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get data summary");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Delete ALL user financial data
     * DELETE /api/user/data/all
     * 
     * Requires confirmation parameter to prevent accidental deletion
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> deleteAllData(
            @RequestParam(required = true) String confirmation) {
        
        try {
            String userId = getCurrentUserId();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = auth != null && auth.getPrincipal() instanceof User user ? user.getEmail() : "unknown";
            
            // Require explicit confirmation
            if (!"DELETE_ALL_DATA".equals(confirmation)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid confirmation");
                error.put("message", "You must provide confirmation='DELETE_ALL_DATA' to delete all data");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            log.warn("User {} ({}) is deleting all their data", userEmail, userId);
            
            Map<String, Object> result = userDataDeletionService.deleteAllUserData(userId);
            
            if (Boolean.TRUE.equals(result.get("success"))) {
                log.info("Successfully deleted all data for user {} ({})", userEmail, userId);
                return ResponseEntity.ok(result);
            } else {
                log.error("Failed to delete all data for user {} ({})", userEmail, userId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            
        } catch (RuntimeException e) {
            log.error("Error deleting user data", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to delete user data");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
