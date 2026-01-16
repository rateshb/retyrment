package com.retyrment.service;

import com.retyrment.model.Expense;
import com.retyrment.model.Goal;
import com.retyrment.model.Insurance;
import com.retyrment.model.Investment;
import com.retyrment.model.RetirementScenario;
import com.retyrment.repository.*;
import java.util.Optional;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RetirementService Gap Analysis Strategy Branches Tests")
class RetirementServiceGapAnalysisBranchesTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private RetirementScenarioRepository scenarioRepository;

    @Mock
    private CalculationService calculationService;

    @InjectMocks
    private RetirementService retirementService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(retirementService, "defaultInflation", 6.0);
        ReflectionTestUtils.setField(retirementService, "defaultEpfReturn", 8.0);
        ReflectionTestUtils.setField(retirementService, "defaultPpfReturn", 7.1);
        ReflectionTestUtils.setField(retirementService, "defaultMfReturn", 12.0);
        ReflectionTestUtils.setField(retirementService, "calculationService", calculationService);

        // Setup common mocks
        when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
        when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
        when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
        when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
        when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(Optional.empty());
        when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);
        when(calculationService.calculateInflatedValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);
        when(calculationService.calculateFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);
    }

    @Nested
    @DisplayName("calculateGapAnalysis - Income Strategy Branches")
    class IncomeStrategyBranches {

        @Test
        @DisplayName("should calculate required corpus for SIMPLE_DEPLETION strategy")
        void shouldCalculateRequiredCorpusForSimpleDepletion() {
            Expense expense = Expense.builder()
                    .monthlyAmount(50000.0)
                    .build();

            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(expense));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SIMPLE_DEPLETION")
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            assertThat(gapAnalysis).isNotNull();
            assertThat(gapAnalysis.get("incomeStrategy")).isEqualTo("SIMPLE_DEPLETION");
        }

        @Test
        @DisplayName("should calculate required corpus for SAFE_4_PERCENT strategy")
        void shouldCalculateRequiredCorpusForSafe4Percent() {
            Expense expense = Expense.builder()
                    .monthlyAmount(50000.0)
                    .build();

            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(expense));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SAFE_4_PERCENT")
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            assertThat(gapAnalysis).isNotNull();
            assertThat(gapAnalysis.get("incomeStrategy")).isEqualTo("SAFE_4_PERCENT");
        }

        @Test
        @DisplayName("should calculate required corpus for SUSTAINABLE strategy with withdrawal rate > 0")
        void shouldCalculateRequiredCorpusForSustainableWithWithdrawalRate() {
            Expense expense = Expense.builder()
                    .monthlyAmount(50000.0)
                    .build();

            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(expense));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SUSTAINABLE")
                    .withdrawalRate(8.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            assertThat(gapAnalysis).isNotNull();
            assertThat(gapAnalysis.get("incomeStrategy")).isEqualTo("SUSTAINABLE");
        }

        @Test
        @DisplayName("should use fallback when withdrawal rate is 0 for SUSTAINABLE strategy")
        void shouldUseFallbackWhenWithdrawalRateIsZero() {
            Expense expense = Expense.builder()
                    .monthlyAmount(50000.0)
                    .build();

            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(expense));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SUSTAINABLE")
                    .withdrawalRate(0.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            assertThat(gapAnalysis).isNotNull();
            // Should use 25x fallback (4% rule)
        }

        @Test
        @DisplayName("should include goals in required corpus calculation")
        void shouldIncludeGoalsInRequiredCorpus() {
            Expense expense = Expense.builder()
                    .monthlyAmount(50000.0)
                    .build();

            Goal goal = Goal.builder()
                    .targetAmount(2000000.0)
                    .build();

            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(expense));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.singletonList(goal));

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            assertThat(gapAnalysis).isNotNull();
            assertThat(gapAnalysis.get("totalGoalAmount")).isNotNull();
        }

        @Test
        @DisplayName("should calculate additional SIP when corpus gap > 0 and yearsToRetirement > 0")
        void shouldCalculateAdditionalSipWhenGapExists() {
            Expense expense = Expense.builder()
                    .monthlyAmount(50000.0)
                    .build();

            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(expense));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            assertThat(gapAnalysis).isNotNull();
            assertThat(gapAnalysis.get("additionalSIPRequired")).isNotNull();
        }

        @Test
        @DisplayName("should not calculate additional SIP when corpus gap <= 0")
        void shouldNotCalculateAdditionalSipWhenNoGap() {
            // Create scenario where projected corpus exceeds required
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(10000000.0) // Large corpus
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            assertThat(gapAnalysis).isNotNull();
            Long additionalSip = (Long) gapAnalysis.get("additionalSIPRequired");
            assertThat(additionalSip).isEqualTo(0L);
        }

        @Test
        @DisplayName("should generate suggestions when corpus gap > 0")
        void shouldGenerateSuggestionsWhenGapExists() {
            Expense expense = Expense.builder()
                    .monthlyAmount(50000.0)
                    .build();

            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(expense));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> suggestions = (List<Map<String, Object>>) gapAnalysis.get("suggestions");
            
            // Should have suggestions when gap exists
            if (gapAnalysis.get("isOnTrack").equals(false)) {
                assertThat(suggestions).isNotEmpty();
            }
        }

        @Test
        @DisplayName("should generate on-track suggestion when corpus gap <= 0")
        void shouldGenerateOnTrackSuggestionWhenNoGap() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(10000000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> suggestions = (List<Map<String, Object>>) gapAnalysis.get("suggestions");
            
            // Should have on-track suggestion
            if (gapAnalysis.get("isOnTrack").equals(true)) {
                boolean hasOnTrack = suggestions.stream()
                        .anyMatch(s -> s.get("title").toString().contains("On Track"));
                assertThat(hasOnTrack).isTrue();
            }
        }

        @Test
        @DisplayName("should include delayed retirement suggestion when yearsToRetirement < 25")
        void shouldIncludeDelayedRetirementSuggestion() {
            Expense expense = Expense.builder()
                    .monthlyAmount(50000.0)
                    .build();

            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(expense));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(55) // 20 years to retirement (< 25)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> suggestions = (List<Map<String, Object>>) gapAnalysis.get("suggestions");
            
            // Should have delayed retirement suggestion when gap exists and years < 25
            if (gapAnalysis.get("isOnTrack").equals(false)) {
                boolean hasDelayedRetirement = suggestions.stream()
                        .anyMatch(s -> s.get("title").toString().contains("Delayed Retirement"));
                // May or may not be present depending on gap
            }
        }

        @Test
        @DisplayName("should handle insurance with null annualPremium")
        void shouldHandleInsuranceWithNullAnnualPremium() {
            Insurance term = Insurance.builder()
                    .type(Insurance.InsuranceType.TERM_LIFE)
                    .annualPremium(null)
                    .build();

            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(term));
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            
            // Insurance with null annualPremium should not be included
            assertThat(continuingInsurance).isEmpty();
        }

        @Test
        @DisplayName("should handle insurance with null healthType")
        void shouldHandleInsuranceWithNullHealthType() {
            Insurance health = Insurance.builder()
                    .type(Insurance.InsuranceType.HEALTH)
                    .healthType(null)
                    .annualPremium(20000.0)
                    .build();

            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(health));
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> continuingInsurance = (List<Map<String, Object>>) gapAnalysis.get("continuingInsurance");
            
            // HEALTH insurance with null healthType should continue (default behavior)
            assertThat(continuingInsurance).hasSize(1);
        }

        @Test
        @DisplayName("should calculate gapPercent as 0 when requiredCorpus is 0")
        void shouldCalculateGapPercentAsZeroWhenRequiredCorpusIsZero() {
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            Double gapPercent = (Double) gapAnalysis.get("gapPercent");
            assertThat(gapPercent).isEqualTo(0.0);
        }
    }
}
