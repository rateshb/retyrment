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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "expenses")
public class Expense {

    @Id
    private String id;
    
    private String userId;              // Owner of this record

    @NotNull(message = "Expense category is required")
    private ExpenseCategory category;   // RENT, UTILITIES, GROCERIES, etc.
    
    @NotBlank(message = "Expense name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;                // Description
    
    @NotNull(message = "Monthly amount is required")
    @Positive(message = "Monthly amount must be positive")
    @Max(value = 10000000, message = "Monthly amount cannot exceed 1 crore")
    private Double monthlyAmount;       // Monthly expense amount
    
    private Boolean isFixed;            // Fixed or variable expense

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum ExpenseCategory {
        RENT,
        UTILITIES,
        GROCERIES,
        TRANSPORT,
        ENTERTAINMENT,
        EDUCATION,
        HEALTHCARE,
        SHOPPING,
        DINING,
        TRAVEL,
        SUBSCRIPTIONS,
        OTHER
    }
}
