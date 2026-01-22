package com.retyrment.controller;

import com.retyrment.model.User;
import com.retyrment.model.UserSettings;
import com.retyrment.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class UserSettingsController {
    
    private final UserSettingsRepository userSettingsRepository;
    
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new IllegalStateException("User not authenticated");
    }
    
    /**
     * Get user settings (returns defaults if not set)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserSettings() {
        String userId = getCurrentUserId();
        
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElse(getDefaultSettings(userId));
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("currentAge", settings.getCurrentAge() != null ? settings.getCurrentAge() : 35);
        response.put("retirementAge", settings.getRetirementAge() != null ? settings.getRetirementAge() : 60);
        response.put("lifeExpectancy", settings.getLifeExpectancy() != null ? settings.getLifeExpectancy() : 85);
        response.put("inflationRate", settings.getInflationRate() != null ? settings.getInflationRate() : 6.0);
        response.put("epfReturn", settings.getEpfReturn() != null ? settings.getEpfReturn() : 10.0);
        response.put("ppfReturn", settings.getPpfReturn() != null ? settings.getPpfReturn() : 7.1);
        response.put("mfEquityReturn", settings.getMfEquityReturn() != null ? settings.getMfEquityReturn() : 12.0);
        response.put("mfDebtReturn", settings.getMfDebtReturn() != null ? settings.getMfDebtReturn() : 7.0);
        response.put("fdReturn", settings.getFdReturn() != null ? settings.getFdReturn() : 6.5);
        response.put("emergencyFundMonths", settings.getEmergencyFundMonths() != null ? settings.getEmergencyFundMonths() : 6);
        response.put("sipStepup", settings.getSipStepup() != null ? settings.getSipStepup() : 10.0);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update user settings
     */
    @PutMapping
    public ResponseEntity<Map<String, Object>> updateUserSettings(@RequestBody Map<String, Object> settingsData) {
        String userId = getCurrentUserId();
        
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElse(UserSettings.builder()
                        .userId(userId)
                        .createdAt(LocalDateTime.now())
                        .build());
        
        // Update fields from request
        if (settingsData.containsKey("currentAge")) {
            settings.setCurrentAge(((Number) settingsData.get("currentAge")).intValue());
        }
        if (settingsData.containsKey("retirementAge")) {
            settings.setRetirementAge(((Number) settingsData.get("retirementAge")).intValue());
        }
        if (settingsData.containsKey("lifeExpectancy")) {
            settings.setLifeExpectancy(((Number) settingsData.get("lifeExpectancy")).intValue());
        }
        if (settingsData.containsKey("inflationRate")) {
            settings.setInflationRate(((Number) settingsData.get("inflationRate")).doubleValue());
        }
        if (settingsData.containsKey("epfReturn")) {
            settings.setEpfReturn(((Number) settingsData.get("epfReturn")).doubleValue());
        }
        if (settingsData.containsKey("ppfReturn")) {
            settings.setPpfReturn(((Number) settingsData.get("ppfReturn")).doubleValue());
        }
        if (settingsData.containsKey("mfEquityReturn")) {
            settings.setMfEquityReturn(((Number) settingsData.get("mfEquityReturn")).doubleValue());
        }
        if (settingsData.containsKey("mfDebtReturn")) {
            settings.setMfDebtReturn(((Number) settingsData.get("mfDebtReturn")).doubleValue());
        }
        if (settingsData.containsKey("fdReturn")) {
            settings.setFdReturn(((Number) settingsData.get("fdReturn")).doubleValue());
        }
        if (settingsData.containsKey("emergencyFundMonths")) {
            settings.setEmergencyFundMonths(((Number) settingsData.get("emergencyFundMonths")).intValue());
        }
        if (settingsData.containsKey("sipStepup")) {
            settings.setSipStepup(((Number) settingsData.get("sipStepup")).doubleValue());
        }
        
        settings.setUpdatedAt(LocalDateTime.now());
        
        UserSettings saved = userSettingsRepository.save(settings);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Settings updated successfully");
        response.put("settings", saved);
        
        return ResponseEntity.ok(response);
    }
    
    private UserSettings getDefaultSettings(String userId) {
        return UserSettings.builder()
                .userId(userId)
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .inflationRate(6.0)
                .epfReturn(10.0)
                .ppfReturn(7.1)
                .mfEquityReturn(12.0)
                .mfDebtReturn(7.0)
                .fdReturn(6.5)
                .emergencyFundMonths(6)
                .sipStepup(10.0)
                .build();
    }
}
