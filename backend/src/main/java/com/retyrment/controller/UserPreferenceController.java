package com.retyrment.controller;

import com.retyrment.model.UserPreference;
import com.retyrment.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/preferences")
@RequiredArgsConstructor
public class UserPreferenceController extends BaseController {

    private final UserPreferenceRepository preferenceRepository;

    protected String getCurrentUserId() {
        try {
            return super.getCurrentUserId();
        } catch (IllegalStateException e) {
            return "default";
        }
    }

    @GetMapping
    public ResponseEntity<UserPreference> getPreferences() {
        String userId = getCurrentUserId();
        
        UserPreference prefs = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
        
        return ResponseEntity.ok(prefs);
    }

    @PutMapping
    public ResponseEntity<UserPreference> updatePreferences(@RequestBody UserPreference preferences) {
        String userId = getCurrentUserId();
        
        UserPreference existing = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
        
        // Update fields
        if (preferences.getCurrency() != null) {
            existing.setCurrency(preferences.getCurrency());
            // Auto-set currency symbol
            try {
                UserPreference.Currency curr = UserPreference.Currency.valueOf(preferences.getCurrency());
                existing.setCurrencySymbol(curr.getSymbol());
                existing.setLocale(curr.getLocale());
            } catch (IllegalArgumentException e) {
                existing.setCurrencySymbol(preferences.getCurrencySymbol());
            }
        }
        if (preferences.getCountry() != null) {
            existing.setCountry(preferences.getCountry());
            // Auto-set financial year start month
            try {
                UserPreference.Country country = UserPreference.Country.valueOf(preferences.getCountry());
                existing.setFinancialYearStartMonth(country.getFyStartMonth());
            } catch (IllegalArgumentException e) {
                // Keep existing
            }
        }
        if (preferences.getDateFormat() != null) {
            existing.setDateFormat(preferences.getDateFormat());
        }
        if (preferences.getNumberFormat() != null) {
            existing.setNumberFormat(preferences.getNumberFormat());
        }
        if (preferences.getShowAmountInWords() != null) {
            existing.setShowAmountInWords(preferences.getShowAmountInWords());
        }
        if (preferences.getTheme() != null) {
            existing.setTheme(preferences.getTheme());
        }
        if (preferences.getFinancialYearStartMonth() != null) {
            existing.setFinancialYearStartMonth(preferences.getFinancialYearStartMonth());
        }
        if (preferences.getDefaultInflationRate() != null) {
            existing.setDefaultInflationRate(preferences.getDefaultInflationRate());
        }
        if (preferences.getDefaultEquityReturn() != null) {
            existing.setDefaultEquityReturn(preferences.getDefaultEquityReturn());
        }
        if (preferences.getDefaultDebtReturn() != null) {
            existing.setDefaultDebtReturn(preferences.getDefaultDebtReturn());
        }
        if (preferences.getDefaultRetirementAge() != null) {
            existing.setDefaultRetirementAge(preferences.getDefaultRetirementAge());
        }
        if (preferences.getEmailNotifications() != null) {
            existing.setEmailNotifications(preferences.getEmailNotifications());
        }
        if (preferences.getPaymentReminders() != null) {
            existing.setPaymentReminders(preferences.getPaymentReminders());
        }
        if (preferences.getReminderDaysBefore() != null) {
            existing.setReminderDaysBefore(preferences.getReminderDaysBefore());
        }
        
        UserPreference saved = preferenceRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/options")
    public ResponseEntity<Map<String, Object>> getOptions() {
        Map<String, Object> options = new LinkedHashMap<>();
        
        // Currencies
        List<Map<String, String>> currencies = new ArrayList<>();
        for (UserPreference.Currency c : UserPreference.Currency.values()) {
            Map<String, String> currency = new LinkedHashMap<>();
            currency.put("code", c.name());
            currency.put("symbol", c.getSymbol());
            currency.put("name", c.getName());
            currencies.add(currency);
        }
        options.put("currencies", currencies);
        
        // Countries
        List<Map<String, Object>> countries = new ArrayList<>();
        for (UserPreference.Country c : UserPreference.Country.values()) {
            Map<String, Object> country = new LinkedHashMap<>();
            country.put("code", c.name());
            country.put("name", c.getName());
            country.put("defaultCurrency", c.getDefaultCurrency());
            country.put("fyStartMonth", c.getFyStartMonth());
            countries.add(country);
        }
        options.put("countries", countries);
        
        // Date formats
        options.put("dateFormats", Arrays.asList(
                Map.of("value", "DD/MM/YYYY", "label", "DD/MM/YYYY (31/12/2024)"),
                Map.of("value", "MM/DD/YYYY", "label", "MM/DD/YYYY (12/31/2024)"),
                Map.of("value", "YYYY-MM-DD", "label", "YYYY-MM-DD (2024-12-31)")
        ));
        
        // Number formats
        options.put("numberFormats", Arrays.asList(
                Map.of("value", "indian", "label", "Indian (12,34,567.00)"),
                Map.of("value", "western", "label", "Western (1,234,567.00)")
        ));
        
        // Themes
        options.put("themes", Arrays.asList(
                Map.of("value", "light", "label", "Light"),
                Map.of("value", "dark", "label", "Dark"),
                Map.of("value", "system", "label", "System Default")
        ));
        
        return ResponseEntity.ok(options);
    }

    private UserPreference createDefaultPreferences(String userId) {
        UserPreference defaults = UserPreference.builder()
                .userId(userId)
                .currency("INR")
                .currencySymbol("₹")
                .country("IN")
                .locale("en-IN")
                .dateFormat("DD/MM/YYYY")
                .numberFormat("indian")
                .showAmountInWords(true)
                .theme("light")
                .financialYearStartMonth(4)
                .defaultInflationRate(6.0)
                .defaultEquityReturn(12.0)
                .defaultDebtReturn(7.0)
                .defaultRetirementAge(60)
                .emailNotifications(true)
                .paymentReminders(true)
                .reminderDaysBefore(3)
                .build();
        
        return preferenceRepository.save(defaults);
    }
}
