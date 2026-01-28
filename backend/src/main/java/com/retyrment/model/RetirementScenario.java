package com.retyrment.model;

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
import java.util.Collections;
import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode
@lombok.ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "retirement_scenarios")
public class RetirementScenario {

    @Id
    private String id;
    
    private String userId;              // Owner of this record

    private String name;                // Scenario name (e.g., "Conservative", "Aggressive")
    private String description;         // Description
    private Integer currentAge;         // User's current age
    private Integer retirementAge;      // Planned retirement age
    private Integer lifeExpectancy;     // Expected life span

    // Return assumptions (complex - for multi-period scenarios)
    private List<PeriodReturn> epfReturns;
    private List<PeriodReturn> ppfReturns;
    private List<PeriodReturn> mfReturns;
    private List<PeriodReturn> debtReturns;

    // Defensive getters to prevent EI_EXPOSE_REP
    public List<PeriodReturn> getEpfReturns() {
        return epfReturns == null ? null : Collections.unmodifiableList(new ArrayList<>(epfReturns));
    }

    public void setEpfReturns(List<PeriodReturn> epfReturns) {
        this.epfReturns = epfReturns == null ? null : new ArrayList<>(epfReturns);
    }

    public List<PeriodReturn> getPpfReturns() {
        return ppfReturns == null ? null : Collections.unmodifiableList(new ArrayList<>(ppfReturns));
    }

    public void setPpfReturns(List<PeriodReturn> ppfReturns) {
        this.ppfReturns = ppfReturns == null ? null : new ArrayList<>(ppfReturns);
    }

    public List<PeriodReturn> getMfReturns() {
        return mfReturns == null ? null : Collections.unmodifiableList(new ArrayList<>(mfReturns));
    }

    public void setMfReturns(List<PeriodReturn> mfReturns) {
        this.mfReturns = mfReturns == null ? null : new ArrayList<>(mfReturns);
    }

    public List<PeriodReturn> getDebtReturns() {
        return debtReturns == null ? null : Collections.unmodifiableList(new ArrayList<>(debtReturns));
    }

    public void setDebtReturns(List<PeriodReturn> debtReturns) {
        this.debtReturns = debtReturns == null ? null : new ArrayList<>(debtReturns);
    }

    // Simple return assumptions (for quick adjustments from frontend)
    private Double epfReturn;           // Simple EPF return %
    private Double ppfReturn;           // Simple PPF return %
    private Double mfReturn;            // Simple MF return %
    private Double npsReturn;           // Simple NPS return %
    private Double inflation;           // Alias for inflationRate

    // Other assumptions
    private Double inflationRate;       // Annual inflation rate
    private Double sipStepUpPercent;    // Annual SIP step-up %
    private Double sipStepup;           // Alias for sipStepUpPercent (frontend naming)
    private Double lumpsumAmount;       // Yearly lumpsum investment
    private String lumpsumFrequency;    // "yearly" or "monthly"
    
    // Settings effective year and income strategy
    private Integer effectiveFromYear;  // Year from which adjusted settings take effect (0 = current year)
    private String incomeStrategy;      // SUSTAINABLE, SAFE_4_PERCENT, SIMPLE_DEPLETION
    private Double corpusReturnRate;    // Expected return on corpus at retirement (default 10%)
    private Double withdrawalRate;      // Withdrawal rate from corpus (default 8%)
    
    // Rate reduction over time (PPF/EPF/FD rates decrease over years)
    private Boolean enableRateReduction;    // Enable rate reduction feature (default true)
    private Double rateReductionPercent;    // Rate reduction % (default 0.5%)
    private Integer rateReductionYears;     // Apply reduction every N years (default 5)

    private Boolean isDefault;          // Is this the default scenario?

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodReturn {
        private Integer fromYear;       // Start year of period
        private Integer toYear;         // End year of period
        private Double rate;            // Return rate for this period
    }
}
