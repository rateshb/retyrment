package com.retyrment.controller;

import com.retyrment.model.RetirementScenario;
import com.retyrment.model.UserStrategy;
import com.retyrment.model.User;
import com.retyrment.repository.RetirementScenarioRepository;
import com.retyrment.repository.UserStrategyRepository;
import com.retyrment.service.RetirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/retirement")
@RequiredArgsConstructor
public class RetirementController extends BaseController {

    private final RetirementScenarioRepository scenarioRepository;
    private final UserStrategyRepository userStrategyRepository;
    private final RetirementService retirementService;

    @GetMapping("/matrix")
    public Map<String, Object> getRetirementMatrix() {
        String userId = getCurrentUserId();
        return retirementService.generateRetirementMatrix(userId, null);
    }

    @PostMapping("/calculate")
    public Map<String, Object> calculateWithAssumptions(@RequestBody RetirementScenario scenario) {
        String userId = getCurrentUserId();
        return retirementService.generateRetirementMatrix(userId, scenario);
    }

    @GetMapping("/scenarios")
    public List<RetirementScenario> getAllScenarios() {
        return scenarioRepository.findAll();
    }

    @GetMapping("/scenarios/default")
    public ResponseEntity<RetirementScenario> getDefaultScenario() {
        return scenarioRepository.findByIsDefaultTrue()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/scenarios/{id}")
    public ResponseEntity<RetirementScenario> getScenarioById(@PathVariable String id) {
        return scenarioRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/scenarios")
    public RetirementScenario createScenario(@RequestBody RetirementScenario scenario) {
        // If this is set as default, unset other defaults
        if (Boolean.TRUE.equals(scenario.getIsDefault())) {
            scenarioRepository.findByIsDefaultTrue().ifPresent(existing -> {
                existing.setIsDefault(false);
                scenarioRepository.save(existing);
            });
        }
        return scenarioRepository.save(scenario);
    }

    @PutMapping("/scenarios/{id}")
    public ResponseEntity<RetirementScenario> updateScenario(@PathVariable String id, 
                                                              @RequestBody RetirementScenario scenario) {
        if (!scenarioRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        scenario.setId(id);
        return ResponseEntity.ok(scenarioRepository.save(scenario));
    }

    @DeleteMapping("/scenarios/{id}")
    public ResponseEntity<Void> deleteScenario(@PathVariable String id) {
        if (!scenarioRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        scenarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get investments and insurance policies maturing before retirement.
     * These become available for reinvestment.
     */
    @GetMapping("/maturing")
    public Map<String, Object> getMaturingBeforeRetirement(
            @RequestParam(defaultValue = "35") int currentAge,
            @RequestParam(defaultValue = "60") int retirementAge) {
        String userId = getCurrentUserId();
        return retirementService.calculateMaturingBeforeRetirement(userId, currentAge, retirementAge);
    }

    /**
     * Get withdrawal strategy recommendations.
     * Provides optimal order of fund withdrawal after retirement.
     */
    @GetMapping("/withdrawal-strategy")
    public Map<String, Object> getWithdrawalStrategy(
            @RequestParam(defaultValue = "35") int currentAge,
            @RequestParam(defaultValue = "60") int retirementAge,
            @RequestParam(defaultValue = "85") int lifeExpectancy) {
        String userId = getCurrentUserId();
        return retirementService.generateWithdrawalStrategy(userId, currentAge, retirementAge, lifeExpectancy);
    }

    // ==================== USER STRATEGY ENDPOINTS ====================

    /**
     * Get current user's saved strategy
     */
    @GetMapping("/strategy")
    public ResponseEntity<UserStrategy> getUserStrategy(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        return userStrategyRepository.findByUserId(user.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Save or update user's strategy
     */
    @PostMapping("/strategy")
    public ResponseEntity<UserStrategy> saveUserStrategy(
            @AuthenticationPrincipal User user,
            @RequestBody UserStrategy strategy) {
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        
        // Find existing or create new
        UserStrategy existing = userStrategyRepository.findByUserId(user.getId()).orElse(null);
        
        if (existing != null) {
            // Update existing
            strategy.setId(existing.getId());
            strategy.setCreatedAt(existing.getCreatedAt());
        } else {
            strategy.setCreatedAt(LocalDateTime.now());
        }
        
        strategy.setUserId(user.getId());
        strategy.setUpdatedAt(LocalDateTime.now());
        
        return ResponseEntity.ok(userStrategyRepository.save(strategy));
    }

    /**
     * Delete user's saved strategy
     */
    @DeleteMapping("/strategy")
    public ResponseEntity<Void> deleteUserStrategy(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        userStrategyRepository.findByUserId(user.getId())
                .ifPresent(s -> userStrategyRepository.deleteById(s.getId()));
        return ResponseEntity.noContent().build();
    }

}
