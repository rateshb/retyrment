package com.retyrment.controller;

import com.retyrment.model.User;
import com.retyrment.repository.UserRepository;
import com.retyrment.security.JwtUtils;
import com.retyrment.service.FeatureAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final FeatureAccessService featureAccessService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        if (auth.getPrincipal() instanceof User user) {
            return ResponseEntity.ok(userToResponse(user));
        }
        
        return ResponseEntity.status(401).body(Map.of("error", "Invalid authentication"));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        
        if (token == null || !jwtUtils.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("valid", false));
        }
        
        String email = jwtUtils.getEmailFromToken(token);
        return userRepository.findByEmail(email)
            .map(user -> {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("valid", true);
                response.putAll(userToResponse(user));
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.status(401).body(Map.of("valid", false)));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Clear security context
        SecurityContextHolder.clearContext();
        // Note: Frontend should clear localStorage token and user data
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    
    /**
     * Get feature access based on user role
     */
    @GetMapping("/features")
    public ResponseEntity<?> getFeatureAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        if (auth.getPrincipal() instanceof User user) {
            // Get feature access from service (includes per-user overrides)
            Map<String, Object> features = featureAccessService.getFeatureAccessMap(user);
            
            // Add legacy feature flags for backward compatibility
            features.put("dataEntry", true);
            features.put("viewInvestments", features.get("investmentPage"));
            features.put("viewProjections", true);
            features.put("recommendations", user.isPro());
            features.put("retirementPlanning", features.get("retirementPage"));
            features.put("adjustAssumptions", user.isPro());
            features.put("downloadReports", features.get("reportsPage"));
            features.put("monteCarlo", features.get("simulationPage"));
            features.put("goalAnalysis", features.get("goalsPage"));
            features.put("userManagement", user.isAdmin());
            
            return ResponseEntity.ok(Map.of(
                "role", user.getRole().name(),
                "effectiveRole", user.getEffectiveRole().name(),
                "isPro", user.isPro(),
                "isAdmin", user.isAdmin(),
                "features", features
            ));
        }
        
        return ResponseEntity.status(401).body(Map.of("error", "Invalid authentication"));
    }
    
    private Map<String, Object> userToResponse(User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("picture", user.getPicture());
        response.put("role", user.getRole().name());
        response.put("effectiveRole", user.getEffectiveRole().name());
        response.put("isPro", user.isPro());
        response.put("isAdmin", user.isAdmin());
        
        // Trial information
        if (user.isInTrial()) {
            Map<String, Object> trial = new LinkedHashMap<>();
            trial.put("active", true);
            trial.put("daysRemaining", user.getTrialDaysRemaining());
            trial.put("endDate", user.getTrialEndDate());
            response.put("trial", trial);
        }
        
        // PRO Subscription information
        if (user.getRole() == User.UserRole.PRO) {
            Map<String, Object> subscription = new LinkedHashMap<>();
            subscription.put("active", user.isSubscriptionActive());
            subscription.put("startDate", user.getSubscriptionStartDate());
            subscription.put("endDate", user.getSubscriptionEndDate());
            subscription.put("daysRemaining", user.getSubscriptionDaysRemaining());
            subscription.put("expired", !user.isSubscriptionActive());
            response.put("subscription", subscription);
        }
        
        // Role expiry information (for temporary role changes)
        if (user.getRoleExpiryDate() != null) {
            Map<String, Object> roleInfo = new LinkedHashMap<>();
            roleInfo.put("temporary", true);
            roleInfo.put("expiryDate", user.getRoleExpiryDate());
            roleInfo.put("daysRemaining", user.getRoleDaysRemaining());
            roleInfo.put("originalRole", user.getOriginalRole() != null ? user.getOriginalRole().name() : null);
            roleInfo.put("reason", user.getRoleChangeReason());
            response.put("roleInfo", roleInfo);
        }
        
        return response;
    }
}
