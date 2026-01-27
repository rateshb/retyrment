package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
import com.retyrment.model.Income;
import com.retyrment.repository.IncomeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/income")
@RequiredArgsConstructor
public class IncomeController extends BaseController {

    private final IncomeRepository incomeRepository;

    @GetMapping
    public List<Income> getAllIncome() {
        String userId = getCurrentUserId();
        return incomeRepository.findByUserId(userId);
    }

    @GetMapping("/active")
    public List<Income> getActiveIncome() {
        String userId = getCurrentUserId();
        return incomeRepository.findByUserIdAndIsActiveTrue(userId);
    }

    @GetMapping("/{id}")
    public Income getIncomeById(@PathVariable String id) {
        String userId = getCurrentUserId();
        return incomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Income", id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Income createIncome(@Valid @RequestBody Income income) {
        String userId = getCurrentUserId();
        income.setUserId(userId);
        income.setIsActive(income.getIsActive() != null ? income.getIsActive() : true);
        return incomeRepository.save(income);
    }

    @PutMapping("/{id}")
    public Income updateIncome(@PathVariable String id, @Valid @RequestBody Income income) {
        String userId = getCurrentUserId();
        Income existing = incomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Income", id));
        income.setId(id);
        income.setUserId(userId); // Ensure userId cannot be changed
        return incomeRepository.save(income);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIncome(@PathVariable String id) {
        String userId = getCurrentUserId();
        if (!incomeRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Income", id);
        }
        incomeRepository.deleteById(id);
    }
}
