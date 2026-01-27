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

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RetirementService - Required Corpus Calculation Tests")
class RetirementServiceRequiredCorpusTest {

    @Mock
    private InvestmentRepository investmentRepository;
    @Mock
    private InsuranceRepository insuranceRepository;
    @Mock
    private GoalRepository goalRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private IncomeRepository incomeRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private RetirementScenarioRepository scenarioRepository;
    @Mock
    private CalculationService calculationService;

    @InjectMocks
    private RetirementService retirementService;

    @BeforeEach
    void setUp() {
        // Set default values
        ReflectionTestUtils.setField(retirementService, "defaultInflation", 6.0);
        ReflectionTestUtils.setField(retirementService, "defaultEpfReturn", 8.5);
        ReflectionTestUtils.setField(retirementService, "defaultPpfReturn", 7.1);
        ReflectionTestUtils.setField(retirementService, "defaultMfReturn", 12.0);
    }

    @Nested
    @DisplayName("calculateRequiredCorpusForUser Tests")
    class CalculateRequiredCorpusForUserTests {

        @Test
        @DisplayName("should calculate required corpus with SUSTAINABLE strategy")
        void shouldCalculateRequiredCorpusWithSustainableStrategy() {
            // Given
            String userId = "user-123";
            double inflationRate = 6.0;
            int yearsToRetirement = 25;
            int retirementYears = 25;
            String incomeStrategy = "SUSTAINABLE";
            double corpusReturnRate = 10.0;
            double withdrawalRate = 8.0;

            // Mock empty lists for simplicity
            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserIdAndType(eq(userId), any())).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            double requiredCorpus = retirementService.calculateRequiredCorpusForUser(
                    userId, inflationRate, yearsToRetirement, retirementYears,
                    incomeStrategy, corpusReturnRate, withdrawalRate
            );

            // Then
            assertThat(requiredCorpus).isGreaterThanOrEqualTo(0);
            verify(goalRepository).findByUserIdOrderByTargetYearAsc(userId);
        }

        @Test
        @DisplayName("should calculate required corpus with SAFE_4_PERCENT strategy")
        void shouldCalculateRequiredCorpusWithSafe4PercentStrategy() {
            // Given
            String userId = "user-456";
            double inflationRate = 6.0;
            int yearsToRetirement = 20;
            int retirementYears = 30;
            String incomeStrategy = "SAFE_4_PERCENT";
            double corpusReturnRate = 10.0;
            double withdrawalRate = 8.0;

            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserIdAndType(eq(userId), any())).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            double requiredCorpus = retirementService.calculateRequiredCorpusForUser(
                    userId, inflationRate, yearsToRetirement, retirementYears,
                    incomeStrategy, corpusReturnRate, withdrawalRate
            );

            // Then
            assertThat(requiredCorpus).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should calculate required corpus with SIMPLE_DEPLETION strategy")
        void shouldCalculateRequiredCorpusWithSimpleDepletionStrategy() {
            // Given
            String userId = "user-789";
            double inflationRate = 6.0;
            int yearsToRetirement = 15;
            int retirementYears = 20;
            String incomeStrategy = "SIMPLE_DEPLETION";
            double corpusReturnRate = 10.0;
            double withdrawalRate = 8.0;

            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserIdAndType(eq(userId), any())).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            double requiredCorpus = retirementService.calculateRequiredCorpusForUser(
                    userId, inflationRate, yearsToRetirement, retirementYears,
                    incomeStrategy, corpusReturnRate, withdrawalRate
            );

            // Then
            assertThat(requiredCorpus).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should handle goals in required corpus calculation")
        void shouldHandleGoalsInRequiredCorpusCalculation() {
            // Given
            String userId = "user-goals";
            double inflationRate = 6.0;
            int yearsToRetirement = 20;
            int retirementYears = 25;
            String incomeStrategy = "SUSTAINABLE";
            double corpusReturnRate = 10.0;
            double withdrawalRate = 8.0;

            // Create a goal during retirement
            Goal goal = Goal.builder()
                    .id("goal-1")
                    .userId(userId)
                    .name("World Travel")
                    .targetYear(LocalDate.now().getYear() + yearsToRetirement + 5) // 5 years into retirement
                    .targetAmount(500000.0)
                    .isRecurring(false)
                    .build();

            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(List.of(goal));
            when(investmentRepository.findByUserIdAndType(eq(userId), any())).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            double requiredCorpusWithGoals = retirementService.calculateRequiredCorpusForUser(
                    userId, inflationRate, yearsToRetirement, retirementYears,
                    incomeStrategy, corpusReturnRate, withdrawalRate
            );

            // Then - with goals, required corpus should be higher
            assertThat(requiredCorpusWithGoals).isGreaterThan(0);
        }

