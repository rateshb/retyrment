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
    
    // Money-back specific fields
    private String moneyBackYears;      // Years when money back is received, e.g., "5,10,15"
    private Double moneyBackPercent;    // Percentage of sum assured received each time
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
}
