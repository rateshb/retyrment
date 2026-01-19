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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Financial goal with support for one-time and recurring goals.
 * 
 * Examples:
 * - One-time: Child's education (₹25L in 2030)
 * - Recurring: Annual vacation (₹2L every year from 2025 to 2045)
 * - Recurring with interval: Car replacement (₹15L every 5 years)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "goals")
public class Goal {

    @Id
    private String id;
    
    private String userId;              // Owner of this record

    @Size(max = 10, message = "Icon cannot exceed 10 characters")
    private String icon;                // Emoji icon for the goal
    
    @NotBlank(message = "Goal name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;                // Goal name
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;         // Additional details
    
    @NotNull(message = "Target amount is required")
    @Positive(message = "Target amount must be positive")
    private Double targetAmount;        // Target amount in today's value
    
    @NotNull(message = "Target year is required")
    @Min(value = 2024, message = "Target year must be 2024 or later")
    @Max(value = 2100, message = "Target year cannot exceed 2100")
    private Integer targetYear;         // First occurrence year (or only year for one-time)
    
    private Priority priority;          // HIGH, MEDIUM, LOW
    
    // ========== RECURRING GOAL FIELDS ==========
    
    private Boolean isRecurring;        // Is this a recurring goal?
    
    /**
     * Interval between occurrences in years.
     * Examples:
     * - 1 = every year (annual vacation)
     * - 2 = every 2 years (biennial event)
     * - 5 = every 5 years (car replacement)
     */
    @Min(value = 1, message = "Recurrence interval must be at least 1 year")
    @Max(value = 20, message = "Recurrence interval cannot exceed 20 years")
    private Integer recurrenceInterval;
    
    /**
     * Last year of recurrence (inclusive).
     * If null, recurs until retirement year.
     * Example: Vacation every year from 2025 to 2045
     */
    @Max(value = 2100, message = "End year cannot exceed 2100")
    private Integer recurrenceEndYear;
    
    /**
     * Whether to adjust the amount for inflation each occurrence.
     * If true, amount increases by inflationRate each interval.
     * Example: ₹2L vacation in 2025 becomes ₹2.12L in 2026 (at 6% inflation)
     */
    @Builder.Default
    private Boolean adjustForInflation = true;
    
    /**
     * Custom inflation rate for this goal (if different from global).
     * Null means use global inflation rate.
     * Example: Education inflation at 10% vs general 6%
     */
    private Double customInflationRate;

    @Deprecated
    private RecurrenceType recurrence;  // Legacy field - use recurrenceInterval instead

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Priority {
        HIGH,
        MEDIUM,
        LOW
    }

    @Deprecated
    public enum RecurrenceType {
        YEARLY,
        NONE
    }

    /**
     * Generate all occurrences of this goal for retirement calculations.
     * For one-time goals, returns a single entry.
     * For recurring goals, expands to all years based on interval.
     * 
     * @param retirementYear The user's retirement year (default end for recurring)
     * @param inflationRate Global inflation rate (used if customInflationRate is null)
     * @return List of goal occurrences with year and inflated amount
     */
    public List<GoalOccurrence> expandOccurrences(int retirementYear, double inflationRate) {
        List<GoalOccurrence> occurrences = new ArrayList<>();
        
        // Handle null values
        if (targetYear == null || targetAmount == null) {
            return occurrences; // Return empty list for invalid goals
        }
        
        double amount = targetAmount;
        
        if (!Boolean.TRUE.equals(isRecurring)) {
            // One-time goal
            occurrences.add(new GoalOccurrence(targetYear, amount, name));
            return occurrences;
        }
        
        // Recurring goal
        int interval = recurrenceInterval != null ? recurrenceInterval : 1;
        int endYear = recurrenceEndYear != null ? recurrenceEndYear : retirementYear;
        double rate = customInflationRate != null ? customInflationRate : inflationRate;
        boolean inflate = Boolean.TRUE.equals(adjustForInflation);
        
        int occurrenceCount = 0;
        
        for (int year = targetYear; year <= endYear; year += interval) {
            // Calculate inflated amount if enabled
            double occurrenceAmount = amount;
            if (inflate && occurrenceCount > 0) {
                int yearsFromStart = year - targetYear;
                occurrenceAmount = amount * Math.pow(1 + rate / 100, yearsFromStart);
            }
            
            String occurrenceName = interval > 1 
                ? String.format("%s (%d)", name, year)
                : name;
            
            occurrences.add(new GoalOccurrence(year, Math.round(occurrenceAmount), occurrenceName));
            occurrenceCount++;
        }
        
        return occurrences;
    }

    /**
     * Represents a single occurrence of a goal (for calculations)
     */
    @Data
    @AllArgsConstructor
    public static class GoalOccurrence {
        private int year;
        private double amount;
        private String description;
    }
    
    /**
     * Calculate total cost of all occurrences (for summary display)
     */
    public double getTotalCost(int retirementYear, double inflationRate) {
        if (targetYear == null || targetAmount == null) {
            return 0.0; // Return 0 for invalid goals
        }
        return expandOccurrences(retirementYear, inflationRate).stream()
                .mapToDouble(GoalOccurrence::getAmount)
                .sum();
    }
    
    /**
     * Get number of occurrences
     */
    public int getOccurrenceCount(int retirementYear) {
        if (targetYear == null) {
            return 0;
        }
        if (!Boolean.TRUE.equals(isRecurring)) {
            return 1;
        }
        int interval = recurrenceInterval != null ? recurrenceInterval : 1;
        int endYear = recurrenceEndYear != null ? recurrenceEndYear : retirementYear;
        return Math.max(1, (endYear - targetYear) / interval + 1);
    }
}
