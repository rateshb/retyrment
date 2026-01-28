package com.retyrment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * User-specific settings for financial calculations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_settings")
public class UserSettings {
    
    @Id
    private String id;
    
    private String userId;
    
    // Age & Timeline
    private Integer currentAge;
    private Integer retirementAge;
    private Integer lifeExpectancy;
    
    // Return Rates
    private Double inflationRate;
    private Double epfReturn;
    private Double ppfReturn;
    private Double mfEquityReturn;
    private Double mfDebtReturn;
    private Double npsReturn;
    private Double fdReturn;
    
    // Other Settings
    private Integer emergencyFundMonths;
    private Double sipStepup;
    
    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
