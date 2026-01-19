package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
import com.retyrment.model.Expense;
import com.retyrment.model.Expense.ExpenseCategory;
import com.retyrment.model.Expense.ExpenseFrequency;
import com.retyrment.model.User;
import com.retyrment.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseController Tests")
class ExpenseControllerTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private ExpenseController expenseController;

    private static final String TEST_USER_ID = "user-123";
    private User testUser;
    private Expense testExpense;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(TEST_USER_ID);
        testUser.setEmail("test@example.com");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getPrincipal()).thenReturn(testUser);

        testExpense = createExpense("Rent", ExpenseCategory.RENT, 20000.0, true, false);
    }

    @Test
    @DisplayName("Should get all expenses")
    void testGetAllExpenses() {
        // Arrange
        Expense expense2 = createExpense("Groceries", ExpenseCategory.GROCERIES, 8000.0, false, false);
        when(expenseRepository.findByUserId(TEST_USER_ID)).thenReturn(Arrays.asList(testExpense, expense2));

        // Act
        List<Expense> result = expenseController.getAllExpenses();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(expenseRepository).findByUserId(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should get expenses by category")
    void testGetExpensesByCategory() {
        // Arrange
        when(expenseRepository.findByUserIdAndCategory(TEST_USER_ID, ExpenseCategory.RENT))
                .thenReturn(List.of(testExpense));

        // Act
        List<Expense> result = expenseController.getExpensesByCategory(ExpenseCategory.RENT);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ExpenseCategory.RENT, result.get(0).getCategory());
        verify(expenseRepository).findByUserIdAndCategory(TEST_USER_ID, ExpenseCategory.RENT);
    }

    @Test
    @DisplayName("Should get fixed expenses")
    void testGetFixedExpenses() {
        // Arrange
        when(expenseRepository.findByUserIdAndIsFixedTrue(TEST_USER_ID))
                .thenReturn(List.of(testExpense));

        // Act
        List<Expense> result = expenseController.getFixedExpenses();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsFixed());
        verify(expenseRepository).findByUserIdAndIsFixedTrue(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should get variable expenses")
    void testGetVariableExpenses() {
        // Arrange
        Expense variableExpense = createExpense("Entertainment", ExpenseCategory.ENTERTAINMENT, 5000.0, false, false);
        when(expenseRepository.findByUserIdAndIsFixedFalse(TEST_USER_ID))
                .thenReturn(List.of(variableExpense));

        // Act
        List<Expense> result = expenseController.getVariableExpenses();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsFixed());
    }

    @Test
    @DisplayName("Should get time-bound expenses")
    void testGetTimeBoundExpenses() {
        // Arrange
        Expense timeBoundExpense = createExpense("School Fee", ExpenseCategory.SCHOOL_FEE, 30000.0, true, true);
        timeBoundExpense.setEndAge(18);
        when(expenseRepository.findByUserIdAndIsTimeBoundTrue(TEST_USER_ID))
                .thenReturn(List.of(timeBoundExpense));

        // Act
        List<Expense> result = expenseController.getTimeBoundExpenses();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsTimeBound());
    }

    @Test
    @DisplayName("Should get recurring expenses")
    void testGetRecurringExpenses() {
        // Arrange
        when(expenseRepository.findByUserIdAndIsTimeBoundFalse(TEST_USER_ID))
                .thenReturn(List.of(testExpense));

        // Act
        List<Expense> result = expenseController.getRecurringExpenses();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsTimeBound());
    }

    @Test
    @DisplayName("Should get education expenses")
    void testGetEducationExpenses() {
        // Arrange
        Expense schoolFee = createExpense("School", ExpenseCategory.SCHOOL_FEE, 50000.0, true, true);
        when(expenseRepository.findEducationExpenses(TEST_USER_ID))
                .thenReturn(List.of(schoolFee));

        // Act
        List<Expense> result = expenseController.getEducationExpenses();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ExpenseCategory.SCHOOL_FEE, result.get(0).getCategory());
    }

    @Test
    @DisplayName("Should get expenses by dependent")
    void testGetExpensesByDependent() {
        // Arrange
        Expense childExpense = createExpense("School Fee", ExpenseCategory.SCHOOL_FEE, 30000.0, true, true);
        childExpense.setDependentName("John");
        when(expenseRepository.findByUserIdAndDependentName(TEST_USER_ID, "John"))
                .thenReturn(List.of(childExpense));

        // Act
        List<Expense> result = expenseController.getExpensesByDependent("John");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getDependentName());
    }

    @Test
    @DisplayName("Should get expenses ending by year")
    void testGetExpensesEndingByYear() {
        // Arrange
        when(expenseRepository.findExpensesEndingByYear(TEST_USER_ID, 2030))
                .thenReturn(List.of(testExpense));

        // Act
        List<Expense> result = expenseController.getExpensesEndingByYear(2030);

        // Assert
        assertNotNull(result);
        verify(expenseRepository).findExpensesEndingByYear(TEST_USER_ID, 2030);
    }

    @Test
    @DisplayName("Should get expenses continuing after year")
    void testGetExpensesContinuingAfterYear() {
        // Arrange
        when(expenseRepository.findExpensesContinuingAfterYear(TEST_USER_ID, 2030))
                .thenReturn(List.of(testExpense));

        // Act
        List<Expense> result = expenseController.getExpensesContinuingAfterYear(2030);

        // Assert
        assertNotNull(result);
        verify(expenseRepository).findExpensesContinuingAfterYear(TEST_USER_ID, 2030);
    }

    @Test
    @DisplayName("Should get expense summary with all breakdowns")
    void testGetExpenseSummary() {
        // Arrange
        Expense expense1 = createExpense("Rent", ExpenseCategory.RENT, 20000.0, true, false);
        Expense expense2 = createExpense("School", ExpenseCategory.SCHOOL_FEE, 30000.0, true, true);
        expense2.setDependentName("Child1");
        expense2.setEndAge(18);

        when(expenseRepository.findByUserId(TEST_USER_ID)).thenReturn(Arrays.asList(expense1, expense2));

        // Act
        Map<String, Object> result = expenseController.getExpenseSummary();

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("totalMonthlyEquivalent"));
        assertTrue(result.containsKey("totalYearly"));
        assertTrue(result.containsKey("timeBoundMonthly"));
        assertTrue(result.containsKey("recurringMonthly"));
        assertTrue(result.containsKey("timeBoundCount"));
        assertTrue(result.containsKey("recurringCount"));
        assertTrue(result.containsKey("categoryBreakdown"));
        assertTrue(result.containsKey("dependentBreakdown"));
        
        assertEquals(2, result.get("totalCount"));
        assertEquals(1, result.get("timeBoundCount"));
        assertEquals(1, result.get("recurringCount"));
    }

    @Test
    @DisplayName("Should get investment opportunities from ending expenses")
    void testGetInvestmentOpportunities() {
        // Arrange
        Expense timeBoundExpense = createExpense("School", ExpenseCategory.SCHOOL_FEE, 30000.0, true, true);
        timeBoundExpense.setEndAge(18);
        timeBoundExpense.setDependentCurrentAge(10);
        timeBoundExpense.setDependentDob(LocalDate.now());

        when(expenseRepository.findByUserIdAndIsTimeBoundTrue(TEST_USER_ID))
                .thenReturn(List.of(timeBoundExpense));

        // Act
        Map<String, Object> result = expenseController.getInvestmentOpportunities(60, 35);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("freedUpByYear"));
        assertTrue(result.containsKey("totalMonthlyFreedUpByRetirement"));
        assertTrue(result.containsKey("totalPotentialCorpus"));
        assertTrue(result.containsKey("retirementYear"));
    }

    @Test
    @DisplayName("Should get expense projection with inflation")
    void testGetExpenseProjection() {
        // Arrange
        Expense expense1 = createExpense("Rent", ExpenseCategory.RENT, 20000.0, true, false);
        Expense expense2 = createExpense("School", ExpenseCategory.SCHOOL_FEE, 30000.0, true, true);
        expense2.setEndAge(18);
        expense2.setDependentCurrentAge(10);

        when(expenseRepository.findByUserId(TEST_USER_ID)).thenReturn(Arrays.asList(expense1, expense2));

        // Act
        Map<String, Object> result = expenseController.getExpenseProjection(60, 35, 6.0);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("yearlyProjection"));
        assertTrue(result.containsKey("currentYearlyExpense"));
        assertTrue(result.containsKey("expenseAtRetirement"));
        assertTrue(result.containsKey("inflationRate"));
    }

    @Test
    @DisplayName("Should get expense by ID")
    void testGetExpense_Success() {
        // Arrange
        when(expenseRepository.findByIdAndUserId("expense-1", TEST_USER_ID))
                .thenReturn(Optional.of(testExpense));

        // Act
        Expense result = expenseController.getExpenseById("expense-1");

        // Assert
        assertNotNull(result);
        assertEquals(testExpense.getName(), result.getName());
    }

    @Test
    @DisplayName("Should throw exception when expense not found")
    void testGetExpense_NotFound() {
        // Arrange
        when(expenseRepository.findByIdAndUserId("expense-999", TEST_USER_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
            expenseController.getExpenseById("expense-999")
        );
    }

    @Test
    @DisplayName("Should create expense with defaults")
    void testCreateExpense() {
        // Arrange
        Expense newExpense = new Expense();
        newExpense.setName("Test");
        newExpense.setCategory(ExpenseCategory.OTHER);
        newExpense.setAmount(5000.0);

        when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Expense result = expenseController.createExpense(newExpense);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        assertTrue(result.getIsFixed()); // Default
        assertFalse(result.getIsTimeBound()); // Default
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("Should set default frequency if not provided")
    void testCreateExpense_DefaultFrequency() {
        // Arrange
        Expense newExpense = new Expense();
        newExpense.setName("Test");
        newExpense.setCategory(ExpenseCategory.OTHER);
        newExpense.setAmount(5000.0);

        when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Expense result = expenseController.createExpense(newExpense);

        // Assert
        assertEquals(ExpenseFrequency.MONTHLY, result.getFrequency());
    }

    @Test
    @DisplayName("Should calculate monthly amount when creating yearly expense")
    void testCreateExpense_YearlyFrequency() {
        // Arrange
        Expense newExpense = new Expense();
        newExpense.setName("Annual Insurance");
        newExpense.setCategory(ExpenseCategory.INSURANCE_PREMIUM);
        newExpense.setAmount(12000.0);
        newExpense.setFrequency(ExpenseFrequency.YEARLY);

        when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Expense result = expenseController.createExpense(newExpense);

        // Assert
        assertEquals(1000.0, result.getMonthlyAmount(), 0.01); // 12000 / 12
    }

    @Test
    @DisplayName("Should update expense")
    void testUpdateExpense_Success() {
        // Arrange
        when(expenseRepository.findByIdAndUserId("expense-1", TEST_USER_ID))
                .thenReturn(Optional.of(testExpense));

        Expense updatedData = new Expense();
        updatedData.setName("Updated Rent");
        updatedData.setAmount(25000.0);
        updatedData.setCategory(ExpenseCategory.RENT);
        updatedData.setFrequency(ExpenseFrequency.MONTHLY);

        when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Expense result = expenseController.updateExpense("expense-1", updatedData);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent expense")
    void testUpdateExpense_NotFound() {
        // Arrange
        when(expenseRepository.findByIdAndUserId("expense-999", TEST_USER_ID))
                .thenReturn(Optional.empty());

        Expense updatedData = new Expense();

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
            expenseController.updateExpense("expense-999", updatedData)
        );
    }

    @Test
    @DisplayName("Should delete expense")
    void testDeleteExpense_Success() {
        // Arrange
        when(expenseRepository.existsByIdAndUserId("expense-1", TEST_USER_ID)).thenReturn(true);

        // Act
        expenseController.deleteExpense("expense-1");

        // Assert
        verify(expenseRepository).existsByIdAndUserId("expense-1", TEST_USER_ID);
        verify(expenseRepository).deleteById("expense-1");
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent expense")
    void testDeleteExpense_NotFound() {
        // Arrange
        when(expenseRepository.existsByIdAndUserId("expense-999", TEST_USER_ID)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
            expenseController.deleteExpense("expense-999")
        );
        verify(expenseRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("Should handle empty expenses list")
    void testGetExpenseSummary_EmptyList() {
        // Arrange
        when(expenseRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of());

        // Act
        Map<String, Object> result = expenseController.getExpenseSummary();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.get("totalCount"));
        assertEquals(0L, result.get("totalMonthlyEquivalent"));
    }

    @Test
    @DisplayName("Should handle investment opportunities with no time-bound expenses")
    void testGetInvestmentOpportunities_NoTimeBoundExpenses() {
        // Arrange
        when(expenseRepository.findByUserIdAndIsTimeBoundTrue(TEST_USER_ID))
                .thenReturn(List.of());

        // Act
        Map<String, Object> result = expenseController.getInvestmentOpportunities(60, 35);

        // Assert
        assertNotNull(result);
        List<?> freedUp = (List<?>) result.get("freedUpByYear");
        assertTrue(freedUp.isEmpty());
        assertEquals(0L, result.get("totalMonthlyFreedUpByRetirement"));
    }

    @Test
    @DisplayName("Should handle different age parameters")
    void testGetInvestmentOpportunities_DifferentAges() {
        // Arrange
        when(expenseRepository.findByUserIdAndIsTimeBoundTrue(TEST_USER_ID))
                .thenReturn(List.of());

        // Act
        Map<String, Object> result1 = expenseController.getInvestmentOpportunities(65, 40);
        Map<String, Object> result2 = expenseController.getInvestmentOpportunities(55, 50);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotEquals(result1.get("retirementYear"), result2.get("retirementYear"));
    }

    @Test
    @DisplayName("Should handle different inflation rates")
    void testGetExpenseProjection_DifferentInflationRates() {
        // Arrange
        Expense expense = createExpense("Rent", ExpenseCategory.RENT, 20000.0, true, false);
        when(expenseRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of(expense));

        // Act
        Map<String, Object> result1 = expenseController.getExpenseProjection(60, 35, 5.0);
        Map<String, Object> result2 = expenseController.getExpenseProjection(60, 35, 7.0);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(5.0, result1.get("inflationRate"));
        assertEquals(7.0, result2.get("inflationRate"));
    }

    // Helper method
    private Expense createExpense(String name, ExpenseCategory category, Double amount, 
                                  Boolean isFixed, Boolean isTimeBound) {
        Expense expense = new Expense();
        expense.setId("expense-" + System.nanoTime());
        expense.setUserId(TEST_USER_ID);
        expense.setName(name);
        expense.setCategory(category);
        expense.setAmount(amount);
        expense.setFrequency(ExpenseFrequency.MONTHLY);
        expense.setMonthlyAmount(amount);
        expense.setIsFixed(isFixed);
        expense.setIsTimeBound(isTimeBound);
        return expense;
    }
}
