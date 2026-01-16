package com.retyrment.controller;

import com.retyrment.dto.UserResponseDTO;
import com.retyrment.model.User;
import com.retyrment.repository.UserRepository;
import com.retyrment.security.JwtUtils;
import com.retyrment.service.FeatureAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
            return ResponseEntity.ok(UserResponseDTO.fromUser(user, false));
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
                UserResponseDTO userResponse = UserResponseDTO.fromUser(user, false);
                Map<String, Object> response = userResponse.toMap();
                response.put("valid", true);
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
}
