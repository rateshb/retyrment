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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "calendar_entries")
public class CalendarEntry {

    @Id
    private String id;
    
    private String userId;              // Owner of this record

    private String description;         // Payment description
    private CalendarCategory category;  // SIP, INSURANCE, EMI, EDUCATION, INVESTMENT, OTHER
    private Double amount;              // Payment amount
    private Frequency frequency;        // MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY
    private List<Integer> dueMonths;    // Which months this is due (1-12)
    private Boolean autoLinked;         // Is this linked to another entry?
    private String linkedTo;            // ID of linked entity (investment, insurance, loan)
    private String linkedType;          // Type of linked entity
    private Integer reminderDays;       // Days before due to show reminder
    private Boolean isActive;           // Is this entry active?

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum CalendarCategory {
        SIP,
        INSURANCE,
        EMI,
        EDUCATION,
        INVESTMENT,
        PPF,
        RD,
        OTHER
    }

    public enum Frequency {
        MONTHLY,
        QUARTERLY,
        HALF_YEARLY,
        YEARLY,
        ONE_TIME
    }
}
