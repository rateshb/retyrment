package com.retyrment.controller;

import com.retyrment.model.Insurance;
import com.retyrment.model.Insurance.InsuranceType;
import com.retyrment.repository.InsuranceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/insurance")
@RequiredArgsConstructor
public class InsuranceController extends BaseController {

    private final InsuranceRepository insuranceRepository;

    @GetMapping
    public List<Insurance> getAllInsurance() {
        String userId = getCurrentUserId();
        return insuranceRepository.findByUserId(userId);
    }

    @GetMapping("/type/{type}")
    public List<Insurance> getInsuranceByType(@PathVariable InsuranceType type) {
        String userId = getCurrentUserId();
        return insuranceRepository.findByUserIdAndType(userId, type);
    }

    @GetMapping("/investment-linked")
    public List<Insurance> getInvestmentLinkedPolicies() {
        String userId = getCurrentUserId();
        return insuranceRepository.findByUserIdAndTypeIn(userId, Arrays.asList(
                InsuranceType.ULIP, InsuranceType.ENDOWMENT, InsuranceType.MONEY_BACK));
    }

    @GetMapping("/renewal-month/{month}")
    public List<Insurance> getByRenewalMonth(@PathVariable Integer month) {
        String userId = getCurrentUserId();
        return insuranceRepository.findByUserId(userId).stream()
                .filter(i -> i.getRenewalMonth() != null && i.getRenewalMonth().equals(month))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Insurance> getInsuranceById(@PathVariable String id) {
        String userId = getCurrentUserId();
        return insuranceRepository.findByIdAndUserId(id, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Insurance createInsurance(@RequestBody Insurance insurance) {
        String userId = getCurrentUserId();
        insurance.setUserId(userId);
        return insuranceRepository.save(insurance);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Insurance> updateInsurance(@PathVariable String id, 
                                                      @RequestBody Insurance insurance) {
        String userId = getCurrentUserId();
        Insurance existing = insuranceRepository.findByIdAndUserId(id, userId)
                .orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        insurance.setId(id);
        insurance.setUserId(userId); // Ensure userId cannot be changed
        return ResponseEntity.ok(insuranceRepository.save(insurance));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInsurance(@PathVariable String id) {
        String userId = getCurrentUserId();
        if (!insuranceRepository.existsByIdAndUserId(id, userId)) {
            return ResponseEntity.notFound().build();
        }
        insuranceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