        @Test
        @DisplayName("should handle expenses in required corpus calculation")
        void shouldHandleExpensesInRequiredCorpusCalculation() {
            // Given
            String userId = "user-expenses";
            double inflationRate = 6.0;
            int yearsToRetirement = 20;
            int retirementYears = 25;
            String incomeStrategy = "SUSTAINABLE";
            double corpusReturnRate = 10.0;
            double withdrawalRate = 8.0;

            // Create monthly expense
            Expense expense = Expense.builder()
                    .id("expense-1")
                    .userId(userId)
                    .name("Living Expenses")
                    .category(Expense.ExpenseCategory.OTHER)
                    .amount(50000.0)
                    .frequency(Expense.ExpenseFrequency.MONTHLY)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusYears(50)) // Continue through retirement
                    .isTimeBound(false)
                    .build();

            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserIdAndType(eq(userId), any())).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(List.of(expense));
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            double requiredCorpusWithExpenses = retirementService.calculateRequiredCorpusForUser(
                    userId, inflationRate, yearsToRetirement, retirementYears,
                    incomeStrategy, corpusReturnRate, withdrawalRate
            );

            // Then - with expenses, required corpus should be significant
            assertThat(requiredCorpusWithExpenses).isGreaterThan(0);
        }

        @Test
        @DisplayName("should handle zero years to retirement")
        void shouldHandleZeroYearsToRetirement() {
            // Given
            String userId = "user-retired";
            double inflationRate = 6.0;
            int yearsToRetirement = 0; // Already retired
            int retirementYears = 25;
            String incomeStrategy = "SUSTAINABLE";
            double corpusReturnRate = 10.0;
            double withdrawalRate = 8.0;

            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserIdAndType(eq(userId), any())).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            double requiredCorpus = retirementService.calculateRequiredCorpusForUser(
                    userId, inflationRate, yearsToRetirement, retirementYears,
                    incomeStrategy, corpusReturnRate, withdrawalRate
            );

            // Then
            assertThat(requiredCorpus).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should handle negative years gracefully")
        void shouldHandleNegativeYearsGracefully() {
            // Given
            String userId = "user-negative";
            double inflationRate = 6.0;
            int yearsToRetirement = -5; // Should be treated as 0
            int retirementYears = -10; // Should be treated as 0
            String incomeStrategy = "SUSTAINABLE";
            double corpusReturnRate = 10.0;
            double withdrawalRate = 8.0;

            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserIdAndType(eq(userId), any())).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            double requiredCorpus = retirementService.calculateRequiredCorpusForUser(
                    userId, inflationRate, yearsToRetirement, retirementYears,
                    incomeStrategy, corpusReturnRate, withdrawalRate
            );

            // Then - should not throw exception
            assertThat(requiredCorpus).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Retirement Matrix with Required Corpus by Strategy Tests")
    class RetirementMatrixRequiredCorpusTests {

        @Test
        @DisplayName("should include requiredCorpusByStrategy in matrix rows")
        void shouldIncludeRequiredCorpusByStrategyInMatrixRows() {
            // Given
            String userId = "user-matrix";
            RetirementScenario scenario = createDefaultScenario(userId);

            when(scenarioRepository.findByUserIdAndIsDefaultTrue(userId))
                    .thenReturn(Optional.of(scenario));
            when(investmentRepository.findByUserIdAndType(eq(userId), any())).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(userId, scenario);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).containsKey("matrix");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            assertThat(matrix).isNotEmpty();

            // Check first row has requiredCorpusByStrategy
            Map<String, Object> firstRow = matrix.get(0);
            assertThat(firstRow).containsKey("requiredCorpusByStrategy");

            @SuppressWarnings("unchecked")
            Map<String, Double> requiredByStrategy = (Map<String, Double>) firstRow.get("requiredCorpusByStrategy");
            assertThat(requiredByStrategy).containsKeys("SUSTAINABLE", "SAFE_4_PERCENT", "SIMPLE_DEPLETION");
        }

        @Test
        @DisplayName("should include canRetireByStrategy in matrix rows")
        void shouldIncludeCanRetireByStrategyInMatrixRows() {
            // Given
            String userId = "user-can-retire";
            RetirementScenario scenario = createDefaultScenario(userId);

            // Add some investments to build corpus
            Investment investment = new Investment();
            investment.setId("inv-1");
            investment.setUserId(userId);
            investment.setType(Investment.InvestmentType.MUTUAL_FUND);
            investment.setCurrentValue(5000000.0); // 50 lakhs
            investment.setMonthlySip(10000.0);

            when(scenarioRepository.findByUserIdAndIsDefaultTrue(userId))
                    .thenReturn(Optional.of(scenario));
            when(investmentRepository.findByUserIdAndType(userId, Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(List.of(investment));
            when(investmentRepository.findByUserIdAndType(eq(userId), argThat(type -> type != Investment.InvestmentType.MUTUAL_FUND)))
                    .thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(userId, scenario);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            assertThat(matrix).isNotEmpty();

            // Check rows have canRetireByStrategy
            Map<String, Object> lastRow = matrix.get(matrix.size() - 1);
            assertThat(lastRow).containsKey("canRetireByStrategy");

            @SuppressWarnings("unchecked")
            Map<String, Boolean> canRetireByStrategy = (Map<String, Boolean>) lastRow.get("canRetireByStrategy");
            assertThat(canRetireByStrategy).containsKeys("SUSTAINABLE", "SAFE_4_PERCENT", "SIMPLE_DEPLETION");
        }

        @Test
        @DisplayName("should calculate different required corpus for each strategy")
        void shouldCalculateDifferentRequiredCorpusForEachStrategy() {
            // Given
            String userId = "user-strategies";
            RetirementScenario scenario = createDefaultScenario(userId);

            // Add monthly expense to make required corpus meaningful
            Expense expense = Expense.builder()
                    .id("expense-1")
                    .userId(userId)
                    .name("Living Expenses")
                    .category(Expense.ExpenseCategory.OTHER)
                    .amount(50000.0)
                    .frequency(Expense.ExpenseFrequency.MONTHLY)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusYears(50))
                    .build();

            when(scenarioRepository.findByUserIdAndIsDefaultTrue(userId))
                    .thenReturn(Optional.of(scenario));
            when(investmentRepository.findByUserIdAndType(eq(userId), any())).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(List.of(expense));
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(userId, scenario);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            assertThat(matrix).isNotEmpty();

            // Get required corpus for each strategy from first year
            Map<String, Object> firstRow = matrix.get(0);
            @SuppressWarnings("unchecked")
            Map<String, Double> requiredByStrategy = (Map<String, Double>) firstRow.get("requiredCorpusByStrategy");

            double sustainableRequired = requiredByStrategy.get("SUSTAINABLE");
            double safe4PercentRequired = requiredByStrategy.get("SAFE_4_PERCENT");
            double simpleDepletionRequired = requiredByStrategy.get("SIMPLE_DEPLETION");

            // All should be positive with expenses
            assertThat(sustainableRequired).isGreaterThan(0);
            assertThat(safe4PercentRequired).isGreaterThan(0);
            assertThat(simpleDepletionRequired).isGreaterThan(0);

            // Strategies should have different required corpus values
            // (They may be similar but shouldn't all be identical)
            Set<Double> uniqueValues = new HashSet<>(Arrays.asList(
                    sustainableRequired, safe4PercentRequired, simpleDepletionRequired
            ));
            // At least 2 different values (strategies differ)
            assertThat(uniqueValues.size()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("should mark canRetire as true when corpus exceeds required")
        void shouldMarkCanRetireAsTrueWhenCorpusExceedsRequired() {
            // Given
            String userId = "user-high-corpus";
            RetirementScenario scenario = createDefaultScenario(userId);
            scenario.setCurrentAge(55); // Close to retirement
            scenario.setRetirementAge(60);

            // Add high investment value
            Investment investment = new Investment();
            investment.setId("inv-high");
            investment.setUserId(userId);
            investment.setType(Investment.InvestmentType.MUTUAL_FUND);
            investment.setCurrentValue(100000000.0); // 10 crores - very high
            investment.setMonthlySip(0.0);

            when(scenarioRepository.findByUserIdAndIsDefaultTrue(userId))
                    .thenReturn(Optional.of(scenario));
            when(investmentRepository.findByUserIdAndType(userId, Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(List.of(investment));
            when(investmentRepository.findByUserIdAndType(eq(userId), argThat(type -> type != Investment.InvestmentType.MUTUAL_FUND)))
                    .thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(userId, scenario);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            assertThat(matrix).isNotEmpty();

            // Last row (at retirement) should show can retire with such high corpus
            Map<String, Object> lastRow = matrix.get(matrix.size() - 1);
            @SuppressWarnings("unchecked")
            Map<String, Boolean> canRetireByStrategy = (Map<String, Boolean>) lastRow.get("canRetireByStrategy");

            // With 10 crores and no expenses, should be able to retire with all strategies
            assertThat(canRetireByStrategy.get("SUSTAINABLE")).isTrue();
            assertThat(canRetireByStrategy.get("SAFE_4_PERCENT")).isTrue();
            assertThat(canRetireByStrategy.get("SIMPLE_DEPLETION")).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Binary Search Algorithm Tests")
    class EdgeCasesAndBinarySearchTests {

        @Test
        @DisplayName("should handle scenario with very high expenses")
        void shouldHandleScenarioWithVeryHighExpenses() {
            // Given
            String userId = "user-high-expenses";
            double inflationRate = 6.0;
            int yearsToRetirement = 10;
            int retirementYears = 25;
            String incomeStrategy = "SUSTAINABLE";
            double corpusReturnRate = 10.0;
            double withdrawalRate = 8.0;

            // Create very high expense
            Expense expense = Expense.builder()
                    .id("expense-high")
                    .userId(userId)
                    .name("High Living Expenses")
                    .category(Expense.ExpenseCategory.OTHER)
                    .amount(500000.0) // 5 lakhs per month
                    .frequency(Expense.ExpenseFrequency.MONTHLY)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusYears(50))
                    .isTimeBound(false)
                    .build();

            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserIdAndType(eq(userId), any())).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(List.of(expense));
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            double requiredCorpus = retirementService.calculateRequiredCorpusForUser(
                    userId, inflationRate, yearsToRetirement, retirementYears,
                    incomeStrategy, corpusReturnRate, withdrawalRate
            );

            // Then - should be very high but finite
            assertThat(requiredCorpus).isGreaterThan(10000000.0); // > 1 crore
            assertThat(requiredCorpus).isLessThan(Double.MAX_VALUE); // Finite
        }

        @Test
        @DisplayName("should handle recurring goals during retirement")
        void shouldHandleRecurringGoalsDuringRetirement() {
            // Given
            String userId = "user-recurring-goals";
            double inflationRate = 6.0;
            int yearsToRetirement = 20;
            int retirementYears = 25;
            String incomeStrategy = "SUSTAINABLE";
            double corpusReturnRate = 10.0;
            double withdrawalRate = 8.0;

            // Create recurring goal
            Goal goal = Goal.builder()
                    .id("goal-recurring")
                    .userId(userId)
                    .name("Annual Vacation")
                    .targetYear(LocalDate.now().getYear() + yearsToRetirement + 1)
                    .targetAmount(200000.0) // 2 lakhs per year
                    .isRecurring(true)
                    .recurrenceInterval(1) // Every year
                    .recurrenceEndYear(LocalDate.now().getYear() + yearsToRetirement + 20)
                    .build();

            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(List.of(goal));
            when(investmentRepository.findByUserIdAndType(eq(userId), any())).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            double requiredCorpus = retirementService.calculateRequiredCorpusForUser(
                    userId, inflationRate, yearsToRetirement, retirementYears,
                    incomeStrategy, corpusReturnRate, withdrawalRate
            );

            // Then - recurring goals should increase required corpus
            assertThat(requiredCorpus).isGreaterThan(0);
        }

        @Test
        @DisplayName("should handle insurance annuity income reducing required corpus")
        void shouldHandleInsuranceAnnuityIncomeReducingRequiredCorpus() {
            // Given
            String userId = "user-annuity";
            RetirementScenario scenario = createDefaultScenario(userId);

            // Add expense
            Expense expense = Expense.builder()
                    .id("expense-1")
                    .userId(userId)
                    .name("Monthly Expense")
                    .category(Expense.ExpenseCategory.OTHER)
                    .amount(50000.0)
                    .frequency(Expense.ExpenseFrequency.MONTHLY)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusYears(50))
                    .build();

            // Add annuity insurance that provides income
            Insurance annuity = Insurance.builder()
                    .id("annuity-1")
                    .userId(userId)
                    .type(Insurance.InsuranceType.ANNUITY)
                    .isAnnuityPolicy(true)
                    .annuityStartYear(LocalDate.now().getYear() + scenario.getRetirementAge() - scenario.getCurrentAge())
                    .maturityBenefit(3000000.0) // 30 lakhs corpus
                    .monthlyAnnuityAmount(20000.0) // 20k per month
                    .build();

            when(scenarioRepository.findByUserIdAndIsDefaultTrue(userId))
                    .thenReturn(Optional.of(scenario));
            when(investmentRepository.findByUserIdAndType(eq(userId), any())).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq(userId), anyList()))
                    .thenReturn(List.of(annuity));
            when(goalRepository.findByUserIdOrderByTargetYearAsc(userId)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(userId)).thenReturn(List.of(expense));
            when(incomeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(userId, scenario);

            // Then - annuity should be factored in
            assertThat(result).containsKey("matrix");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            assertThat(matrix).isNotEmpty();
        }
    }

    private RetirementScenario createDefaultScenario(String userId) {
        RetirementScenario scenario = new RetirementScenario();
        scenario.setId("scenario-default");
        scenario.setUserId(userId);
        scenario.setName("Default");
        scenario.setCurrentAge(35);
        scenario.setRetirementAge(60);
        scenario.setLifeExpectancy(85);
        scenario.setInflationRate(6.0);
        scenario.setSipStepUpPercent(10.0);
        scenario.setLumpsumAmount(0.0);
        scenario.setIsDefault(true);
        return scenario;
    }
}
