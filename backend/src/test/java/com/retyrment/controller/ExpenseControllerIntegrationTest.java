package com.retyrment.controller;

import com.retyrment.model.Expense;
import com.retyrment.model.Expense.ExpenseCategory;
import com.retyrment.model.User;
import com.retyrment.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExpenseController Branch Coverage Tests")
class ExpenseControllerIntegrationTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ExpenseController expenseController;

    private User testUser1;
    private Expense user1Expense1;
    private Expense user1Expense2;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        
        testUser1 = User.builder()
                .id("user-1")
                .email("user1@test.com")
                .name("User One")
                .role(User.UserRole.FREE)
                .build();

        user1Expense1 = Expense.builder()
                .id("exp-1")
                .userId("user-1")
                .name("Groceries")
                .category(ExpenseCategory.GROCERIES)
                .monthlyAmount(5000.0)
                .isFixed(true)
                .build();

        user1Expense2 = Expense.builder()
                .id("exp-2")
                .userId("user-1")
                .name("Rent")
                .category(ExpenseCategory.RENT)
                .monthlyAmount(15000.0)
                .isFixed(true)
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser1);
    }

    @Nested
    @DisplayName("GET /expenses - Branch Coverage")
    class GetAllExpenses {

        @Test
        @DisplayName("should return only current user's expenses")
        void shouldReturnOnlyCurrentUserExpenses() {
            when(expenseRepository.findByUserId("user-1"))
                    .thenReturn(Arrays.asList(user1Expense1, user1Expense2));

            List<Expense> result = expenseController.getAllExpenses();

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(exp -> exp.getUserId().equals("user-1"));
            verify(expenseRepository).findByUserId("user-1");
        }

        @Test
        @DisplayName("should return empty list when user has no expenses")
        void shouldReturnEmptyListForNewUser() {
            when(expenseRepository.findByUserId("user-1"))
                    .thenReturn(Collections.emptyList());

            List<Expense> result = expenseController.getAllExpenses();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /expenses/category/{category} - Branch Coverage")
    class GetExpensesByCategory {

        @Test
        @DisplayName("should return expenses filtered by category")
        void shouldReturnExpensesByCategory() {
            when(expenseRepository.findByUserId("user-1"))
                    .thenReturn(Arrays.asList(user1Expense1, user1Expense2));

            List<Expense> result = expenseController.getExpensesByCategory(ExpenseCategory.GROCERIES);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo(ExpenseCategory.GROCERIES);
            assertThat(result.get(0).getName()).isEqualTo("Groceries");
        }

        @Test
        @DisplayName("should return empty list when no expenses match category")
        void shouldReturnEmptyListWhenNoMatch() {
            when(expenseRepository.findByUserId("user-1"))
                    .thenReturn(Arrays.asList(user1Expense1, user1Expense2));

            List<Expense> result = expenseController.getExpensesByCategory(ExpenseCategory.ENTERTAINMENT);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle all expense categories")
        void shouldHandleAllCategories() {
            when(expenseRepository.findByUserId("user-1"))
                    .thenReturn(Arrays.asList(user1Expense1));

            // Test each category - should not throw exceptions
            for (ExpenseCategory category : ExpenseCategory.values()) {
                List<Expense> result = expenseController.getExpensesByCategory(category);
                assertThat(result).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("GET /expenses/fixed and /expenses/variable - Branch Coverage")
    class GetFixedAndVariableExpenses {

        @Test
        @DisplayName("should return only fixed expenses")
        void shouldReturnFixedExpenses() {
            when(expenseRepository.findByUserIdAndIsFixedTrue("user-1"))
                    .thenReturn(Arrays.asList(user1Expense1, user1Expense2));

            List<Expense> result = expenseController.getFixedExpenses();

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(Expense::getIsFixed);
        }

        @Test
        @DisplayName("should return only variable expenses")
        void shouldReturnVariableExpenses() {
            Expense variableExpense = Expense.builder()
                    .id("exp-4")
                    .userId("user-1")
                    .name("Entertainment")
                    .category(ExpenseCategory.ENTERTAINMENT)
                    .monthlyAmount(2000.0)
                    .isFixed(false)
                    .build();

            when(expenseRepository.findByUserIdAndIsFixedFalse("user-1"))
                    .thenReturn(Arrays.asList(variableExpense));

            List<Expense> result = expenseController.getVariableExpenses();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsFixed()).isFalse();
        }

        @Test
        @DisplayName("should return empty list when no fixed expenses")
        void shouldReturnEmptyListWhenNoFixedExpenses() {
            when(expenseRepository.findByUserIdAndIsFixedTrue("user-1"))
                    .thenReturn(Collections.emptyList());

            List<Expense> result = expenseController.getFixedExpenses();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /expenses/{id} - Branch Coverage")
    class GetExpenseById {

        @Test
        @DisplayName("should return expense when it belongs to user")
        void shouldReturnExpenseWhenBelongsToUser() {
            when(expenseRepository.findByIdAndUserId("exp-1", "user-1"))
                    .thenReturn(Optional.of(user1Expense1));

            Expense result = expenseController.getExpenseById("exp-1");

            assertThat(result.getId()).isEqualTo("exp-1");
            assertThat(result.getName()).isEqualTo("Groceries");
        }

        @Test
        @DisplayName("should throw exception when expense belongs to another user")
        void shouldThrowExceptionWhenExpenseBelongsToOtherUser() {
            when(expenseRepository.findByIdAndUserId("exp-3", "user-1"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseController.getExpenseById("exp-3"))
                    .isInstanceOf(com.retyrment.exception.ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when expense does not exist")
        void shouldThrowExceptionWhenExpenseNotFound() {
            when(expenseRepository.findByIdAndUserId("non-existent-id", "user-1"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseController.getExpenseById("non-existent-id"))
                    .isInstanceOf(com.retyrment.exception.ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("POST /expenses - Branch Coverage")
    class CreateExpense {

        @Test
        @DisplayName("should create expense with isFixed=true when not provided")
        void shouldCreateExpenseWithDefaultIsFixed() {
            Expense newExpense = Expense.builder()
                    .name("New Expense")
                    .category(ExpenseCategory.ENTERTAINMENT)
                    .monthlyAmount(3000.0)
                    .isFixed(null) // Not provided
                    .build();

            Expense savedExpense = Expense.builder()
                    .id("new-id")
                    .userId("user-1")
                    .name("New Expense")
                    .category(ExpenseCategory.ENTERTAINMENT)
                    .monthlyAmount(3000.0)
                    .isFixed(true) // Should default to true
                    .build();

            when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

            Expense result = expenseController.createExpense(newExpense);

            assertThat(result.getIsFixed()).isTrue();
            assertThat(result.getUserId()).isEqualTo("user-1");
            verify(expenseRepository).save(argThat(exp -> 
                exp.getUserId().equals("user-1") && exp.getIsFixed()));
        }

        @Test
        @DisplayName("should create expense with isFixed=false when explicitly set")
        void shouldCreateExpenseWithIsFixedFalse() {
            Expense newExpense = Expense.builder()
                    .name("Variable Expense")
                    .category(ExpenseCategory.SHOPPING)
                    .monthlyAmount(2000.0)
                    .isFixed(false)
                    .build();

            Expense savedExpense = Expense.builder()
                    .id("new-id")
                    .userId("user-1")
                    .name("Variable Expense")
                    .category(ExpenseCategory.SHOPPING)
                    .monthlyAmount(2000.0)
                    .isFixed(false)
                    .build();

            when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

            Expense result = expenseController.createExpense(newExpense);

            assertThat(result.getIsFixed()).isFalse();
            verify(expenseRepository).save(argThat(exp -> 
                exp.getUserId().equals("user-1") && !exp.getIsFixed()));
        }
    }

    @Nested
    @DisplayName("PUT /expenses/{id} - Branch Coverage")
    class UpdateExpense {

        @Test
        @DisplayName("should update expense when it belongs to user")
        void shouldUpdateExpenseWhenBelongsToUser() {
            Expense updatedExpense = Expense.builder()
                    .name("Updated Groceries")
                    .category(ExpenseCategory.GROCERIES)
                    .monthlyAmount(6000.0)
                    .isFixed(true)
                    .build();

            Expense savedExpense = Expense.builder()
                    .id("exp-1")
                    .userId("user-1")
                    .name("Updated Groceries")
                    .category(ExpenseCategory.GROCERIES)
                    .monthlyAmount(6000.0)
                    .isFixed(true)
                    .build();

            when(expenseRepository.findByIdAndUserId("exp-1", "user-1"))
                    .thenReturn(Optional.of(user1Expense1));
            when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

            Expense result = expenseController.updateExpense("exp-1", updatedExpense);

            assertThat(result.getName()).isEqualTo("Updated Groceries");
            assertThat(result.getUserId()).isEqualTo("user-1");
            verify(expenseRepository).save(argThat(exp -> 
                exp.getId().equals("exp-1") && exp.getUserId().equals("user-1")));
        }

        @Test
        @DisplayName("should throw exception when updating other user's expense")
        void shouldThrowExceptionWhenUpdatingOtherUserExpense() {
            Expense updatedExpense = Expense.builder()
                    .name("Hacked Expense")
                    .category(ExpenseCategory.GROCERIES)
                    .monthlyAmount(10000.0)
                    .build();

            when(expenseRepository.findByIdAndUserId("exp-3", "user-1"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseController.updateExpense("exp-3", updatedExpense))
                    .isInstanceOf(com.retyrment.exception.ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("DELETE /expenses/{id} - Branch Coverage")
    class DeleteExpense {

        @Test
        @DisplayName("should delete expense when it belongs to user")
        void shouldDeleteExpenseWhenBelongsToUser() {
            when(expenseRepository.existsByIdAndUserId("exp-1", "user-1"))
                    .thenReturn(true);
            doNothing().when(expenseRepository).deleteById("exp-1");

            expenseController.deleteExpense("exp-1");

            verify(expenseRepository).existsByIdAndUserId("exp-1", "user-1");
            verify(expenseRepository).deleteById("exp-1");
        }

        @Test
        @DisplayName("should throw exception when deleting other user's expense")
        void shouldThrowExceptionWhenDeletingOtherUserExpense() {
            when(expenseRepository.existsByIdAndUserId("exp-3", "user-1"))
                    .thenReturn(false);

            assertThatThrownBy(() -> expenseController.deleteExpense("exp-3"))
                    .isInstanceOf(com.retyrment.exception.ResourceNotFoundException.class);

            verify(expenseRepository, never()).deleteById(anyString());
        }
    }

    @Nested
    @DisplayName("getCurrentUserId - Branch Coverage")
    class GetCurrentUserId {

        @Test
        @DisplayName("should throw exception when authentication is null")
        void shouldThrowExceptionWhenAuthenticationIsNull() {
            // Override the setUp mock for this test
            when(securityContext.getAuthentication()).thenReturn(null);

            assertThatThrownBy(() -> expenseController.getAllExpenses())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("User not authenticated");
        }

        @Test
        @DisplayName("should throw exception when principal is not User")
        void shouldThrowExceptionWhenPrincipalIsNotUser() {
            // Override the setUp mock for this test
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn("not-a-user");

            assertThatThrownBy(() -> expenseController.getAllExpenses())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("User not authenticated");
        }
    }
}
