package com.retyrment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_preferences")
public class UserPreference {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;              // Links to User

    // Regional Settings
    private String currency;            // INR, USD, EUR, GBP, etc.
    private String currencySymbol;      // ₹, $, €, £, etc.
    private String country;             // IN, US, UK, etc.
    private String locale;              // en-IN, en-US, etc.
    private String dateFormat;          // DD/MM/YYYY, MM/DD/YYYY, YYYY-MM-DD

    // Display Preferences
    private String numberFormat;        // Indian (12,34,567), Western (1,234,567)
    private Boolean showAmountInWords;  // Show amount text like "Ten Lakhs"
    private String theme;               // light, dark, system

    // Financial Year Settings
    private Integer financialYearStartMonth;  // 4 for April (India), 1 for January
    
    // Default Values for Calculations
    private Double defaultInflationRate;
    private Double defaultEquityReturn;
    private Double defaultDebtReturn;
    private Integer defaultRetirementAge;

    // Notification Preferences
    private Boolean emailNotifications;
    private Boolean paymentReminders;
    private Integer reminderDaysBefore;  // Days before payment to remind

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Currency enum for reference
    public enum Currency {
        INR("₹", "Indian Rupee", "en-IN"),
        USD("$", "US Dollar", "en-US"),
        EUR("€", "Euro", "de-DE"),
        GBP("£", "British Pound", "en-GB"),
        AED("د.إ", "UAE Dirham", "ar-AE"),
        SGD("S$", "Singapore Dollar", "en-SG"),
        AUD("A$", "Australian Dollar", "en-AU"),
        CAD("C$", "Canadian Dollar", "en-CA"),
        JPY("¥", "Japanese Yen", "ja-JP");

        private final String symbol;
        private final String name;
        private final String locale;

        Currency(String symbol, String name, String locale) {
            this.symbol = symbol;
            this.name = name;
            this.locale = locale;
        }

        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public String getLocale() { return locale; }
    }

    // Country enum for reference
    public enum Country {
        IN("India", "INR", 4),      // FY starts April
        US("United States", "USD", 1),
        UK("United Kingdom", "GBP", 4),
        AE("UAE", "AED", 1),
        SG("Singapore", "SGD", 4),
        AU("Australia", "AUD", 7),  // FY starts July
        CA("Canada", "CAD", 4),
        DE("Germany", "EUR", 1),
        JP("Japan", "JPY", 4);

        private final String name;
        private final String defaultCurrency;
        private final int fyStartMonth;

        Country(String name, String defaultCurrency, int fyStartMonth) {
            this.name = name;
            this.defaultCurrency = defaultCurrency;
            this.fyStartMonth = fyStartMonth;
        }

        public String getName() { return name; }
        public String getDefaultCurrency() { return defaultCurrency; }
        public int getFyStartMonth() { return fyStartMonth; }
    }
}
