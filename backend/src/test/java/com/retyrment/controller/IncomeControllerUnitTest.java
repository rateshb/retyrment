package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
import com.retyrment.model.Income;
import com.retyrment.model.User;
import com.retyrment.repository.IncomeRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncomeController Unit Tests")
class IncomeControllerUnitTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private IncomeController incomeController;

    private Income testIncome;
    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        
        testUser = User.builder()
                .id("user-1")
                .email("user1@example.com")
                .role(User.UserRole.FREE)
                .build();
        
        otherUser = User.builder()
                .id("user-2")
                .email("user2@example.com")
                .role(User.UserRole.FREE)
                .build();
        
        testIncome = Income.builder()
                .id("income-1")
                .userId("user-1")
                .source("Salary")
                .monthlyAmount(100000.0)
                .annualIncrement(7.0)
                .isActive(true)
                .build();
        
        // Setup default authentication
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Nested
    @DisplayName("getAllIncome - Data Isolation")
    class GetAllIncome {
        @Test
        @DisplayName("should return only current user's incomes")
        void shouldReturnOnlyCurrentUserIncomes() {
            Income user1Income2 = Income.builder().id("income-2").userId("user-1").source("Freelance").build();
            Income user2Income = Income.builder().id("income-3").userId("user-2").source("Other").build();
            
            when(incomeRepository.findByUserId("user-1")).thenReturn(Arrays.asList(testIncome, user1Income2));

            List<Income> result = incomeController.getAllIncome();

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(income -> income.getUserId().equals("user-1"));
            verify(incomeRepository).findByUserId("user-1");
            verify(incomeRepository, never()).findAll();
        }

        @Test
        @DisplayName("should return empty list when user has no incomes")
        void shouldReturnEmptyListWhenNoIncomes() {
            when(incomeRepository.findByUserId("user-1")).thenReturn(Arrays.asList());

            List<Income> result = incomeController.getAllIncome();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when user not authenticated")
        void shouldThrowExceptionWhenNotAuthenticated() {
            when(authentication.getPrincipal()).thenReturn("anonymousUser");

            assertThatThrownBy(() -> incomeController.getAllIncome())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("User not authenticated");
        }
    }

    @Nested
    @DisplayName("getActiveIncome - Data Isolation")
    class GetActiveIncome {
        @Test
        @DisplayName("should return only current user's active incomes")
        void shouldReturnOnlyCurrentUserActiveIncomes() {
            when(incomeRepository.findByUserIdAndIsActiveTrue("user-1")).thenReturn(Arrays.asList(testIncome));

            List<Income> result = incomeController.getActiveIncome();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo("user-1");
            assertThat(result.get(0).getIsActive()).isTrue();
            verify(incomeRepository).findByUserIdAndIsActiveTrue("user-1");
        }
    }

    @Nested
    @DisplayName("getIncomeById - Data Isolation")
    class GetIncomeById {
        @Test
        @DisplayName("should return income when it belongs to current user")
        void shouldReturnIncomeWhenBelongsToUser() {
            when(incomeRepository.findByIdAndUserId("income-1", "user-1")).thenReturn(Optional.of(testIncome));

            Income result = incomeController.getIncomeById("income-1");

            assertThat(result.getId()).isEqualTo("income-1");
            assertThat(result.getUserId()).isEqualTo("user-1");
            verify(incomeRepository).findByIdAndUserId("income-1", "user-1");
        }

        @Test
        @DisplayName("should throw exception when income belongs to another user")
        void shouldThrowExceptionWhenIncomeBelongsToOtherUser() {
            Income otherUserIncome = Income.builder().id("income-1").userId("user-2").build();
            when(incomeRepository.findByIdAndUserId("income-1", "user-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incomeController.getIncomeById("income-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
            
            verify(incomeRepository).findByIdAndUserId("income-1", "user-1");
            verify(incomeRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("should throw exception when income not found")
        void shouldThrowExceptionWhenNotFound() {
            when(incomeRepository.findByIdAndUserId("invalid-id", "user-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incomeController.getIncomeById("invalid-id"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createIncome - Data Isolation")
    class CreateIncome {
        @Test
        @DisplayName("should automatically set userId from authenticated user")
        void shouldAutomaticallySetUserId() {
            Income newIncome = Income.builder()
                    .source("Bonus")
                    .monthlyAmount(20000.0)
                    .build();
            
            when(incomeRepository.save(any(Income.class))).thenAnswer(inv -> {
                Income i = inv.getArgument(0);
                i.setId("new-id");
                return i;
            });

            Income result = incomeController.createIncome(newIncome);

            assertThat(result.getUserId()).isEqualTo("user-1");
            assertThat(result.getIsActive()).isTrue();
            verify(incomeRepository).save(argThat(income -> income.getUserId().equals("user-1")));
        }

        @Test
        @DisplayName("should override userId if provided in request")
        void shouldOverrideUserIdIfProvided() {
            Income newIncome = Income.builder()
                    .userId("user-2") // Attempt to set different userId
                    .source("Bonus")
                    .monthlyAmount(20000.0)
                    .build();
            
            when(incomeRepository.save(any(Income.class))).thenAnswer(inv -> {
                Income i = inv.getArgument(0);
                i.setId("new-id");
                return i;
            });

            Income result = incomeController.createIncome(newIncome);

            // userId should be overridden to current user
            assertThat(result.getUserId()).isEqualTo("user-1");
            verify(incomeRepository).save(argThat(income -> income.getUserId().equals("user-1")));
        }

        @Test
        @DisplayName("should set default isActive to true when null")
        void shouldSetDefaultIsActiveWhenNull() {
            Income newIncome = Income.builder()
                    .source("Freelance")
                    .monthlyAmount(50000.0)
                    .isActive(null)
                    .build();
            
            when(incomeRepository.save(any(Income.class))).thenAnswer(inv -> inv.getArgument(0));

            incomeController.createIncome(newIncome);

            verify(incomeRepository).save(argThat(income -> income.getIsActive().equals(true)));
        }

        @Test
        @DisplayName("should preserve isActive when provided as false")
        void shouldPreserveIsActiveWhenProvidedAsFalse() {
            Income newIncome = Income.builder()
                    .source("Freelance")
                    .monthlyAmount(50000.0)
                    .isActive(false)
                    .build();
            
            when(incomeRepository.save(any(Income.class))).thenAnswer(inv -> inv.getArgument(0));

            incomeController.createIncome(newIncome);

            verify(incomeRepository).save(argThat(income -> income.getIsActive().equals(false)));
        }

        @Test
        @DisplayName("should preserve isActive when provided as true")
        void shouldPreserveIsActiveWhenProvidedAsTrue() {
            Income newIncome = Income.builder()
                    .source("Freelance")
                    .monthlyAmount(50000.0)
                    .isActive(true)
                    .build();
            
            when(incomeRepository.save(any(Income.class))).thenAnswer(inv -> inv.getArgument(0));

            incomeController.createIncome(newIncome);

            verify(incomeRepository).save(argThat(income -> income.getIsActive().equals(true)));
        }
    }

    @Nested
    @DisplayName("updateIncome - Data Isolation")
    class UpdateIncome {
        @Test
        @DisplayName("should update income when it belongs to current user")
        void shouldUpdateIncomeWhenBelongsToUser() {
            when(incomeRepository.findByIdAndUserId("income-1", "user-1")).thenReturn(Optional.of(testIncome));
            when(incomeRepository.save(any(Income.class))).thenReturn(testIncome);

            Income updated = Income.builder()
                    .source("Updated Salary")
                    .monthlyAmount(120000.0)
                    .build();
            
            Income result = incomeController.updateIncome("income-1", updated);

            assertThat(result).isNotNull();
            verify(incomeRepository).findByIdAndUserId("income-1", "user-1");
            verify(incomeRepository).save(argThat(income -> 
                income.getId().equals("income-1") && income.getUserId().equals("user-1")));
        }

        @Test
        @DisplayName("should preserve userId and prevent changing it")
        void shouldPreserveUserId() {
            when(incomeRepository.findByIdAndUserId("income-1", "user-1")).thenReturn(Optional.of(testIncome));
            when(incomeRepository.save(any(Income.class))).thenReturn(testIncome);

            Income updated = Income.builder()
                    .userId("user-2") // Attempt to change userId
                    .source("Updated Salary")
                    .build();
            
            Income result = incomeController.updateIncome("income-1", updated);

            verify(incomeRepository).save(argThat(income -> 
                income.getUserId().equals("user-1"))); // Should remain user-1
        }

        @Test
        @DisplayName("should throw exception when updating other user's income")
        void shouldThrowExceptionWhenUpdatingOtherUserIncome() {
            when(incomeRepository.findByIdAndUserId("income-1", "user-1")).thenReturn(Optional.empty());

            Income updated = Income.builder().source("Updated").build();
            
            assertThatThrownBy(() -> incomeController.updateIncome("income-1", updated))
                    .isInstanceOf(ResourceNotFoundException.class);
            
            verify(incomeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteIncome - Data Isolation")
    class DeleteIncome {
        @Test
        @DisplayName("should delete income when it belongs to current user")
        void shouldDeleteIncomeWhenBelongsToUser() {
            when(incomeRepository.existsByIdAndUserId("income-1", "user-1")).thenReturn(true);
            doNothing().when(incomeRepository).deleteById("income-1");

            incomeController.deleteIncome("income-1");

            verify(incomeRepository).existsByIdAndUserId("income-1", "user-1");
            verify(incomeRepository).deleteById("income-1");
        }

        @Test
        @DisplayName("should throw exception when deleting other user's income")
        void shouldThrowExceptionWhenDeletingOtherUserIncome() {
            when(incomeRepository.existsByIdAndUserId("income-1", "user-1")).thenReturn(false);

            assertThatThrownBy(() -> incomeController.deleteIncome("income-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
            
            verify(incomeRepository).existsByIdAndUserId("income-1", "user-1");
            verify(incomeRepository, never()).deleteById(anyString());
        }
    }
}
