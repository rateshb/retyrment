package com.retyrment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Insurance policy model supporting various types including:
 * - Term Life Insurance
 * - Health Insurance (Personal, Group, Family Floater)
 * - Investment-linked (ULIP, Endowment, Money-back)
 * - Annuity/Pension policies
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "insurance")
public class Insurance {

    @Id
    private String id;
    
    private String userId;              // Owner of this record

    private InsuranceType type;         // TERM_LIFE, HEALTH, ULIP, ENDOWMENT, MONEY_BACK, VEHICLE, OTHER
    private HealthInsuranceType healthType;  // For HEALTH: GROUP, PERSONAL, FAMILY_FLOATER
    private String company;             // Insurance company name
    private String policyName;          // Policy name
    private String policyNumber;        // Policy number

    // Coverage & Premium
    private Double sumAssured;          // Coverage amount
    private Double annualPremium;       // Yearly premium (total if paid in installments)
    private Double premiumAmount;       // Per-installment premium amount
    private PremiumFrequency premiumFrequency;  // MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY, SINGLE
    
    // Premium payment schedule (based on frequency)
    private Integer renewalMonth;       // For YEARLY: Month when renewal is due (1-12)
    private Integer renewalDay;         // Day of month for premium
    private String quarterlyMonths;     // For QUARTERLY: e.g., "1,4,7,10" (Jan, Apr, Jul, Oct)
    private String halfYearlyMonths;    // For HALF_YEARLY: e.g., "3,9" (Mar, Sep)
    
    // Premium payment duration
    private Boolean continuesAfterRetirement;   // Does premium continue after retirement?
    private Integer premiumEndAge;      // Age until which premium is payable
    private Integer premiumPaymentYears;// Number of years to pay premium (for limited pay policies)
    private Integer premiumPaymentEndYear; // Calendar year when premium payment ends

    // For investment-linked policies (ULIP, Endowment, Money-back)
    private Double fundValue;           // Current fund value
    private Double guaranteedReturns;   // Guaranteed return %
    private Double bonusAccrued;        // For traditional policies
    private Double maturityBenefit;     // Expected maturity amount
    
    // ========== ENHANCED MONEY-BACK FIELDS ==========
    
    /**
     * List of money-back payouts with different percentages at different years.
     * Supports complex plans like:
     * - Year 5: 20% of sum assured
     * - Year 10: 30% of sum assured
     * - Year 15: 50% of sum assured + bonus
     */
    private List<MoneyBackPayout> moneyBackPayouts;
    
    // Legacy money-back fields (kept for backward compatibility)
    @Deprecated
    private String moneyBackYears;      // Years when money back is received, e.g., "5,10,15"
    @Deprecated
    private Double moneyBackPercent;    // Percentage of sum assured received each time
    @Deprecated
    private Double moneyBackAmount;     // Fixed amount received each time
    
    // Annuity/Pension policies - pay for N years, receive monthly from N+1
    private Boolean isAnnuityPolicy;    // Is this an annuity/pension policy?
    private Integer annuityStartYear;   // Year when annuity payments begin
    private Double monthlyAnnuityAmount;// Monthly annuity amount received
    private Double annuityGrowthRate;   // Annual growth rate of annuity (%)

    // Tenure & Dates
    private Integer policyTerm;         // Policy term in years
    private Integer coverageEndAge;     // Age until which coverage continues (for term/health)
    private LocalDate startDate;        // Policy start date
    private LocalDate maturityDate;     // Maturity date (null for term/health - use coverageEndAge)
    private LocalDate nextPremiumDate;  // Next premium due date
    
    // For whom is this policy? (helps in recommendations)
    private String coveredMemberId;     // FamilyMember ID this policy covers
    private String coveredMemberName;   // Name for display

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum InsuranceType {
        TERM_LIFE,
        HEALTH,
        ULIP,
        ENDOWMENT,
        MONEY_BACK,
        ANNUITY,        // Pension/Annuity policies - pay for N years, receive monthly
        VEHICLE,
        OTHER
    }

