package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
import com.retyrment.model.Expense;
import com.retyrment.model.Expense.ExpenseCategory;
import com.retyrment.model.Expense.ExpenseFrequency;
import com.retyrment.model.User;
import com.retyrment.repository.ExpenseRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
        return expenseRepository.findByUserIdAndCategory(userId, category);
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
    
    // ===== NEW ENDPOINTS FOR TIME-BOUND EXPENSES =====
    
    @GetMapping("/time-bound")
    public List<Expense> getTimeBoundExpenses() {
        String userId = getCurrentUserId();
        return expenseRepository.findByUserIdAndIsTimeBoundTrue(userId);
    }
    
    @GetMapping("/recurring")
    public List<Expense> getRecurringExpenses() {
        String userId = getCurrentUserId();
        return expenseRepository.findByUserIdAndIsTimeBoundFalse(userId);
    }
    
    @GetMapping("/education")
    public List<Expense> getEducationExpenses() {
        String userId = getCurrentUserId();
        return expenseRepository.findEducationExpenses(userId);
    }
    
    @GetMapping("/dependent/{dependentName}")
    public List<Expense> getExpensesByDependent(@PathVariable String dependentName) {
        String userId = getCurrentUserId();
        return expenseRepository.findByUserIdAndDependentName(userId, dependentName);
    }
    
    @GetMapping("/ending-by/{year}")
    public List<Expense> getExpensesEndingByYear(@PathVariable Integer year) {
        String userId = getCurrentUserId();
        return expenseRepository.findExpensesEndingByYear(userId, year);
    }
    
    @GetMapping("/continuing-after/{year}")
    public List<Expense> getExpensesContinuingAfterYear(@PathVariable Integer year) {
        String userId = getCurrentUserId();
        return expenseRepository.findExpensesContinuingAfterYear(userId, year);
    }
    
    /**
     * Get a summary of expenses with monthly/yearly totals
     * Includes breakdown by time-bound vs recurring
     */
    @GetMapping("/summary")
    public Map<String, Object> getExpenseSummary() {
        String userId = getCurrentUserId();
        List<Expense> allExpenses = expenseRepository.findByUserId(userId);
        
        Map<String, Object> summary = new LinkedHashMap<>();
        
        // Calculate totals
        double totalMonthlyEquivalent = allExpenses.stream()
                .mapToDouble(Expense::getMonthlyEquivalent)
                .sum();
        
        double totalYearly = allExpenses.stream()
                .mapToDouble(Expense::getYearlyAmount)
                .sum();
        
        // Separate time-bound and recurring
        List<Expense> timeBound = allExpenses.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsTimeBound()))
                .toList();
        
        List<Expense> recurring = allExpenses.stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsTimeBound()))
                .toList();
        
        double timeBoundMonthly = timeBound.stream()
                .mapToDouble(Expense::getMonthlyEquivalent)
                .sum();
        
        double recurringMonthly = recurring.stream()
                .mapToDouble(Expense::getMonthlyEquivalent)
                .sum();
        
        summary.put("totalMonthlyEquivalent", Math.round(totalMonthlyEquivalent));
        summary.put("totalYearly", Math.round(totalYearly));
        summary.put("timeBoundMonthly", Math.round(timeBoundMonthly));
        summary.put("recurringMonthly", Math.round(recurringMonthly));
        summary.put("timeBoundCount", timeBound.size());
        summary.put("recurringCount", recurring.size());
        summary.put("totalCount", allExpenses.size());
        
        // Category breakdown
        Map<String, Double> categoryBreakdown = allExpenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().toString(),
                        Collectors.summingDouble(Expense::getMonthlyEquivalent)
                ));
        summary.put("categoryBreakdown", categoryBreakdown);
        
        // Dependents summary
        Map<String, Double> dependentBreakdown = allExpenses.stream()
                .filter(e -> e.getDependentName() != null)
                .collect(Collectors.groupingBy(
                        Expense::getDependentName,
                        Collectors.summingDouble(Expense::getMonthlyEquivalent)
                ));
        summary.put("dependentBreakdown", dependentBreakdown);
        
        return summary;
    }
    
    /**
     * Get investment opportunities from ending expenses
     * Shows how much money will be freed up when time-bound expenses end
     */
    @GetMapping("/investment-opportunities")
    public Map<String, Object> getInvestmentOpportunities(
            @RequestParam(defaultValue = "60") Integer retirementAge,
            @RequestParam(defaultValue = "35") Integer currentAge) {
        
        String userId = getCurrentUserId();
        List<Expense> timeBoundExpenses = expenseRepository.findByUserIdAndIsTimeBoundTrue(userId);
        
        Map<String, Object> opportunities = new LinkedHashMap<>();
        List<Map<String, Object>> freedUpByYear = new ArrayList<>();
        
        int currentYear = LocalDate.now().getYear();
        int retirementYear = currentYear + (retirementAge - currentAge);
        
        // Group expenses by end year
        Map<Integer, List<Expense>> expensesByEndYear = timeBoundExpenses.stream()
                .filter(e -> e.calculateEndYear() != null)
                .collect(Collectors.groupingBy(Expense::calculateEndYear));
        
        double cumulativeFreedUp = 0;
        
        for (int year = currentYear; year <= retirementYear; year++) {
            List<Expense> endingThisYear = expensesByEndYear.getOrDefault(year, Collections.emptyList());
            
            if (!endingThisYear.isEmpty()) {
                double monthlyFreedUp = endingThisYear.stream()
                        .mapToDouble(Expense::getMonthlyEquivalent)
                        .sum();
                
                cumulativeFreedUp += monthlyFreedUp;
                
                Map<String, Object> yearData = new LinkedHashMap<>();
                yearData.put("year", year);
                yearData.put("age", currentAge + (year - currentYear));
                yearData.put("monthlyFreedUp", Math.round(monthlyFreedUp));
                yearData.put("yearlyFreedUp", Math.round(monthlyFreedUp * 12));
                yearData.put("cumulativeMonthlyFreedUp", Math.round(cumulativeFreedUp));
                yearData.put("endingExpenses", endingThisYear.stream()
                        .map(e -> Map.of(
                                "name", e.getName(),
                                "category", e.getCategory().toString(),
                                "monthlyAmount", Math.round(e.getMonthlyEquivalent()),
                                "dependentName", e.getDependentName() != null ? e.getDependentName() : ""
                        ))
                        .toList());
                
                // Calculate potential investment growth if this money is invested
                int yearsToRetirement = retirementYear - year;
                double potentialCorpus = calculateSIPFutureValue(monthlyFreedUp, 12.0, yearsToRetirement);
                yearData.put("potentialCorpusAt12Percent", Math.round(potentialCorpus));
                
                freedUpByYear.add(yearData);
            }
        }
        
        // Calculate total potential corpus from all freed-up expenses
        double totalPotentialCorpus = freedUpByYear.stream()
                .mapToDouble(y -> ((Number) y.get("potentialCorpusAt12Percent")).doubleValue())
                .sum();
        
        opportunities.put("freedUpByYear", freedUpByYear);
        opportunities.put("totalMonthlyFreedUpByRetirement", Math.round(cumulativeFreedUp));
        opportunities.put("totalYearlyFreedUpByRetirement", Math.round(cumulativeFreedUp * 12));
        opportunities.put("totalPotentialCorpus", Math.round(totalPotentialCorpus));
        opportunities.put("retirementYear", retirementYear);
        opportunities.put("currentYear", currentYear);
        
        return opportunities;
    }
    
    /**
     * Get expense projection for retirement planning
     * Shows how expenses change over time as time-bound expenses end
     */
    @GetMapping("/projection")
    public Map<String, Object> getExpenseProjection(
            @RequestParam(defaultValue = "60") Integer retirementAge,
            @RequestParam(defaultValue = "35") Integer currentAge,
            @RequestParam(defaultValue = "6.0") Double inflationRate) {
        
        String userId = getCurrentUserId();
        List<Expense> allExpenses = expenseRepository.findByUserId(userId);
        
        Map<String, Object> projection = new LinkedHashMap<>();
        List<Map<String, Object>> yearlyProjection = new ArrayList<>();
        
        int currentYear = LocalDate.now().getYear();
        int retirementYear = currentYear + (retirementAge - currentAge);
        int lifeExpectancy = 85;
        int endYear = currentYear + (lifeExpectancy - currentAge);
        
        for (int year = currentYear; year <= endYear; year += 5) {
            int yearsFromNow = year - currentYear;
            int age = currentAge + yearsFromNow;
            
            // Calculate active expenses for this year
            double totalYearlyExpense = 0;
            List<String> activeExpenses = new ArrayList<>();
            List<String> endedExpenses = new ArrayList<>();
            
            for (Expense expense : allExpenses) {
                if (expense.isActiveInYear(year)) {
                    double inflatedAmount = expense.getInflatedYearlyAmount(yearsFromNow);
                    // Apply general inflation on top of expense-specific increase
                    if (expense.getAnnualIncreasePercent() == null) {
                        inflatedAmount = expense.getYearlyAmount() * Math.pow(1 + inflationRate / 100, yearsFromNow);
                    }
                    totalYearlyExpense += inflatedAmount;
                    activeExpenses.add(expense.getName());
                } else {
                    endedExpenses.add(expense.getName());
                }
            }
            
            Map<String, Object> yearData = new LinkedHashMap<>();
            yearData.put("year", year);
            yearData.put("age", age);
            yearData.put("isRetired", year >= retirementYear);
            yearData.put("yearlyExpense", Math.round(totalYearlyExpense));
            yearData.put("monthlyExpense", Math.round(totalYearlyExpense / 12));
            yearData.put("activeExpenseCount", activeExpenses.size());
            yearData.put("endedExpenseCount", endedExpenses.size());
            
            yearlyProjection.add(yearData);
        }
        
        // Calculate expenses at retirement
        double expenseAtRetirement = 0;
        for (Expense expense : allExpenses) {
            if (expense.isActiveInYear(retirementYear)) {
                int yearsToRetirement = retirementYear - currentYear;
                double inflatedAmount = expense.getInflatedYearlyAmount(yearsToRetirement);
                if (expense.getAnnualIncreasePercent() == null) {
                    inflatedAmount = expense.getYearlyAmount() * Math.pow(1 + inflationRate / 100, yearsToRetirement);
                }
                expenseAtRetirement += inflatedAmount;
            }
        }
        
        projection.put("yearlyProjection", yearlyProjection);
        projection.put("currentYearlyExpense", Math.round(allExpenses.stream()
                .mapToDouble(Expense::getYearlyAmount).sum()));
        projection.put("expenseAtRetirement", Math.round(expenseAtRetirement));
        projection.put("monthlyExpenseAtRetirement", Math.round(expenseAtRetirement / 12));
        projection.put("retirementYear", retirementYear);
        projection.put("inflationRate", inflationRate);
        
        return projection;
    }
    
    // Helper method for SIP future value calculation
    private double calculateSIPFutureValue(double monthlySIP, double annualRate, int years) {
        if (years <= 0 || monthlySIP <= 0) {
            return 0;
        }
        double monthlyRate = annualRate / 100 / 12;
        int months = years * 12;
        return monthlySIP * ((Math.pow(1 + monthlyRate, months) - 1) / monthlyRate) * (1 + monthlyRate);
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
        
        // Set defaults
        expense.setIsFixed(expense.getIsFixed() != null ? expense.getIsFixed() : true);
        expense.setIsTimeBound(expense.getIsTimeBound() != null ? expense.getIsTimeBound() : false);
        
        if (expense.getFrequency() == null) {
            expense.setFrequency(ExpenseFrequency.MONTHLY);
        }
        
        // Calculate monthly amount for backward compatibility
        expense.setMonthlyAmount(expense.getMonthlyEquivalent());
        
        // Calculate end year if time-bound
        if (Boolean.TRUE.equals(expense.getIsTimeBound())) {
            expense.setEndYear(expense.calculateEndYear());
        }
        
        return expenseRepository.save(expense);
    }

    @PutMapping("/{id}")
    public Expense updateExpense(@PathVariable String id, @Valid @RequestBody Expense expense) {
        String userId = getCurrentUserId();
        // Verify expense exists and belongs to user
        expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        
        expense.setId(id);
        expense.setUserId(userId);
        
        // Calculate monthly amount for backward compatibility
        expense.setMonthlyAmount(expense.getMonthlyEquivalent());
        
        // Calculate end year if time-bound
        if (Boolean.TRUE.equals(expense.getIsTimeBound())) {
            expense.setEndYear(expense.calculateEndYear());
        } else {
            expense.setEndYear(null);
        }
        
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
    
    /**
     * Get all unique dependent names for the user
     */
    @GetMapping("/dependents")
    public List<String> getDependents() {
        String userId = getCurrentUserId();
        return expenseRepository.findByUserId(userId).stream()
                .map(Expense::getDependentName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }
    
    /**
     * Get expense frequency options
     */
    @GetMapping("/options/frequencies")
    public List<Map<String, Object>> getFrequencyOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        for (ExpenseFrequency freq : ExpenseFrequency.values()) {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("value", freq.name());
            option.put("label", formatFrequencyLabel(freq));
            option.put("monthsInterval", freq.getMonthsInterval());
            options.add(option);
        }
        return options;
    }
    
    /**
     * Get expense category options
     */
    @GetMapping("/options/categories")
    public List<Map<String, Object>> getCategoryOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        for (ExpenseCategory cat : ExpenseCategory.values()) {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("value", cat.name());
            option.put("label", formatCategoryLabel(cat));
            option.put("isEducation", isEducationCategory(cat));
            option.put("isTimeBoundTypical", isTypicallyTimeBound(cat));
            options.add(option);
        }
        return options;
    }
    
    private String formatFrequencyLabel(ExpenseFrequency freq) {
        return switch (freq) {
            case MONTHLY -> "Monthly";
            case QUARTERLY -> "Quarterly (Every 3 months)";
            case HALF_YEARLY -> "Half-Yearly (Every 6 months)";
            case YEARLY -> "Yearly (Annual)";
            case ONE_TIME -> "One-Time";
        };
    }
    
    private String formatCategoryLabel(ExpenseCategory cat) {
        return switch (cat) {
            case RENT -> "Rent";
            case UTILITIES -> "Utilities (Electricity, Water, Gas)";
            case GROCERIES -> "Groceries";
            case TRANSPORT -> "Transport";
            case ENTERTAINMENT -> "Entertainment";
            case HEALTHCARE -> "Healthcare";
            case SHOPPING -> "Shopping";
            case DINING -> "Dining Out";
            case TRAVEL -> "Travel";
            case SUBSCRIPTIONS -> "Subscriptions (OTT, Gym, etc.)";
            case SCHOOL_FEE -> "School Fee";
            case COLLEGE_FEE -> "College Fee";
            case TUITION -> "Tuition/Coaching";
            case COACHING -> "Coaching Classes";
            case BOOKS_SUPPLIES -> "Books & Supplies";
            case HOSTEL -> "Hostel/Accommodation";
            case CHILDCARE -> "Childcare";
            case DAYCARE -> "Daycare";
            case ELDERLY_CARE -> "Elderly Care";
            case MAINTENANCE -> "Maintenance";
            case SOCIETY_CHARGES -> "Society Charges";
            case INSURANCE_PREMIUM -> "Insurance Premium";
            case OTHER -> "Other";
        };
    }
    
    private boolean isEducationCategory(ExpenseCategory cat) {
        return cat == ExpenseCategory.SCHOOL_FEE || 
               cat == ExpenseCategory.COLLEGE_FEE || 
               cat == ExpenseCategory.TUITION ||
               cat == ExpenseCategory.COACHING ||
               cat == ExpenseCategory.BOOKS_SUPPLIES ||
               cat == ExpenseCategory.HOSTEL;
    }
    
    private boolean isTypicallyTimeBound(ExpenseCategory cat) {
        return isEducationCategory(cat) ||
               cat == ExpenseCategory.CHILDCARE ||
               cat == ExpenseCategory.DAYCARE ||
               cat == ExpenseCategory.ELDERLY_CARE;
    }
}
