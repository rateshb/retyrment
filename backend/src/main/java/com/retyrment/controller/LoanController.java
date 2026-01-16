package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
import com.retyrment.model.Loan;
import com.retyrment.model.Loan.LoanType;
import com.retyrment.model.User;
import com.retyrment.repository.LoanRepository;
import com.retyrment.service.CalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanRepository loanRepository;
    private final CalculationService calculationService;

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new IllegalStateException("User not authenticated");
    }

    @GetMapping
    public List<Loan> getAllLoans() {
        String userId = getCurrentUserId();
        return loanRepository.findByUserId(userId);
    }

    @GetMapping("/type/{type}")
    public List<Loan> getLoansByType(@PathVariable LoanType type) {
        String userId = getCurrentUserId();
        return loanRepository.findByUserId(userId).stream()
                .filter(l -> l.getType() == type)
                .toList();
    }

    @GetMapping("/active")
    public List<Loan> getActiveLoans() {
        String userId = getCurrentUserId();
        return loanRepository.findByUserIdAndRemainingMonthsGreaterThan(userId, 0);
    }

    @GetMapping("/{id}")
    public Loan getLoanById(@PathVariable String id) {
        String userId = getCurrentUserId();
        return loanRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
    }

    @GetMapping("/{id}/amortization")
    public List<Map<String, Object>> getAmortizationSchedule(@PathVariable String id) {
        String userId = getCurrentUserId();
        Loan loan = loanRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
        return calculationService.calculateAmortization(loan);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Loan createLoan(@Valid @RequestBody Loan loan) {
        String userId = getCurrentUserId();
        loan.setUserId(userId);
        return loanRepository.save(loan);
    }

    @PutMapping("/{id}")
    public Loan updateLoan(@PathVariable String id, @Valid @RequestBody Loan loan) {
        String userId = getCurrentUserId();
        Loan existing = loanRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
        loan.setId(id);
        loan.setUserId(userId); // Ensure userId cannot be changed
        return loanRepository.save(loan);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLoan(@PathVariable String id) {
        String userId = getCurrentUserId();
        if (!loanRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Loan", id);
        }
        loanRepository.deleteById(id);
    }
}
