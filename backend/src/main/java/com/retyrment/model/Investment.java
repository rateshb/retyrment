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
@Document(collection = "investments")
public class Investment {

    @Id
    private String id;
    
    private String userId;              // Owner of this record

    @NotNull(message = "Investment type is required")
    private InvestmentType type;        // MUTUAL_FUND, STOCK, FD, RD, PPF, EPF, NPS, REAL_ESTATE, GOLD, OTHER, CASH
    
    @NotBlank(message = "Investment name is required")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    private String name;                // Fund name, stock name, bank name, etc.
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;         // Additional details

    // Amounts
    @PositiveOrZero(message = "Invested amount cannot be negative")
    private Double investedAmount;      // Total amount invested
    
    @PositiveOrZero(message = "Current value cannot be negative")
    private Double currentValue;        // Current market value
    
    @PositiveOrZero(message = "Purchase price cannot be negative")
    private Double purchasePrice;       // For real estate - purchase price
    
    // For SIP/Recurring investments
    @PositiveOrZero(message = "Monthly SIP cannot be negative")
    private Double monthlySip;          // Monthly SIP amount
    
    @Min(value = 1, message = "SIP day must be between 1 and 28")
    @Max(value = 28, message = "SIP day must be between 1 and 28")
    private Integer sipDay;             // Day of month when SIP is debited (1-28)
    
    @PositiveOrZero(message = "Yearly contribution cannot be negative")
    private Double yearlyContribution;  // For PPF
    
    @Min(value = 1, message = "RD day must be between 1 and 28")
    @Max(value = 28, message = "RD day must be between 1 and 28")
    private Integer rdDay;              // Day of month when RD is debited (1-28)
    
    // Dates
    @PastOrPresent(message = "Purchase date cannot be in the future")
    private LocalDate purchaseDate;     // When investment was made / First investment date
    
    @PastOrPresent(message = "Evaluation date cannot be in the future")
    private LocalDate evaluationDate;   // Date when current value was evaluated/checked
    
    private LocalDate maturityDate;     // For FD, PPF
    
    @PastOrPresent(message = "Start date cannot be in the future")
    private LocalDate startDate;        // For SIP, RD - when recurring started

    // Returns
    @Min(value = 0, message = "Interest rate cannot be negative")
    @Max(value = 100, message = "Interest rate cannot exceed 100%")
    private Double interestRate;        // For FD, RD - actual rate
    
    @Min(value = -50, message = "Expected return cannot be less than -50%")
    @Max(value = 100, message = "Expected return cannot exceed 100%")
    private Double expectedReturn;      // Expected annual return %

    // Emergency fund tagging (primarily for FD)
    private Boolean isEmergencyFund;    // If true, excluded from retirement corpus

    // Tenure
    @Min(value = 1, message = "Tenure must be at least 1 month")
    @Max(value = 600, message = "Tenure cannot exceed 50 years (600 months)")
    private Integer tenureMonths;       // For RD, FD

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum InvestmentType {
        MUTUAL_FUND,
        STOCK,
        FD,
        RD,
        PPF,
        EPF,
        NPS,
        REAL_ESTATE,
        GOLD,
        CRYPTO,
        CASH,
        OTHER
    }
}
