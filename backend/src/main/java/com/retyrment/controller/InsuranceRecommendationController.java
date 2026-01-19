package com.retyrment.controller;

import com.retyrment.model.User;
import com.retyrment.service.InsuranceRecommendationService;
import com.retyrment.service.InsuranceRecommendationService.InsuranceRecommendation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for insurance recommendations.
 * Provides personalized recommendations for:
 * - Health Insurance (based on family composition, ages, risk factors)
 * - Term Life Insurance (based on income, dependents, liabilities)
 */
@RestController
@RequestMapping("/insurance/recommendations")
@RequiredArgsConstructor
public class InsuranceRecommendationController {

    private final InsuranceRecommendationService recommendationService;

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new IllegalStateException("User not authenticated");
    }

    /**
     * Get comprehensive insurance recommendations
     * 
     * Returns:
     * - Health insurance recommendation with coverage breakdown by family member
     * - Term insurance recommendation with calculation breakdown
     * - Summary with urgency assessment and action items
     */
    @GetMapping
    public InsuranceRecommendation getRecommendations() {
        String userId = getCurrentUserId();
        return recommendationService.generateRecommendations(userId);
    }
}
