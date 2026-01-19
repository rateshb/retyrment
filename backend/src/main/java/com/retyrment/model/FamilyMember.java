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
import java.time.Period;

/**
 * Represents a family member for insurance and financial planning.
 * Used for calculating insurance requirements, dependent expenses, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "family_members")
public class FamilyMember {

    @Id
    private String id;
    
    private String userId;              // Owner of this record

    private String name;                // Family member's name
    private Relationship relationship;  // SELF, SPOUSE, CHILD, PARENT, SIBLING, OTHER
    private LocalDate dateOfBirth;      // Date of birth
    private Gender gender;              // For insurance calculations
    
    // Employment & Income (for SELF, SPOUSE)
    private Boolean isEarning;          // Does this person earn?
    private Double monthlyIncome;       // Monthly income (if earning)
    private Double annualIncome;        // Annual income (calculated or entered)
    private Boolean hasOwnInsurance;    // Does this person have their own insurance?
    
    // Health Information (affects health insurance premium)
    private Boolean hasPreExistingConditions;
    private String preExistingConditions;  // e.g., "Diabetes, Hypertension"
    private Boolean isSmoker;
    private Boolean isAlcoholic;
    
    // Education (for children - affects financial planning)
    private EducationLevel currentEducation;  // For children
    private Integer expectedEducationEndAge;  // When education expenses end
    
    // Dependency (for insurance calculation)
    private Boolean isDependent;        // Is financially dependent on user?
    private Integer dependencyEndAge;   // Age until which dependent (e.g., child till 25)
    
    // Insurance coverage (existing)
    private Double existingHealthCover;  // Existing health insurance coverage
    private Double existingLifeCover;    // Existing life insurance coverage
    
    // For parents
    private Boolean livesWithUser;       // For senior citizen benefits
    private Boolean hasSeparateHealthPolicy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Relationship {
        SELF,
        SPOUSE,
        CHILD,
        PARENT,        // Father/Mother
        PARENT_IN_LAW, // Father-in-law/Mother-in-law
        SIBLING,
        OTHER
    }

    public enum Gender {
        MALE,
        FEMALE,
        OTHER
    }

    public enum EducationLevel {
        PRE_SCHOOL,
        PRIMARY,       // Class 1-5
        MIDDLE,        // Class 6-8
        SECONDARY,     // Class 9-10
        HIGHER_SECONDARY, // Class 11-12
        UNDERGRADUATE,
        POSTGRADUATE,
        WORKING,
        NOT_APPLICABLE
    }

    /**
     * Calculate current age from date of birth
     */
    public Integer getCurrentAge() {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Check if this member is a minor (under 18)
     */
    public boolean isMinor() {
        Integer age = getCurrentAge();
        return age != null && age < 18;
    }

    /**
     * Check if this member is a senior citizen (60+)
     */
    public boolean isSeniorCitizen() {
        Integer age = getCurrentAge();
        return age != null && age >= 60;
    }

    /**
     * Check if this member is super senior (80+)
     */
    public boolean isSuperSenior() {
        Integer age = getCurrentAge();
        return age != null && age >= 80;
    }

    /**
     * Get years until dependency ends (for children)
     */
    public Integer getYearsOfDependency() {
        Integer age = getCurrentAge();
        if (age == null || dependencyEndAge == null) {
            return null;
        }
        return Math.max(0, dependencyEndAge - age);
    }
}
