package com.retyrment.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "user_strategies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStrategy {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId;
    
    // Selected income strategy for retirement
    private String selectedIncomeStrategy; // SIMPLE_DEPLETION, SAFE_4_PERCENT, SUSTAINABLE
    
    // What-if scenarios user has enabled
    private Boolean sellIlliquidAssets;
    private Integer sellIlliquidAssetsYear; // Year when to sell
    private Double illiquidAssetsValue; // Expected value
    
    private Boolean reinvestMaturities;
    private Double expectedMaturitiesValue;
    
    private Boolean redirectLoanEMIs;
    private Integer loanEndYear;
    private Double monthlyEMIAmount;
    
    private Boolean increaseSIP;
    private Double sipIncreasePercent;
    private Double newSIPAmount;
    
    // Monthly savings allocation preferences
    private Double emergencyFundAllocation; // Percentage
    private Double corpusAllocation;
    private Double goalsAllocation;
    private Double flexibleAllocation;
    
    // Custom notes
    private String strategyNotes;
    
    // Computed projections based on selected strategy
    private Double projectedCorpusWithStrategy;
    private Double projectedGapWithStrategy;
    private Boolean isOnTrackWithStrategy;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Strategy simulation parameters
    private Map<String, Object> simulationParams;

    // Defensive getter/setter to prevent EI_EXPOSE_REP
    public Map<String, Object> getSimulationParams() {
        return simulationParams == null ? null : Collections.unmodifiableMap(new HashMap<>(simulationParams));
    }

    public void setSimulationParams(Map<String, Object> simulationParams) {
        this.simulationParams = simulationParams == null ? null : new HashMap<>(simulationParams);
    }
}
