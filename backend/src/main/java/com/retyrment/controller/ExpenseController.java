package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
import com.retyrment.model.Expense;
import com.retyrment.model.Expense.ExpenseCategory;
import com.retyrment.model.User;
import com.retyrment.repository.ExpenseRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseRepository expenseRepository;

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new IllegalStateException("User not authenticated");
    }

    @GetMapping
    public List<Expense> getAllExpenses() {
        String userId = getCurrentUserId();
        return expenseRepository.findByUserId(userId);
    }

    @GetMapping("/category/{category}")
    public List<Expense> getExpensesByCategory(@PathVariable ExpenseCategory category) {
        String userId = getCurrentUserId();
        return expenseRepository.findByUserId(userId).stream()
                .filter(e -> e.getCategory() == category)
                .toList();
    }

    @GetMapping("/fixed")
    public List<Expense> getFixedExpenses() {
        String userId = getCurrentUserId();
        return expenseRepository.findByUserIdAndIsFixedTrue(userId);
    }

    @GetMapping("/variable")
    public List<Expense> getVariableExpenses() {
        String userId = getCurrentUserId();
        return expenseRepository.findByUserIdAndIsFixedFalse(userId);
    }

    @GetMapping("/{id}")
    public Expense getExpenseById(@PathVariable String id) {
        String userId = getCurrentUserId();
        return expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Expense createExpense(@Valid @RequestBody Expense expense) {
        String userId = getCurrentUserId();
        expense.setUserId(userId);
        expense.setIsFixed(expense.getIsFixed() != null ? expense.getIsFixed() : true);
        return expenseRepository.save(expense);
    }

    @PutMapping("/{id}")
    public Expense updateExpense(@PathVariable String id, @Valid @RequestBody Expense expense) {
        String userId = getCurrentUserId();
        Expense existing = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        expense.setId(id);
        expense.setUserId(userId); // Ensure userId cannot be changed
        return expenseRepository.save(expense);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable String id) {
        String userId = getCurrentUserId();
        if (!expenseRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Expense", id);
        }
        expenseRepository.deleteById(id);
    }
}
