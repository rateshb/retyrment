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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetirementServiceTest {

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
        // Inject default values using reflection
        ReflectionTestUtils.setField(retirementService, "defaultEpfReturn", 8.15);
        ReflectionTestUtils.setField(retirementService, "defaultPpfReturn", 7.1);
        ReflectionTestUtils.setField(retirementService, "defaultMfReturn", 12.0);
        ReflectionTestUtils.setField(retirementService, "calculationService", calculationService);
    }

    @Nested
    @DisplayName("generateRetirementMatrix")
    class GenerateRetirementMatrix {

        @BeforeEach
        void setUpMocks() {
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("should calculate matrix with default parameters")
        void shouldCalculateWithDefaults() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKeys("matrix", "summary");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            // Should have entries for each year from current age to retirement
            assertThat(matrix).hasSize(26); // 25 years + 1 for current year
        }

        @Test
        @DisplayName("should include investments in calculation")
        void shouldIncludeInvestments() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .investedAmount(100000.0)
                    .currentValue(120000.0)
                    .monthlySip(10000.0)
                    .build();

            Investment ppf = Investment.builder()
                    .type(Investment.InvestmentType.PPF)
                    .currentValue(500000.0)
                    .yearlyContribution(150000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Arrays.asList(mf));
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Arrays.asList(ppf));
            
            // Mock the calculation service for SIP
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenReturn(130000.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenAnswer(invocation -> {
                        double amount = invocation.getArgument(0);
                        double rate = invocation.getArgument(1);
                        int years = invocation.getArgument(2);
                        return amount * Math.pow(1 + rate / 100, years);
                    });

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            
            // Final corpus should be positive
            Long finalCorpus = (Long) summary.get("finalCorpus");
            assertThat(finalCorpus).isPositive();
        }

        @Test
        @DisplayName("should calculate goal outflows correctly")
        void shouldCalculateGoalOutflows() {
            Goal goal = Goal.builder()
                    .name("Child Education")
                    .targetAmount(2500000.0)
                    .targetYear(java.time.LocalDate.now().getYear() + 5)
                    .build();

            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Arrays.asList(goal));
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenAnswer(invocation -> {
                        double amount = invocation.getArgument(0);
                        double rate = invocation.getArgument(1);
                        int years = invocation.getArgument(2);
                        return amount * Math.pow(1 + rate / 100, years);
                    });
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Check that some year has a goal outflow
            boolean hasGoalOutflow = matrix.stream()
                    .anyMatch(row -> {
                        Object outflow = row.get("goalOutflow");
                        return outflow != null && ((Number) outflow).longValue() > 0;
                    });
            
            assertThat(hasGoalOutflow).isTrue();
        }
        
        @Test
        @DisplayName("should handle null scenario with defaults")
        void shouldHandleNullScenario() {
            RetirementScenario defaultScenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .isDefault(true)
                    .build();
            
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(Optional.of(defaultScenario));

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", null);

            assertThat(result).containsKeys("matrix", "summary");
        }
    }

    @Nested
    @DisplayName("Rate Reduction Feature")
    class RateReduction {

        @BeforeEach
        void setUpMocks() {
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(Optional.empty());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenReturn(0.0);
        }

        @Test
        @DisplayName("should apply rate reduction when enabled")
        void shouldApplyRateReduction() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .enableRateReduction(true)
                    .rateReductionYears(5)
                    .rateReductionPercent(0.5)
                    .ppfReturn(7.1)
                    .epfReturn(8.15)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Check that PPF rate decreases after 5 years
            Double ppfRateYear0 = (Double) matrix.get(0).get("ppfRate");
            Double ppfRateYear5 = (Double) matrix.get(5).get("ppfRate");
            Double ppfRateYear10 = (Double) matrix.get(10).get("ppfRate");
            
            // Rate should decrease over time
            assertThat(ppfRateYear5).isLessThanOrEqualTo(ppfRateYear0);
            assertThat(ppfRateYear10).isLessThanOrEqualTo(ppfRateYear5);
        }

        @Test
        @DisplayName("should not go below minimum rate")
        void shouldNotGoBelowMinRate() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .enableRateReduction(true)
                    .rateReductionYears(1) // Aggressive reduction
                    .rateReductionPercent(2.0) // Large reduction
                    .ppfReturn(7.1)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Check that rate never goes below 4%
            for (Map<String, Object> row : matrix) {
                Double ppfRate = (Double) row.get("ppfRate");
                assertThat(ppfRate).isGreaterThanOrEqualTo(4.0);
            }
        }
    }

    @Nested
    @DisplayName("calculateMaturingBeforeRetirement")
    class CalculateMaturingBeforeRetirement {
        @BeforeEach
        void setUpMocks() {
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("should calculate maturing investments")
        void shouldCalculateMaturingInvestments() {
            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            assertThat(result).containsKeys("totalMaturingBeforeRetirement", "maturingInvestments", "maturingInsurance");
            assertThat(result.get("totalMaturingBeforeRetirement")).isNotNull();
            assertThat(result.get("maturingInvestments")).isInstanceOf(List.class);
            assertThat(result.get("maturingInsurance")).isInstanceOf(List.class);
        }

        @Test
        @DisplayName("should return zero when no maturing investments")
        void shouldReturnZeroWhenNoMaturingInvestments() {
            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            assertThat(result).containsKey("totalMaturingBeforeRetirement");
            Object totalValue = result.get("totalMaturingBeforeRetirement");
            // Should be Long from Math.round()
            assertThat(totalValue).isInstanceOf(Long.class);
            assertThat((Long) totalValue).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Income Strategies")
    class IncomeStrategies {
        @BeforeEach
        void setUpMocks() {
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(Optional.empty());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
        }

        @Test
        @DisplayName("should use SUSTAINABLE income strategy")
        void shouldUseSustainableIncomeStrategy() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            assertThat(summary).containsKey("incomeStrategy");
        }

        @Test
        @DisplayName("should use SAFE_4_PERCENT income strategy")
        void shouldUseSafe4PercentIncomeStrategy() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .incomeStrategy("SAFE_4_PERCENT")
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should use SIMPLE_DEPLETION income strategy")
        void shouldUseSimpleDepletionIncomeStrategy() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .incomeStrategy("SIMPLE_DEPLETION")
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle null income strategy with default")
        void shouldHandleNullIncomeStrategy() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .incomeStrategy(null)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle invalid income strategy")
        void shouldHandleInvalidIncomeStrategy() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .incomeStrategy("INVALID_STRATEGY")
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCases {
        @BeforeEach
        void setUpMocks() {
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("should handle zero retirement age")
        void shouldHandleZeroRetirementAge() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(60)
                    .retirementAge(60)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle negative current age")
        void shouldHandleNegativeCurrentAge() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(-10)
                    .retirementAge(60)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle null return rates")
        void shouldHandleNullReturnRates() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .epfReturn(null)
                    .ppfReturn(null)
                    .mfReturn(null)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle rate reduction with zero years")
        void shouldHandleRateReductionWithZeroYears() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .enableRateReduction(true)
                    .rateReductionPercent(0.5)
                    .rateReductionYears(0)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should handle empty maturing investments")
        void shouldHandleEmptyMaturingInvestments() {
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            assertThat(result).containsKey("totalMaturingBeforeRetirement");
            assertThat(result.get("totalMaturingBeforeRetirement")).isInstanceOf(Long.class);
            assertThat((Long) result.get("totalMaturingBeforeRetirement")).isEqualTo(0L);
        }
    }
}
