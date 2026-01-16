package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseController Unit Tests - Data Isolation")
class ExpenseControllerUnitTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ExpenseController expenseController;

    private Expense testExpense;
    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        
        testUser = User.builder()
                .id("user-1")
                .email("user1@example.com")
                .role(User.UserRole.FREE)
                .build();
        
        testExpense = Expense.builder()
                .id("exp-1")
                .userId("user-1")
                .name("Groceries")
                .category(ExpenseCategory.GROCERIES)
                .monthlyAmount(5000.0)
                .isFixed(true)
                .build();
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Nested
    @DisplayName("getAllExpenses - Data Isolation")
    class GetAllExpenses {
        @Test
        @DisplayName("should return only current user's expenses")
        void shouldReturnOnlyCurrentUserExpenses() {
            Expense user1Exp2 = Expense.builder().id("exp-2").userId("user-1").name("Rent").build();
            Expense user2Exp = Expense.builder().id("exp-3").userId("user-2").name("Other").build();
            
            when(expenseRepository.findByUserId("user-1")).thenReturn(Arrays.asList(testExpense, user1Exp2));

            List<Expense> result = expenseController.getAllExpenses();

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(exp -> exp.getUserId().equals("user-1"));
            verify(expenseRepository).findByUserId("user-1");
        }
    }

    @Nested
    @DisplayName("getExpensesByCategory - Branch Coverage")
    class GetExpensesByCategory {
        @Test
        @DisplayName("should filter expenses by category")
        void shouldFilterExpensesByCategory() {
            Expense groceries1 = Expense.builder()
                    .id("exp-1")
                    .userId("user-1")
                    .name("Groceries 1")
                    .category(ExpenseCategory.GROCERIES)
                    .build();
            Expense groceries2 = Expense.builder()
                    .id("exp-2")
                    .userId("user-1")
                    .name("Groceries 2")
                    .category(ExpenseCategory.GROCERIES)
                    .build();
            Expense rent = Expense.builder()
                    .id("exp-3")
                    .userId("user-1")
                    .name("Rent")
                    .category(ExpenseCategory.RENT)
                    .build();

            when(expenseRepository.findByUserId("user-1"))
                    .thenReturn(Arrays.asList(groceries1, groceries2, rent));

            List<Expense> result = expenseController.getExpensesByCategory(ExpenseCategory.GROCERIES);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(expense -> expense.getCategory() == ExpenseCategory.GROCERIES);
        }

        @Test
        @DisplayName("should return empty list when no expenses match category")
        void shouldReturnEmptyListWhenNoMatch() {
            Expense rent = Expense.builder()
                    .id("exp-1")
                    .userId("user-1")
                    .name("Rent")
                    .category(ExpenseCategory.RENT)
                    .build();

            when(expenseRepository.findByUserId("user-1"))
                    .thenReturn(Arrays.asList(rent));

            List<Expense> result = expenseController.getExpensesByCategory(ExpenseCategory.GROCERIES);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getFixedExpenses - Data Isolation")
    class GetFixedExpenses {
        @Test
        @DisplayName("should return only current user's fixed expenses")
        void shouldReturnOnlyCurrentUserFixedExpenses() {
            when(expenseRepository.findByUserIdAndIsFixedTrue("user-1"))
                    .thenReturn(Arrays.asList(testExpense));

            List<Expense> result = expenseController.getFixedExpenses();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo("user-1");
            assertThat(result.get(0).getIsFixed()).isTrue();
        }
    }

    @Nested
    @DisplayName("getExpenseById - Data Isolation")
    class GetExpenseById {
        @Test
        @DisplayName("should return expense when it belongs to current user")
        void shouldReturnExpenseWhenBelongsToUser() {
            when(expenseRepository.findByIdAndUserId("exp-1", "user-1"))
                    .thenReturn(Optional.of(testExpense));

            Expense result = expenseController.getExpenseById("exp-1");

            assertThat(result.getId()).isEqualTo("exp-1");
            assertThat(result.getUserId()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("should throw exception when expense belongs to another user")
        void shouldThrowExceptionWhenExpenseBelongsToOtherUser() {
            when(expenseRepository.findByIdAndUserId("exp-1", "user-1"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseController.getExpenseById("exp-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createExpense - Data Isolation")
    class CreateExpense {
        @Test
        @DisplayName("should automatically set userId from authenticated user")
        void shouldAutomaticallySetUserId() {
            Expense newExpense = Expense.builder()
                    .name("New Expense")
                    .category(ExpenseCategory.ENTERTAINMENT)
                    .monthlyAmount(2000.0)
                    .build();
            
            when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
                Expense e = inv.getArgument(0);
                e.setId("new-id");
                return e;
            });

            Expense result = expenseController.createExpense(newExpense);

            assertThat(result.getUserId()).isEqualTo("user-1");
            verify(expenseRepository).save(argThat(exp -> exp.getUserId().equals("user-1")));
        }

        @Test
        @DisplayName("should set default isFixed to true when null")
        void shouldSetDefaultIsFixedWhenNull() {
            Expense newExpense = Expense.builder()
                    .name("New Expense")
                    .category(ExpenseCategory.ENTERTAINMENT)
                    .monthlyAmount(2000.0)
                    .isFixed(null) // Explicitly null
                    .build();
            
            when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
                Expense e = inv.getArgument(0);
                e.setId("new-id");
                return e;
            });

            Expense result = expenseController.createExpense(newExpense);

            assertThat(result.getIsFixed()).isTrue();
            verify(expenseRepository).save(argThat(exp -> exp.getIsFixed()));
        }

        @Test
        @DisplayName("should retain isFixed as false when explicitly false")
        void shouldRetainIsFixedAsFalse() {
            Expense newExpense = Expense.builder()
                    .name("Variable Expense")
                    .category(ExpenseCategory.ENTERTAINMENT)
                    .monthlyAmount(2000.0)
                    .isFixed(false)
                    .build();
            
            when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
                Expense e = inv.getArgument(0);
                e.setId("new-id");
                return e;
            });

            Expense result = expenseController.createExpense(newExpense);

            assertThat(result.getIsFixed()).isFalse();
            verify(expenseRepository).save(argThat(exp -> !exp.getIsFixed()));
        }
    }

    @Nested
    @DisplayName("updateExpense - Data Isolation")
    class UpdateExpense {
        @Test
        @DisplayName("should update expense when it belongs to current user")
        void shouldUpdateExpenseWhenBelongsToUser() {
            when(expenseRepository.findByIdAndUserId("exp-1", "user-1"))
                    .thenReturn(Optional.of(testExpense));
            when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

            Expense updated = Expense.builder().name("Updated Expense").build();
            Expense result = expenseController.updateExpense("exp-1", updated);

            assertThat(result).isNotNull();
            verify(expenseRepository).save(argThat(exp -> 
                exp.getId().equals("exp-1") && exp.getUserId().equals("user-1")));
        }

        @Test
        @DisplayName("should throw exception when updating other user's expense")
        void shouldThrowExceptionWhenUpdatingOtherUserExpense() {
            when(expenseRepository.findByIdAndUserId("exp-1", "user-1"))
                    .thenReturn(Optional.empty());

            Expense updated = Expense.builder().name("Updated").build();
            
            assertThatThrownBy(() -> expenseController.updateExpense("exp-1", updated))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteExpense - Data Isolation")
    class DeleteExpense {
        @Test
        @DisplayName("should delete expense when it belongs to current user")
        void shouldDeleteExpenseWhenBelongsToUser() {
            when(expenseRepository.existsByIdAndUserId("exp-1", "user-1")).thenReturn(true);
            doNothing().when(expenseRepository).deleteById("exp-1");

            expenseController.deleteExpense("exp-1");

            verify(expenseRepository).existsByIdAndUserId("exp-1", "user-1");
            verify(expenseRepository).deleteById("exp-1");
        }

        @Test
        @DisplayName("should throw exception when deleting other user's expense")
        void shouldThrowExceptionWhenDeletingOtherUserExpense() {
            when(expenseRepository.existsByIdAndUserId("exp-1", "user-1")).thenReturn(false);

            assertThatThrownBy(() -> expenseController.deleteExpense("exp-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
