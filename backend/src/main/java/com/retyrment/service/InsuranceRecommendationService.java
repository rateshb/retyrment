package com.retyrment.service;

import com.retyrment.model.*;
import com.retyrment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Service for generating insurance recommendations based on:
 * - Family composition (dependents, ages)
 * - Income and liabilities
 * - Existing coverage
 * - Risk factors
 * 
 * Recommendations follow Indian insurance industry standards:
 * - Health: Base cover + inflation hedge
 * - Term Life: 10-15x annual income + liabilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InsuranceRecommendationService {

    private final FamilyMemberRepository familyMemberRepository;
    private final IncomeRepository incomeRepository;
    private final InsuranceRepository insuranceRepository;
    private final LoanRepository loanRepository;
    private final SettingsRepository settingsRepository;
    private final ExpenseRepository expenseRepository;

    // Health Insurance Constants
    private static final double BASE_HEALTH_COVER_INDIVIDUAL = 500000;      // 5 Lakhs base
    private static final double BASE_HEALTH_COVER_FAMILY = 1000000;         // 10 Lakhs for family floater
    private static final double SUPER_TOP_UP_COVER = 10000000;              // 1 Crore super top-up
    private static final double HEALTH_COVER_PER_SENIOR = 500000;           // 5 Lakhs per senior
    private static final double HEALTH_COVER_SUPER_SENIOR = 1000000;        // 10 Lakhs for 80+
    private static final double HEALTH_INFLATION_FACTOR = 1.10;             // 10% medical inflation per year
    
    // Term Insurance Constants
    private static final int INCOME_MULTIPLIER_YOUNG = 15;                  // Under 35: 15x income
    private static final int INCOME_MULTIPLIER_MIDDLE = 12;                 // 35-45: 12x income
    private static final int INCOME_MULTIPLIER_OLDER = 10;                  // 45+: 10x income
    private static final double EXPENSE_YEARS_COVERAGE = 10;                // Cover 10 years of expenses
    
    /**
     * Generate comprehensive insurance recommendations for a user
     */
    public InsuranceRecommendation generateRecommendations(String userId) {
        InsuranceRecommendation recommendation = new InsuranceRecommendation();
        
        // Gather all data
        List<FamilyMember> family = familyMemberRepository.findByUserId(userId);
        List<Income> incomes = incomeRepository.findByUserId(userId);
        List<Insurance> existingPolicies = insuranceRepository.findByUserId(userId);
        List<Loan> loans = loanRepository.findByUserId(userId);
        List<Expense> expenses = expenseRepository.findByUserId(userId);
        Settings settings = settingsRepository.findByUserId(userId).orElse(null);
        
        // Calculate user age
        int userAge = settings != null && settings.getCurrentAge() != null ? settings.getCurrentAge() : 35;
        int retirementAge = settings != null && settings.getRetirementAge() != null ? settings.getRetirementAge() : 60;
        int yearsToRetirement = Math.max(0, retirementAge - userAge);
        
        // Calculate totals
        double totalAnnualIncome = incomes.stream()
                .mapToDouble(i -> i.getMonthlyAmount() != null ? i.getMonthlyAmount() * 12 : 0)
                .sum();
        
        double totalLiabilities = loans.stream()
                .mapToDouble(l -> l.getOutstandingAmount() != null ? l.getOutstandingAmount() : 0)
                .sum();
        
        double totalMonthlyExpenses = expenses.stream()
                .mapToDouble(Expense::getMonthlyEquivalent)
                .sum();
        
        // Existing coverage from insurance policies
        double existingHealthCoverFromPolicies = existingPolicies.stream()
                .filter(p -> p.getType() == Insurance.InsuranceType.HEALTH)
                .mapToDouble(p -> p.getSumAssured() != null ? p.getSumAssured() : 0)
                .sum();
        
        // Also include existing health cover reported on family members
        double existingHealthCoverFromFamily = family.stream()
                .mapToDouble(m -> m.getExistingHealthCover() != null ? m.getExistingHealthCover() : 0)
                .sum();
        
        // Total existing health cover (avoid double counting - use max of both)
        double existingHealthCover = Math.max(existingHealthCoverFromPolicies, existingHealthCoverFromFamily);
        
        double existingTermCover = existingPolicies.stream()
                .filter(p -> p.getType() == Insurance.InsuranceType.TERM_LIFE)
                .mapToDouble(p -> p.getSumAssured() != null ? p.getSumAssured() : 0)
                .sum();
        
        // Also include life cover from family members
        double existingLifeCoverFromFamily = family.stream()
                .mapToDouble(m -> m.getExistingLifeCover() != null ? m.getExistingLifeCover() : 0)
                .sum();
        existingTermCover = Math.max(existingTermCover, existingLifeCoverFromFamily);
        
        // Generate Health Insurance Recommendation
        recommendation.healthRecommendation = calculateHealthRecommendation(
                family, userAge, existingHealthCover, yearsToRetirement);
        
        // Generate Term Insurance Recommendation
        recommendation.termRecommendation = calculateTermRecommendation(
                family, userAge, totalAnnualIncome, totalLiabilities, 
                totalMonthlyExpenses, existingTermCover, yearsToRetirement);
        
        // Summary
        recommendation.summary = generateSummary(recommendation, totalAnnualIncome);
        
        return recommendation;
    }

    /**
     * Calculate health insurance recommendation
     */
    private HealthRecommendation calculateHealthRecommendation(
            List<FamilyMember> family, int userAge, double existingCover, int yearsToRetirement) {
        
        HealthRecommendation rec = new HealthRecommendation();
        rec.existingCover = existingCover;
        rec.memberBreakdown = new ArrayList<>();
        
        // Find self and spouse
        FamilyMember self = family.stream()
                .filter(m -> m.getRelationship() == FamilyMember.Relationship.SELF)
                .findFirst().orElse(null);
        
        FamilyMember spouse = family.stream()
                .filter(m -> m.getRelationship() == FamilyMember.Relationship.SPOUSE)
                .findFirst().orElse(null);
        
        // Count children and parents
        List<FamilyMember> children = family.stream()
                .filter(m -> m.getRelationship() == FamilyMember.Relationship.CHILD)
                .toList();
        
        List<FamilyMember> parents = family.stream()
                .filter(m -> m.getRelationship() == FamilyMember.Relationship.PARENT ||
                            m.getRelationship() == FamilyMember.Relationship.PARENT_IN_LAW)
                .toList();
        
        // Base recommendation for self + spouse + children (Family Floater)
        double familyFloaterCover = BASE_HEALTH_COVER_FAMILY; // ₹10 Lakhs
        
        // Super top-up to extend coverage for large claims
        double superTopUpCover = SUPER_TOP_UP_COVER; // ₹1 Crore
        
        // Build family composition string
        int familyCount = (self != null ? 1 : 0) + (spouse != null ? 1 : 0) + children.size();
        String familyComposition = String.format("Self%s%s",
                spouse != null ? " + Spouse" : "",
                !children.isEmpty() ? " + " + children.size() + " Child" + (children.size() > 1 ? "ren" : "") : "");
        
        // Family floater recommendation
        MemberHealthBreakdown familyBreakdown = new MemberHealthBreakdown();
        familyBreakdown.memberType = familyComposition;
        familyBreakdown.recommendedPolicyType = "Family Floater";
        familyBreakdown.recommendedCover = familyFloaterCover;
        familyBreakdown.reasoning = String.format(
                "%d members covered. Base ₹%s for routine hospitalizations. Acts as deductible for Super Top-Up.",
                familyCount, formatAmount(familyFloaterCover));
        
        // Risk factors and premium adjustments
        List<String> riskFactors = new ArrayList<>();
        double premiumMultiplier = 1.0;
        
        if (self != null && Boolean.TRUE.equals(self.getHasPreExistingConditions())) {
            riskFactors.add("Pre-existing conditions - expect 20-30% higher premium");
            premiumMultiplier *= 1.25;
        }
        if (self != null && Boolean.TRUE.equals(self.getIsSmoker())) {
            riskFactors.add("Smoker - expect 10-15% higher premium");
            premiumMultiplier *= 1.1;
        }
        if (children.size() > 2) {
            riskFactors.add("Multiple children - consider ₹15-20L floater instead");
        }
        
        // Estimate premium for family floater (~2.5% for family with inflation)
        familyBreakdown.estimatedPremium = (double) Math.round(familyFloaterCover * 0.025 * premiumMultiplier);
        familyBreakdown.riskFactors = riskFactors;
        rec.memberBreakdown.add(familyBreakdown);
        
        // Super top-up recommendation (supplementary - activates only after base policy exhausted)
        MemberHealthBreakdown topUpBreakdown = new MemberHealthBreakdown();
        topUpBreakdown.memberType = "Super Top-Up";
        topUpBreakdown.recommendedPolicyType = "Super Top-Up (Deductible: ₹" + formatAmount(familyFloaterCover) + ")";
        topUpBreakdown.recommendedCover = superTopUpCover;
        topUpBreakdown.reasoning = String.format(
                "For claims exceeding ₹%s. Provides ₹%s additional coverage at ~₹%s/year premium.",
                formatAmount(familyFloaterCover), formatAmount(superTopUpCover), 
                formatAmount(superTopUpCover * 0.003));
        topUpBreakdown.estimatedPremium = (double) Math.round(superTopUpCover * 0.003); // ~0.3% of cover (~₹30K for 1Cr)
        topUpBreakdown.isSupplementary = true; // Don't add to base total - it's layered coverage
        rec.memberBreakdown.add(topUpBreakdown);
        
        // Parents - separate policies recommended for seniors
        for (FamilyMember parent : parents) {
            MemberHealthBreakdown parentBreakdown = new MemberHealthBreakdown();
            parentBreakdown.memberName = parent.getName();
            parentBreakdown.memberType = parent.getRelationship().toString();
            
            Integer parentAge = parent.getCurrentAge();
            double parentExistingCover = parent.getExistingHealthCover() != null ? parent.getExistingHealthCover() : 0;
            double recommendedCover;
            
            if (parentAge != null && parentAge >= 80) {
                parentBreakdown.recommendedPolicyType = "Senior Citizen Policy (80+)";
                recommendedCover = HEALTH_COVER_SUPER_SENIOR;
                parentBreakdown.reasoning = String.format("Age %d: Super senior citizens need specialized policies. %s",
                        parentAge,
                        parentExistingCover > 0 ? "Has existing cover ₹" + formatAmount(parentExistingCover) : "No existing cover");
                parentBreakdown.estimatedPremium = (double) Math.round(HEALTH_COVER_SUPER_SENIOR * 0.08);
            } else if (parentAge != null && parentAge >= 60) {
                parentBreakdown.recommendedPolicyType = "Senior Citizen Policy";
                recommendedCover = HEALTH_COVER_PER_SENIOR;
                parentBreakdown.reasoning = String.format("Age %d: Separate policy recommended for seniors. %s",
                        parentAge,
                        parentExistingCover > 0 ? "Has existing cover ₹" + formatAmount(parentExistingCover) : "No existing cover");
                parentBreakdown.estimatedPremium = (double) Math.round(HEALTH_COVER_PER_SENIOR * 0.05);
            } else {
                parentBreakdown.recommendedPolicyType = "Individual/Floater";
                recommendedCover = HEALTH_COVER_PER_SENIOR;
                parentBreakdown.reasoning = String.format("Age %d: Can be included in family floater or separate policy. %s",
                        parentAge != null ? parentAge : 0,
                        parentExistingCover > 0 ? "Has existing cover ₹" + formatAmount(parentExistingCover) : "No existing cover");
                parentBreakdown.estimatedPremium = (double) Math.round(HEALTH_COVER_PER_SENIOR * 0.03);
            }
            
            // Show gap - what additional cover is needed
            parentBreakdown.recommendedCover = Math.max(0, recommendedCover - parentExistingCover);
            
            List<String> parentRiskFactors = new ArrayList<>();
            if (Boolean.TRUE.equals(parent.getHasPreExistingConditions())) {
                parentRiskFactors.add("Pre-existing conditions: " + (parent.getPreExistingConditions() != null ? parent.getPreExistingConditions() : "Yes"));
                parentRiskFactors.add("Expect waiting period and higher premium");
            }
            if (parentExistingCover > 0 && parentExistingCover >= recommendedCover) {
                parentRiskFactors.add("✓ Adequate coverage already in place");
            }
            if (!parentRiskFactors.isEmpty()) {
                parentBreakdown.riskFactors = parentRiskFactors;
            }
            
            rec.memberBreakdown.add(parentBreakdown);
        }
        
        // Calculate totals - EXCLUDE supplementary coverage (Super Top-Up) from base total
        // Super Top-Up is layered coverage, not additive
        rec.totalRecommendedCover = rec.memberBreakdown.stream()
                .filter(m -> !m.isSupplementary) // Exclude Super Top-Up from base total
                .mapToDouble(m -> m.recommendedCover)
                .sum();
        rec.gap = Math.max(0, rec.totalRecommendedCover - existingCover);
        rec.totalEstimatedPremium = rec.memberBreakdown.stream()
                .mapToDouble(m -> m.estimatedPremium != null ? m.estimatedPremium : 0)
                .sum();
        
        // Policy suggestions - practical and actionable
        rec.policySuggestions = new ArrayList<>();
        if (existingCover < BASE_HEALTH_COVER_FAMILY) {
            rec.policySuggestions.add("Start with ₹" + formatAmount(BASE_HEALTH_COVER_FAMILY) + " Family Floater as your base policy");
        }
        rec.policySuggestions.add("Add ₹" + formatAmount(SUPER_TOP_UP_COVER) + " Super Top-Up for catastrophic illness coverage at just ~₹30K/year");
        rec.policySuggestions.add("Set your Family Floater sum as the Super Top-Up deductible to avoid coverage gaps");
        if (!parents.isEmpty()) {
            rec.policySuggestions.add("Buy separate senior citizen policies for parents before they turn 60 (significantly lower premiums)");
        }
        rec.policySuggestions.add("Look for policies with: No co-pay, Restoration benefit, No room rent capping");
        
        return rec;
    }

    /**
     * Calculate term insurance recommendation
     */
    private TermRecommendation calculateTermRecommendation(
            List<FamilyMember> family, int userAge, double annualIncome,
            double totalLiabilities, double monthlyExpenses, double existingCover, int yearsToRetirement) {
        
        TermRecommendation rec = new TermRecommendation();
        rec.existingCover = existingCover;
        rec.breakdown = new TermBreakdown();
        
        // Income multiplier based on age
        int multiplier;
        if (userAge < 35) {
            multiplier = INCOME_MULTIPLIER_YOUNG;
            rec.breakdown.multiplierReason = "Under 35: 15x income recommended for longer financial runway";
        } else if (userAge < 45) {
            multiplier = INCOME_MULTIPLIER_MIDDLE;
            rec.breakdown.multiplierReason = "35-45: 12x income recommended";
        } else {
            multiplier = INCOME_MULTIPLIER_OLDER;
            rec.breakdown.multiplierReason = "45+: 10x income recommended as closer to retirement";
        }
        
        // Income replacement
        rec.breakdown.annualIncome = annualIncome;
        rec.breakdown.incomeMultiplier = multiplier;
        rec.breakdown.incomeReplacement = annualIncome * multiplier;
        
        // Liability coverage
        rec.breakdown.totalLiabilities = totalLiabilities;
        rec.breakdown.liabilityCoverage = totalLiabilities; // Cover 100% of liabilities
        
        // Expense coverage (for family sustenance)
        double annualExpenses = monthlyExpenses * 12;
        rec.breakdown.annualExpenses = annualExpenses;
        rec.breakdown.expenseYears = EXPENSE_YEARS_COVERAGE;
        rec.breakdown.expenseCoverage = annualExpenses * EXPENSE_YEARS_COVERAGE;
        
        // Children's future expenses (education, marriage)
        double childrenFutureCost = 0;
        List<FamilyMember> children = family.stream()
                .filter(m -> m.getRelationship() == FamilyMember.Relationship.CHILD)
                .toList();
        
        for (FamilyMember child : children) {
            Integer childAge = child.getCurrentAge();
            if (childAge != null && childAge < 25) {
                // Estimate education + marriage cost
                double educationCost = 2500000; // ₹25 Lakhs education
                double marriageCost = 1500000;   // ₹15 Lakhs marriage
                
                // Adjust based on age (younger = more years = more inflation)
                int yearsToIndependence = Math.max(0, 25 - childAge);
                double inflationFactor = Math.pow(1.06, yearsToIndependence); // 6% inflation
                
                childrenFutureCost += (educationCost + marriageCost) * inflationFactor;
            }
        }
        rec.breakdown.childrenFutureCost = childrenFutureCost;
        rec.breakdown.childCount = children.size();
        
        // Spouse's financial situation
        FamilyMember spouse = family.stream()
                .filter(m -> m.getRelationship() == FamilyMember.Relationship.SPOUSE)
                .findFirst().orElse(null);
        
        double spouseAdjustment = 1.0;
        if (spouse != null) {
            if (Boolean.TRUE.equals(spouse.getIsEarning()) && spouse.getMonthlyIncome() != null) {
                double spouseIncome = spouse.getMonthlyIncome() * 12;
                // Reduce cover proportionally to spouse's income contribution
                double incomeRatio = spouseIncome / (annualIncome + spouseIncome);
                spouseAdjustment = 1 - (incomeRatio * 0.5); // Reduce by 50% of spouse's contribution
                rec.breakdown.spouseAdjustmentReason = String.format(
                        "Spouse earning ₹%s/year - adjusted cover by %.0f%%",
                        formatAmount(spouseIncome), (1 - spouseAdjustment) * 100);
            } else {
                rec.breakdown.spouseAdjustmentReason = "Non-earning spouse - full coverage recommended";
            }
        }
        rec.breakdown.spouseAdjustmentFactor = spouseAdjustment;
        
        // Calculate total recommendation
        double totalRequired = rec.breakdown.incomeReplacement +
                              rec.breakdown.liabilityCoverage +
                              rec.breakdown.expenseCoverage +
                              rec.breakdown.childrenFutureCost;
        
        // Apply spouse adjustment
        totalRequired *= spouseAdjustment;
        
        // Round to nearest 10 Lakhs
        totalRequired = Math.ceil(totalRequired / 1000000) * 1000000;
        
        rec.totalRecommendedCover = totalRequired;
        rec.gap = Math.max(0, totalRequired - existingCover);
        
        // Coverage till age recommendation
        rec.recommendedCoverageAge = Math.max(65, userAge + yearsToRetirement + 5);
        
        // Premium estimate (rough: 0.1-0.3% of cover depending on age)
        double premiumRate = userAge < 35 ? 0.001 : (userAge < 45 ? 0.0015 : 0.0025);
        rec.estimatedAnnualPremium = Math.round(totalRequired * premiumRate);
        
        // Policy suggestions
        rec.suggestions = new ArrayList<>();
        if (existingCover < totalRequired * 0.5) {
            rec.suggestions.add("Critically underinsured! Prioritize getting adequate term cover immediately");
        }
        rec.suggestions.add("Buy from multiple insurers to spread risk (max ₹1-2 Cr per insurer)");
        rec.suggestions.add("Consider increasing cover policy with annual increase option");
        rec.suggestions.add("Add critical illness rider (worth 25-50% of base cover)");
        rec.suggestions.add("Add accidental death benefit rider");
        if (userAge < 40) {
            rec.suggestions.add("Lock in premiums now - they increase significantly with age");
        }
        
        return rec;
    }

    /**
     * Generate summary with action items
     */
    private RecommendationSummary generateSummary(InsuranceRecommendation rec, double annualIncome) {
        RecommendationSummary summary = new RecommendationSummary();
        
        summary.healthCoverGap = rec.healthRecommendation.gap;
        summary.termCoverGap = rec.termRecommendation.gap;
        summary.totalEstimatedPremium = rec.healthRecommendation.totalEstimatedPremium +
                                        rec.termRecommendation.estimatedAnnualPremium;
        
        // Premium as percentage of income
        summary.premiumAsPercentOfIncome = annualIncome > 0 
                ? (summary.totalEstimatedPremium / annualIncome) * 100 : 0;
        
        // Urgency assessment
        List<String> urgentActions = new ArrayList<>();
        List<String> recommendedActions = new ArrayList<>();
        
        // Health insurance urgency
        if (rec.healthRecommendation.existingCover < 500000) {
            urgentActions.add("Get basic health insurance immediately (min ₹5 Lakhs)");
        } else if (rec.healthRecommendation.gap > 0) {
            recommendedActions.add("Increase health coverage or add super top-up");
        }
        
        // Term insurance urgency
        if (rec.termRecommendation.existingCover == 0 && annualIncome > 0) {
            urgentActions.add("Buy term insurance immediately - you have zero life cover");
        } else if (rec.termRecommendation.gap > rec.termRecommendation.totalRecommendedCover * 0.5) {
            urgentActions.add("Significantly underinsured - increase term cover");
        } else if (rec.termRecommendation.gap > 0) {
            recommendedActions.add("Consider increasing term cover to close gap");
        }
        
        summary.urgentActions = urgentActions;
        summary.recommendedActions = recommendedActions;
        
        // Overall score (0-100)
        double healthScore = rec.healthRecommendation.existingCover >= rec.healthRecommendation.totalRecommendedCover ? 100 :
                            (rec.healthRecommendation.existingCover / rec.healthRecommendation.totalRecommendedCover) * 100;
        double termScore = rec.termRecommendation.existingCover >= rec.termRecommendation.totalRecommendedCover ? 100 :
                          (rec.termRecommendation.existingCover / rec.termRecommendation.totalRecommendedCover) * 100;
        
        summary.overallScore = (int) Math.round((healthScore * 0.4 + termScore * 0.6)); // Term weighted more
        
        if (summary.overallScore >= 80) {
            summary.overallStatus = "ADEQUATE";
            summary.overallMessage = "Your insurance coverage is adequate. Review annually.";
        } else if (summary.overallScore >= 50) {
            summary.overallStatus = "NEEDS_IMPROVEMENT";
            summary.overallMessage = "Your coverage has gaps. Consider the recommended actions.";
        } else {
            summary.overallStatus = "CRITICAL";
            summary.overallMessage = "Critical gaps in coverage! Take immediate action.";
        }
        
        return summary;
    }

    private String formatAmount(double amount) {
        if (amount >= 10000000) {
            return String.format("%.2f Cr", amount / 10000000);
        } else if (amount >= 100000) {
            return String.format("%.2f L", amount / 100000);
        } else {
            return String.format("%.0f", amount);
        }
    }

    // ========== Response DTOs ==========

    @lombok.Data
    public static class InsuranceRecommendation {
        HealthRecommendation healthRecommendation;
        TermRecommendation termRecommendation;
        RecommendationSummary summary;
    }

    @lombok.Data
    public static class HealthRecommendation {
        double existingCover;
        double totalRecommendedCover;
        double gap;
        double totalEstimatedPremium;
        List<MemberHealthBreakdown> memberBreakdown;
        List<String> policySuggestions;
    }

    @lombok.Data
    public static class MemberHealthBreakdown {
        String memberName;
        String memberType;
        String recommendedPolicyType;
        double recommendedCover;
        String reasoning;
        Double estimatedPremium;
        List<String> riskFactors;
        boolean isSupplementary; // True for Super Top-Up (doesn't add to base coverage)
    }

    @lombok.Data
    public static class TermRecommendation {
        double existingCover;
        double totalRecommendedCover;
        double gap;
        int recommendedCoverageAge;
        double estimatedAnnualPremium;
        TermBreakdown breakdown;
        List<String> suggestions;
    }

    @lombok.Data
    public static class TermBreakdown {
        double annualIncome;
        int incomeMultiplier;
        String multiplierReason;
        double incomeReplacement;
        double totalLiabilities;
        double liabilityCoverage;
        double annualExpenses;
        double expenseYears;
        double expenseCoverage;
        double childrenFutureCost;
        int childCount;
        double spouseAdjustmentFactor;
        String spouseAdjustmentReason;
    }

    @lombok.Data
    public static class RecommendationSummary {
        double healthCoverGap;
        double termCoverGap;
        double totalEstimatedPremium;
        double premiumAsPercentOfIncome;
        int overallScore;
        String overallStatus;
        String overallMessage;
        List<String> urgentActions;
        List<String> recommendedActions;
    }
}
