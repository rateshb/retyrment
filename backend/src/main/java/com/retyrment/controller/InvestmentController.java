package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
import com.retyrment.model.Investment;
import com.retyrment.model.Investment.InvestmentType;
import com.retyrment.model.User;
import com.retyrment.repository.InvestmentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/investments")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentRepository investmentRepository;

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new IllegalStateException("User not authenticated");
    }

    @GetMapping
    public List<Investment> getAllInvestments() {
        String userId = getCurrentUserId();
        return investmentRepository.findByUserId(userId);
    }

    @GetMapping("/type/{type}")
    public List<Investment> getInvestmentsByType(@PathVariable InvestmentType type) {
        String userId = getCurrentUserId();
        return investmentRepository.findByUserIdAndType(userId, type);
    }

    @GetMapping("/sips")
    public List<Investment> getInvestmentsWithSIP() {
        String userId = getCurrentUserId();
        return investmentRepository.findByUserIdAndMonthlySipGreaterThan(userId, 0.0);
    }

    @GetMapping("/{id}")
    public Investment getInvestmentById(@PathVariable String id) {
        String userId = getCurrentUserId();
        return investmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment", id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Investment createInvestment(@Valid @RequestBody Investment investment) {
        String userId = getCurrentUserId();
        investment.setUserId(userId);
        return investmentRepository.save(investment);
    }

    @PutMapping("/{id}")
    public Investment updateInvestment(@PathVariable String id, 
                                       @Valid @RequestBody Investment investment) {
        String userId = getCurrentUserId();
        Investment existing = investmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment", id));
        investment.setId(id);
        investment.setUserId(userId); // Ensure userId cannot be changed
        return investmentRepository.save(investment);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvestment(@PathVariable String id) {
        String userId = getCurrentUserId();
        if (!investmentRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Investment", id);
        }
        investmentRepository.deleteById(id);
    }
}
