package com.retyrment.model;

import jakarta.validation.constraints.*;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "expenses")
public class Expense {

    @Id
    private String id;
    
    private String userId;              // Owner of this record

    @NotNull(message = "Expense category is required")
    private ExpenseCategory category;   // RENT, UTILITIES, GROCERIES, SCHOOL_FEE, etc.
    
    @NotBlank(message = "Expense name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;                // Description (e.g., "Child 1 School Fee", "Rent")
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Max(value = 10000000, message = "Amount cannot exceed 1 crore")
    private Double amount;              // Amount per frequency period
    
    // Legacy field - kept for backward compatibility, will be calculated from amount and frequency
    private Double monthlyAmount;       // Monthly equivalent (calculated)
    
    private Boolean isFixed;            // Fixed or variable expense
    
    // ===== NEW FIELDS FOR TIME-BOUND EXPENSES =====
    
    /**
     * Frequency of the expense payment
     * MONTHLY - paid every month (default, like rent, utilities)
     * QUARTERLY - paid every 3 months (like school fees)
     * HALF_YEARLY - paid every 6 months
     * YEARLY - paid once a year (like annual school fees, insurance)
     * ONE_TIME - single payment (like admission fee)
     */
    @Builder.Default
    private ExpenseFrequency frequency = ExpenseFrequency.MONTHLY;
    
    /**
     * Whether this expense has an end date/condition
     * If true, the expense will stop at endDate or when dependent reaches endAge
     */
    @Builder.Default
    private Boolean isTimeBound = false;
    
    /**
     * Start date of the expense (when it begins)
     * Useful for future expenses like upcoming school admission
     */
    private LocalDate startDate;
    
    /**
     * End date of the expense (when it stops)
     * E.g., when child finishes school/college
     */
    private LocalDate endDate;
    
    /**
     * End condition based on age (alternative to endDate)
     * E.g., expense ends when dependent turns 22 (finishes college)
     */
    private Integer endAge;
    
    /**
     * Name of the dependent (child, parent, etc.) this expense is for
     * Helps in tracking and reporting
     */
    private String dependentName;
    
    /**
     * Current age of the dependent
     * Used to calculate when the expense will end based on endAge
     */
    private Integer dependentCurrentAge;
    
    /**
     * Date of birth of the dependent (more accurate than current age)
     * Age will be calculated from this if provided
     */
    private LocalDate dependentDob;
    
    /**
     * Expected annual increase in the expense (e.g., school fee hike)
     * Percentage value (e.g., 8.0 for 8% annual increase)
     */
    private Double annualIncreasePercent;
    
    /**
     * Notes about the expense
     */
    private String notes;
    
    /**
     * Whether this expense continues after retirement
     * Calculated based on endDate/endAge vs retirement date
     */
    private Boolean continuesAfterRetirement;
    
    /**
     * The year when this expense ends (calculated field)
     * Helps in retirement planning
     */
    private Integer endYear;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum ExpenseCategory {
        // Regular expenses
        RENT,
        UTILITIES,
        GROCERIES,
        TRANSPORT,
        ENTERTAINMENT,
        HEALTHCARE,
        SHOPPING,
        DINING,
        TRAVEL,
        SUBSCRIPTIONS,
        
        // Education-related (time-bound)
        SCHOOL_FEE,
        COLLEGE_FEE,
        TUITION,
        COACHING,
        BOOKS_SUPPLIES,
        HOSTEL,
        
        // Dependent care (time-bound)
        CHILDCARE,
        DAYCARE,
        ELDERLY_CARE,
        
        // Loans/EMIs (time-bound) - for tracking non-loan regular payments
        MAINTENANCE,
        SOCIETY_CHARGES,
        
        // Insurance premiums (if not tracked in Insurance module)
        INSURANCE_PREMIUM,
        
        OTHER
    }
    
    public enum ExpenseFrequency {
        MONTHLY(1),
        QUARTERLY(3),
        HALF_YEARLY(6),
        YEARLY(12),
        ONE_TIME(0);
        
        private final int monthsInterval;
        
        ExpenseFrequency(int monthsInterval) {
            this.monthsInterval = monthsInterval;
        }
        
        public int getMonthsInterval() {
            return monthsInterval;
        }
    }
    
    // ===== HELPER METHODS =====
    
    /**
     * Calculate monthly equivalent amount based on frequency
     */
    public Double getMonthlyEquivalent() {
        if (amount == null) return 0.0;
        if (frequency == null || frequency == ExpenseFrequency.MONTHLY) {
            return amount;
        }
        if (frequency == ExpenseFrequency.ONE_TIME) {
            return 0.0; // One-time expenses don't have monthly equivalent
        }
        return amount / frequency.getMonthsInterval();
    }
    
    /**
     * Calculate yearly amount
     */
    public Double getYearlyAmount() {
        if (amount == null) return 0.0;
        if (frequency == null || frequency == ExpenseFrequency.MONTHLY) {
            return amount * 12;
        }
        if (frequency == ExpenseFrequency.ONE_TIME) {
            return amount;
        }
        return (amount / frequency.getMonthsInterval()) * 12;
    }
    
    /**
     * Calculate the year when this expense ends
     */
    public Integer calculateEndYear() {
        if (!Boolean.TRUE.equals(isTimeBound)) {
            return null; // Expense continues indefinitely
        }
        
        // If endDate is specified, use it
        if (endDate != null) {
            return endDate.getYear();
        }
        
        // If endAge is specified, calculate based on dependent's age
        if (endAge != null && dependentCurrentAge != null) {
            int yearsRemaining = endAge - dependentCurrentAge;
            return LocalDate.now().getYear() + yearsRemaining;
        }
        
        // If endAge is specified with DOB, calculate more accurately
        if (endAge != null && dependentDob != null) {
            return dependentDob.plusYears(endAge).getYear();
        }
        
        return null;
    }
    
    /**
     * Calculate dependent's current age from DOB
     */
    public Integer getDependentAge() {
        if (dependentDob != null) {
            return LocalDate.now().getYear() - dependentDob.getYear();
        }
        return dependentCurrentAge;
    }
    
    /**
     * Check if expense is active for a given year
     */
    public boolean isActiveInYear(int year) {
        // Check start date
        if (startDate != null && startDate.getYear() > year) {
            return false;
        }
        
        // If not time-bound, always active (after start)
        if (!Boolean.TRUE.equals(isTimeBound)) {
            return true;
        }
        
        // Check end conditions
        Integer calculatedEndYear = calculateEndYear();
        if (calculatedEndYear != null && year > calculatedEndYear) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get the inflated amount for a future year
     */
    public Double getInflatedYearlyAmount(int yearsFromNow) {
        double yearlyAmount = getYearlyAmount();
        if (annualIncreasePercent != null && annualIncreasePercent > 0) {
            return yearlyAmount * Math.pow(1 + annualIncreasePercent / 100, yearsFromNow);
        }
        return yearlyAmount;
    }
    
    /**
     * Check if this expense will continue after retirement
     */
    public boolean willContinueAfterRetirement(int retirementYear) {
        if (!Boolean.TRUE.equals(isTimeBound)) {
            return true; // Non-time-bound expenses continue
        }
        
        Integer calculatedEndYear = calculateEndYear();
        if (calculatedEndYear == null) {
            return true; // No end year means it continues
        }
        
        return calculatedEndYear >= retirementYear;
    }
}
