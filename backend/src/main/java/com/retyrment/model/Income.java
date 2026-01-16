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
@Document(collection = "income")
public class Income {

    @Id
    private String id;
    
    private String userId;              // Owner of this record

    @NotBlank(message = "Income source is required")
    @Size(max = 100, message = "Source name cannot exceed 100 characters")
    private String source;              // e.g., "Salary - TCS", "Freelance"
    
    @NotNull(message = "Monthly amount is required")
    @Positive(message = "Monthly amount must be positive")
    @Max(value = 100000000, message = "Monthly amount cannot exceed 10 crore")
    private Double monthlyAmount;       // Current monthly income
    
    @Min(value = 0, message = "Annual increment cannot be negative")
    @Max(value = 100, message = "Annual increment cannot exceed 100%")
    private Double annualIncrement;     // Expected yearly increment % (default: 7)
    
    @PastOrPresent(message = "Start date cannot be in the future")
    private LocalDate startDate;        // When this income started
    
    private Boolean isActive;           // Is this income source active?

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
