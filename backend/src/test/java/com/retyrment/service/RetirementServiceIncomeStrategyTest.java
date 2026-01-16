package com.retyrment.service;

import com.retyrment.model.*;
import com.retyrment.repository.*;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RetirementService Income Strategy Branch Coverage Tests")
class RetirementServiceIncomeStrategyTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private IncomeRepository incomeRepository;
    
    @Mock
    private LoanRepository loanRepository;
    
    @Mock
    private RetirementScenarioRepository scenarioRepository;

    @InjectMocks
    private RetirementService retirementService;
    
    @Mock
    private CalculationService calculationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(retirementService, "defaultEpfReturn", 8.15);
        ReflectionTestUtils.setField(retirementService, "defaultPpfReturn", 7.1);
        ReflectionTestUtils.setField(retirementService, "defaultMfReturn", 12.0);
        ReflectionTestUtils.setField(retirementService, "calculationService", calculationService);
        
        // Setup default mocks
        when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
        when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
        when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
        when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
        when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
        when(insuranceRepository.findByTypeIn(anyList())).thenReturn(Collections.emptyList());
        when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(java.util.Optional.empty());
    }

    @Nested
    @DisplayName("Income Strategy - SIMPLE_DEPLETION Branch Coverage")
    class SimpleDepletionBranches {

        @Test
        @DisplayName("should handle remainingYears = 0 in projection loop")
        void shouldHandleRemainingYearsZero() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(80)
                    .retirementAge(80)
                    .lifeExpectancy(85)
                    .incomeStrategy("SIMPLE_DEPLETION")
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            assertThat(summary).containsKey("incomeStrategy");
        }

        @Test
        @DisplayName("should handle year >= retirementYears in projection loop")
        void shouldHandleYearGreaterThanRetirementYears() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(65) // Only 5 retirement years
                    .incomeStrategy("SIMPLE_DEPLETION")
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle m loop boundary conditions in SIMPLE_DEPLETION")
        void shouldHandleMLoopBoundaryConditions() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SIMPLE_DEPLETION")
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }
    }

    @Nested
    @DisplayName("Income Strategy - SAFE_4_PERCENT Branch Coverage")
    class Safe4PercentBranches {

        @Test
        @DisplayName("should handle m loop boundary conditions in SAFE_4_PERCENT")
        void shouldHandleMLoopBoundaryConditions() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SAFE_4_PERCENT")
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle year >= retirementYears in SAFE_4_PERCENT")
        void shouldHandleYearGreaterThanRetirementYears() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(65) // Only 5 retirement years
                    .incomeStrategy("SAFE_4_PERCENT")
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }
    }

    @Nested
    @DisplayName("Income Strategy - SUSTAINABLE Branch Coverage")
    class SustainableBranches {

        @Test
        @DisplayName("should handle m loop boundary conditions in SUSTAINABLE")
        void shouldHandleMLoopBoundaryConditions() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(8.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle year >= retirementYears in SUSTAINABLE")
        void shouldHandleYearGreaterThanRetirementYears() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(65) // Only 5 retirement years
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(8.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle withdrawalRate = 0 in calculateGapAnalysis")
        void shouldHandleZeroWithdrawalRate() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(0.0)
                    .build();

            // Add expenses to trigger gap analysis
            Expense expense = Expense.builder()
                    .monthlyAmount(50000.0)
                    .isFixed(true)
                    .build();
            when(expenseRepository.findByUserId("test-user")).thenReturn(List.of(expense));

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }
    }

    @Nested
    @DisplayName("Income Strategy - Gap Analysis Branches")
    class GapAnalysisBranches {

        @Test
        @DisplayName("should handle SIMPLE_DEPLETION in gap analysis")
        void shouldHandleSimpleDepletionInGapAnalysis() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SIMPLE_DEPLETION")
                    .corpusReturnRate(10.0)
                    .build();

            Expense expense = Expense.builder()
                    .monthlyAmount(50000.0)
                    .isFixed(true)
                    .build();
            when(expenseRepository.findByUserId("test-user")).thenReturn(List.of(expense));

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle SAFE_4_PERCENT in gap analysis")
        void shouldHandleSafe4PercentInGapAnalysis() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SAFE_4_PERCENT")
                    .corpusReturnRate(10.0)
                    .build();

            Expense expense = Expense.builder()
                    .monthlyAmount(50000.0)
                    .isFixed(true)
                    .build();
            when(expenseRepository.findByUserId("test-user")).thenReturn(List.of(expense));

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle SUSTAINABLE with withdrawalRate > 0 in gap analysis")
        void shouldHandleSustainableWithPositiveWithdrawalRate() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(8.0)
                    .build();

            Expense expense = Expense.builder()
                    .monthlyAmount(50000.0)
                    .isFixed(true)
                    .build();
            when(expenseRepository.findByUserId("test-user")).thenReturn(List.of(expense));

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle SUSTAINABLE with withdrawalRate = 0 in gap analysis (fallback)")
        void shouldHandleSustainableWithZeroWithdrawalRate() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(0.0)
                    .build();

            Expense expense = Expense.builder()
                    .monthlyAmount(50000.0)
                    .isFixed(true)
                    .build();
            when(expenseRepository.findByUserId("test-user")).thenReturn(List.of(expense));

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }
    }

    @Nested
    @DisplayName("Income Strategy - Projection Loop Branches")
    class ProjectionLoopBranches {

        @Test
        @DisplayName("should handle year <= 30 boundary in projection loop")
        void shouldHandleYearBoundary30() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(100) // 40 retirement years, but loop stops at 30
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(8.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle year > retirementYears in projection loop")
        void shouldHandleYearGreaterThanRetirementYears() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(70) // Only 10 retirement years
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(8.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }
    }
}
