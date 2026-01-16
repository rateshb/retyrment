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
@Document(collection = "goals")
public class Goal {

    @Id
    private String id;
    
    private String userId;              // Owner of this record

    @Size(max = 10, message = "Icon cannot exceed 10 characters")
    private String icon;                // Emoji icon for the goal
    
    @NotBlank(message = "Goal name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;                // Goal name
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;         // Additional details
    
    @NotNull(message = "Target amount is required")
    @Positive(message = "Target amount must be positive")
    private Double targetAmount;        // Target amount in today's value
    
    @NotNull(message = "Target year is required")
    @Min(value = 2024, message = "Target year must be 2024 or later")
    @Max(value = 2100, message = "Target year cannot exceed 2100")
    private Integer targetYear;         // Target year to achieve
    
    private Priority priority;          // HIGH, MEDIUM, LOW
    
    private Boolean isRecurring;        // Is this a recurring goal?
    
    private RecurrenceType recurrence;  // YEARLY for recurring goals

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Priority {
        HIGH,
        MEDIUM,
        LOW
    }

    public enum RecurrenceType {
        YEARLY,
        NONE
    }
}
