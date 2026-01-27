package com.retyrment.controller;

import com.retyrment.service.InsuranceRecommendationService;
import com.retyrment.service.InsuranceRecommendationService.InsuranceRecommendation;
import lombok.RequiredArgsConstructor;
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
public class InsuranceRecommendationController extends BaseController {

    private final InsuranceRecommendationService recommendationService;

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
