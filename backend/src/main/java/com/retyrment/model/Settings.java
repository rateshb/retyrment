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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "settings")
public class Settings {

    @Id
    private String id;
    
    private String userId;              // Owner of this record

    // User info (for personalization)
    private String userName;
    private Integer currentAge;
    private Integer retirementAge;
    private Integer lifeExpectancy;

    // Default return assumptions
    private Double inflationRate;
    private Double epfReturn;
    private Double ppfReturn;
    private Double mfEquityReturn;
    private Double mfDebtReturn;
    private Double fdReturn;
    private Double realEstateReturn;
    private Double goldReturn;
    private Double savingsReturn;
    private Double incomeGrowth;

    // Display preferences
    private String currency;
    private String dateFormat;
    private String theme;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
