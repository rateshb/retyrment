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
@Document(collection = "loans")
public class Loan {

    @Id
    private String id;
    
    private String userId;              // Owner of this record

    @NotNull(message = "Loan type is required")
    private LoanType type;              // HOME, VEHICLE, PERSONAL, EDUCATION, CREDIT_CARD
    
    @NotBlank(message = "Loan name/bank is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;                // Bank name or description
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;         // Additional details

    // Amounts
    @NotNull(message = "Original amount is required")
    @Positive(message = "Original amount must be positive")
    private Double originalAmount;      // Original loan amount
    
    @NotNull(message = "Outstanding amount is required")
    @PositiveOrZero(message = "Outstanding amount cannot be negative")
    private Double outstandingAmount;   // Current outstanding
    
    @NotNull(message = "EMI amount is required")
    @Positive(message = "EMI must be positive")
    private Double emi;                 // Monthly EMI

    @Min(value = 1, message = "EMI day must be between 1 and 31")
    @Max(value = 31, message = "EMI day must be between 1 and 31")
    private Integer emiDay;             // Day of month for EMI debit
    
    @NotNull(message = "Interest rate is required")
    @Min(value = 0, message = "Interest rate cannot be negative")
    @Max(value = 50, message = "Interest rate cannot exceed 50%")
    private Double interestRate;        // Annual interest rate %

    // Tenure
    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Tenure must be at least 1 month")
    @Max(value = 600, message = "Tenure cannot exceed 50 years")
    private Integer tenureMonths;       // Original tenure in months
    
    @PositiveOrZero(message = "Remaining months cannot be negative")
    private Integer remainingMonths;    // Remaining months
    
    @PositiveOrZero(message = "Moratorium months cannot be negative")
    private Integer moratoriumMonths;   // For education loan

    // Dates
    @NotNull(message = "Start date is required")
    private LocalDate startDate;        // Loan start date
    
    private LocalDate endDate;          // Expected end date

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum LoanType {
        HOME,
        VEHICLE,
        PERSONAL,
        EDUCATION,
        CREDIT_CARD,
        OTHER
    }
}
