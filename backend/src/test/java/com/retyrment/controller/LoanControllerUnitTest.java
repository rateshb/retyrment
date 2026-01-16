package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
import com.retyrment.model.Loan;
import com.retyrment.model.Loan.LoanType;
import com.retyrment.model.User;
import com.retyrment.repository.LoanRepository;
import com.retyrment.service.CalculationService;
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

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanController Unit Tests - Data Isolation")
class LoanControllerUnitTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private CalculationService calculationService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LoanController loanController;

    private Loan testLoan;
    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        
        testUser = User.builder()
                .id("user-1")
                .email("user1@example.com")
                .role(User.UserRole.FREE)
                .build();
        
        testLoan = Loan.builder()
                .id("loan-1")
                .userId("user-1")
                .name("Home Loan")
                .type(LoanType.HOME)
                .originalAmount(5000000.0)
                .outstandingAmount(4500000.0)
                .interestRate(8.5)
                .emi(45000.0)
                .remainingMonths(180)
                .build();
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Nested
    @DisplayName("getAllLoans - Data Isolation")
    class GetAllLoans {
        @Test
        @DisplayName("should return only current user's loans")
        void shouldReturnOnlyCurrentUserLoans() {
            Loan user1Loan2 = Loan.builder().id("loan-2").userId("user-1").name("Car Loan").build();
            Loan user2Loan = Loan.builder().id("loan-3").userId("user-2").name("Other").build();
            
            when(loanRepository.findByUserId("user-1")).thenReturn(Arrays.asList(testLoan, user1Loan2));

            List<Loan> result = loanController.getAllLoans();

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(loan -> loan.getUserId().equals("user-1"));
            verify(loanRepository).findByUserId("user-1");
        }
    }

    @Nested
    @DisplayName("getLoansByType - Branch Coverage")
    class GetLoansByType {
        @Test
        @DisplayName("should filter loans by type")
        void shouldFilterLoansByType() {
            Loan homeLoan = Loan.builder()
                    .id("loan-2")
                    .userId("user-1")
                    .type(LoanType.HOME)
                    .build();
            Loan personalLoan = Loan.builder()
                    .id("loan-3")
                    .userId("user-1")
                    .type(LoanType.PERSONAL)
                    .build();

            when(loanRepository.findByUserId("user-1"))
                    .thenReturn(Arrays.asList(testLoan, homeLoan, personalLoan));

            List<Loan> result = loanController.getLoansByType(LoanType.HOME);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(loan -> loan.getType() == LoanType.HOME);
        }

        @Test
        @DisplayName("should return empty list when no loans match type")
        void shouldReturnEmptyListWhenNoMatch() {
            when(loanRepository.findByUserId("user-1"))
                    .thenReturn(Arrays.asList(testLoan));

            List<Loan> result = loanController.getLoansByType(LoanType.EDUCATION);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getActiveLoans - Data Isolation")
    class GetActiveLoans {
        @Test
        @DisplayName("should return only current user's active loans")
        void shouldReturnOnlyCurrentUserActiveLoans() {
            when(loanRepository.findByUserIdAndRemainingMonthsGreaterThan("user-1", 0))
                    .thenReturn(Arrays.asList(testLoan));

            List<Loan> result = loanController.getActiveLoans();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo("user-1");
        }
    }

    @Nested
    @DisplayName("getAmortizationSchedule - Branch Coverage")
    class GetAmortizationSchedule {
        @Test
        @DisplayName("should return amortization schedule when loan exists")
        void shouldReturnAmortizationSchedule() {
            List<Map<String, Object>> schedule = Arrays.asList(new HashMap<>());
            when(loanRepository.findByIdAndUserId("loan-1", "user-1"))
                    .thenReturn(Optional.of(testLoan));
            when(calculationService.calculateAmortization(testLoan)).thenReturn(schedule);

            List<Map<String, Object>> result = loanController.getAmortizationSchedule("loan-1");

            assertThat(result).isEqualTo(schedule);
        }

        @Test
        @DisplayName("should throw exception when loan does not exist")
        void shouldThrowExceptionWhenLoanNotFound() {
            when(loanRepository.findByIdAndUserId("loan-1", "user-1"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanController.getAmortizationSchedule("loan-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getLoanById - Data Isolation")
    class GetLoanById {
        @Test
        @DisplayName("should return loan when it belongs to current user")
        void shouldReturnLoanWhenBelongsToUser() {
            when(loanRepository.findByIdAndUserId("loan-1", "user-1"))
                    .thenReturn(Optional.of(testLoan));

            Loan result = loanController.getLoanById("loan-1");

            assertThat(result.getId()).isEqualTo("loan-1");
            assertThat(result.getUserId()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("should throw exception when loan belongs to another user")
        void shouldThrowExceptionWhenLoanBelongsToOtherUser() {
            when(loanRepository.findByIdAndUserId("loan-1", "user-1"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanController.getLoanById("loan-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createLoan - Data Isolation")
    class CreateLoan {
        @Test
        @DisplayName("should automatically set userId from authenticated user")
        void shouldAutomaticallySetUserId() {
            Loan newLoan = Loan.builder()
                    .name("New Loan")
                    .type(LoanType.PERSONAL)
                    .originalAmount(100000.0)
                    .build();
            
            when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
                Loan l = inv.getArgument(0);
                l.setId("new-id");
                return l;
            });

            Loan result = loanController.createLoan(newLoan);

            assertThat(result.getUserId()).isEqualTo("user-1");
            verify(loanRepository).save(argThat(loan -> loan.getUserId().equals("user-1")));
        }
    }

    @Nested
    @DisplayName("updateLoan - Data Isolation")
    class UpdateLoan {
        @Test
        @DisplayName("should update loan when it belongs to current user")
        void shouldUpdateLoanWhenBelongsToUser() {
            when(loanRepository.findByIdAndUserId("loan-1", "user-1"))
                    .thenReturn(Optional.of(testLoan));
            when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

            Loan updated = Loan.builder().name("Updated Loan").build();
            Loan result = loanController.updateLoan("loan-1", updated);

            assertThat(result).isNotNull();
            verify(loanRepository).save(argThat(loan -> 
                loan.getId().equals("loan-1") && loan.getUserId().equals("user-1")));
        }

        @Test
        @DisplayName("should throw exception when updating other user's loan")
        void shouldThrowExceptionWhenUpdatingOtherUserLoan() {
            when(loanRepository.findByIdAndUserId("loan-1", "user-1"))
                    .thenReturn(Optional.empty());

            Loan updated = Loan.builder().name("Updated").build();
            
            assertThatThrownBy(() -> loanController.updateLoan("loan-1", updated))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteLoan - Data Isolation")
    class DeleteLoan {
        @Test
        @DisplayName("should delete loan when it belongs to current user")
        void shouldDeleteLoanWhenBelongsToUser() {
            when(loanRepository.existsByIdAndUserId("loan-1", "user-1")).thenReturn(true);
            doNothing().when(loanRepository).deleteById("loan-1");

            loanController.deleteLoan("loan-1");

            verify(loanRepository).existsByIdAndUserId("loan-1", "user-1");
            verify(loanRepository).deleteById("loan-1");
        }

        @Test
        @DisplayName("should throw exception when deleting other user's loan")
        void shouldThrowExceptionWhenDeletingOtherUserLoan() {
            when(loanRepository.existsByIdAndUserId("loan-1", "user-1")).thenReturn(false);

            assertThatThrownBy(() -> loanController.deleteLoan("loan-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