    public enum HealthInsuranceType {
        GROUP,          // Employer-provided group health insurance
        PERSONAL,       // Individual health insurance
        FAMILY_FLOATER  // Family floater policy
    }

    public enum PremiumFrequency {
        MONTHLY,
        QUARTERLY,
        HALF_YEARLY,
        YEARLY,
        SINGLE
    }

    /**
     * Embedded class for money-back payout schedule.
     * Allows different percentages at different policy years.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoneyBackPayout {
        private Integer policyYear;      // Which policy year (e.g., 5, 10, 15, 20)
        private Double percentage;       // Percentage of sum assured (e.g., 20.0 for 20%)
        private Double fixedAmount;      // OR fixed amount (if not percentage-based)
        private Boolean includesBonus;   // Does this payout include accrued bonus?
        private String description;      // Optional description (e.g., "Survival Benefit 1")
        
        /**
         * Calculate the payout amount based on sum assured and bonus
         */
        public double calculatePayout(double sumAssured, double bonusAccrued) {
            double payout = 0;
            
            if (percentage != null && percentage > 0) {
                payout = sumAssured * percentage / 100;
            } else if (fixedAmount != null && fixedAmount > 0) {
                payout = fixedAmount;
            }
            
            if (Boolean.TRUE.equals(includesBonus) && bonusAccrued > 0) {
                // Proportional bonus based on percentage
                double bonusPortion = percentage != null ? bonusAccrued * percentage / 100 : 0;
                payout += bonusPortion;
            }
            
            return payout;
        }
    }

    /**
     * Get all money-back payouts (handles both new list and legacy fields)
     */
    public List<MoneyBackPayout> getAllMoneyBackPayouts() {
        if (moneyBackPayouts != null && !moneyBackPayouts.isEmpty()) {
            return new ArrayList<>(moneyBackPayouts);
        }
        
        // Convert legacy fields to list format
        if (moneyBackYears != null && !moneyBackYears.isEmpty()) {
            List<MoneyBackPayout> payouts = new ArrayList<>();
            String[] years = moneyBackYears.split(",");
            for (String yearStr : years) {
                try {
                    int year = Integer.parseInt(yearStr.trim());
                    MoneyBackPayout payout = MoneyBackPayout.builder()
                            .policyYear(year)
                            .percentage(moneyBackPercent)
                            .fixedAmount(moneyBackAmount)
                            .includesBonus(false)
                            .build();
                    payouts.add(payout);
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
            return payouts;
        }
        
        return new ArrayList<>();
    }

    /**
     * Calculate all money-back payouts with calendar years
     */
    public List<PayoutSchedule> getPayoutSchedule() {
        if (startDate == null || type != InsuranceType.MONEY_BACK) {
            return new ArrayList<>();
        }
        
        double bonus = bonusAccrued != null ? bonusAccrued : 0;
        double sumAssuredValue = sumAssured != null ? sumAssured : 0;
        
        return getAllMoneyBackPayouts().stream()
                .map(payout -> {
                    int calendarYear = startDate.getYear() + payout.getPolicyYear();
                    double amount = payout.calculatePayout(sumAssuredValue, bonus);
                    return new PayoutSchedule(
                            calendarYear,
                            payout.getPolicyYear(),
                            amount,
                            payout.getPercentage(),
                            payout.getDescription()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Schedule entry for money-back payouts with calendar year
     */
    @Data
    @AllArgsConstructor
    public static class PayoutSchedule {
        private int calendarYear;
        private int policyYear;
        private double amount;
        private Double percentage;
        private String description;
    }

    /**
     * Get total money-back payouts (excluding final maturity)
     */
    public double getTotalMoneyBackPayouts() {
        double bonus = bonusAccrued != null ? bonusAccrued : 0;
        double sumAssuredValue = sumAssured != null ? sumAssured : 0;
        
        return getAllMoneyBackPayouts().stream()
                .mapToDouble(p -> p.calculatePayout(sumAssuredValue, bonus))
                .sum();
    }
}
