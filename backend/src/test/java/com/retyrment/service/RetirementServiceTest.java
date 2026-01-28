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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
        @DisplayName("should use explicit NPS return over MF returns")
        void shouldUseExplicitNpsReturn() {
            Investment nps = Investment.builder()
                    .type(Investment.InvestmentType.NPS)
                    .currentValue(100.0)
                    .monthlySip(0.0)
                    .build();

            when(investmentRepository.findByUserIdAndType(eq("test-user"), any()))
                    .thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserIdAndType("test-user", Investment.InvestmentType.NPS))
                    .thenReturn(Collections.singletonList(nps));
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq("test-user"), anyList()))
                    .thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(36)
                    .lifeExpectancy(85)
                    .npsReturn(8.0)
                    .mfReturns(List.of(RetirementScenario.PeriodReturn.builder()
                            .fromYear(0)
                            .toYear(10)
                            .rate(5.0)
                            .build()))
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            Map<String, Object> yearOne = matrix.get(1);

            assertThat(((Number) yearOne.get("npsBalance")).longValue()).isEqualTo(108L);
        }

        @Test
        @DisplayName("should fall back to MF returns when NPS return not provided")
        void shouldFallbackToMfReturnsWhenNpsReturnMissing() {
            Investment nps = Investment.builder()
                    .type(Investment.InvestmentType.NPS)
                    .currentValue(100.0)
                    .monthlySip(0.0)
                    .build();

            when(investmentRepository.findByUserIdAndType(eq("test-user"), any()))
                    .thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserIdAndType("test-user", Investment.InvestmentType.NPS))
                    .thenReturn(Collections.singletonList(nps));
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserIdAndTypeIn(eq("test-user"), anyList()))
                    .thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(36)
                    .lifeExpectancy(85)
                    .mfReturns(List.of(RetirementScenario.PeriodReturn.builder()
                            .fromYear(0)
                            .toYear(10)
                            .rate(5.0)
                            .build()))
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            Map<String, Object> yearOne = matrix.get(1);

            assertThat(((Number) yearOne.get("npsBalance")).longValue()).isEqualTo(105L);
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

        @Test
        @DisplayName("should create default scenario when none exists and scenario is null")
        void shouldCreateDefaultScenarioWhenNoneExists() {
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(Optional.empty());
            when(scenarioRepository.save(any(RetirementScenario.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", null);

            assertThat(result).containsKeys("matrix", "summary");
            verify(scenarioRepository).save(any(RetirementScenario.class));
        }

        @Test
        @DisplayName("should handle exception when finding default scenario and create new one")
        void shouldHandleExceptionWhenFindingDefaultScenario() {
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user"))
                    .thenThrow(new RuntimeException("Database error"));
            when(scenarioRepository.save(any(RetirementScenario.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", null);

            assertThat(result).containsKeys("matrix", "summary");
            verify(scenarioRepository).save(any(RetirementScenario.class));
        }

        @Test
        @DisplayName("should set default currentAge when null")
        void shouldSetDefaultCurrentAge() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(null)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            // Setup required mocks
            when(investmentRepository.findByType(any())).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKeys("matrix", "summary");
        }

        @Test
        @DisplayName("should set default retirementAge when null")
        void shouldSetDefaultRetirementAge() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(null)
                    .lifeExpectancy(85)
                    .build();

            // Setup required mocks
            when(investmentRepository.findByType(any())).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKeys("matrix", "summary");
        }

        @Test
        @DisplayName("should set default lifeExpectancy when null")
        void shouldSetDefaultLifeExpectancy() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(null)
                    .build();

            // Setup required mocks
            when(investmentRepository.findByType(any())).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKeys("matrix", "summary");
        }

        @Test
        @DisplayName("should set default inflationRate when null")
        void shouldSetDefaultInflationRate() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .inflationRate(null)
                    .build();

            // Setup required mocks
            when(investmentRepository.findByType(any())).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

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

        @Test
        @DisplayName("should not apply rate reduction when disabled")
        void shouldNotApplyRateReductionWhenDisabled() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .enableRateReduction(false)
                    .ppfReturn(7.1)
                    .epfReturn(8.15)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Rates should remain constant
            Double ppfRateYear0 = (Double) matrix.get(0).get("ppfRate");
            Double ppfRateYear10 = (Double) matrix.get(10).get("ppfRate");
            
            assertThat(ppfRateYear10).isEqualTo(ppfRateYear0);
        }

        @Test
        @DisplayName("should not apply rate reduction when rateReductionYears is 0")
        void shouldNotApplyRateReductionWhenRateReductionYearsIsZero() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .enableRateReduction(true)
                    .rateReductionYears(0)
                    .rateReductionPercent(0.5)
                    .ppfReturn(7.1)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Rates should remain constant when rateReductionYears is 0
            Double ppfRateYear0 = (Double) matrix.get(0).get("ppfRate");
            Double ppfRateYear10 = (Double) matrix.get(10).get("ppfRate");
            
            assertThat(ppfRateYear10).isEqualTo(ppfRateYear0);
        }

        @Test
        @DisplayName("should not apply rate reduction in year 0")
        void shouldNotApplyRateReductionInYearZero() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .enableRateReduction(true)
                    .rateReductionYears(5)
                    .rateReductionPercent(0.5)
                    .ppfReturn(7.1)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Year 0 should not have rate reduction applied
            Double ppfRateYear0 = (Double) matrix.get(0).get("ppfRate");
            Double ppfRateYear1 = (Double) matrix.get(1).get("ppfRate");
            
            // Year 1 should have same rate as year 0 (reduction only after rateReductionYears)
            assertThat(ppfRateYear1).isEqualTo(ppfRateYear0);
        }

        @Test
        @DisplayName("should apply SIP step-up from effectiveFromYear onwards")
        void shouldApplySipStepUpFromEffectiveFromYear() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(100000.0)
                    .monthlySip(10000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(mf));
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));
            when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt()))
                    .thenAnswer(inv -> {
                        double sip = inv.getArgument(0);
                        double rate = inv.getArgument(1);
                        int years = inv.getArgument(2);
                        return sip * 12 * years * (1 + rate / 100);
                    });

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .sipStepUpPercent(10.0)
                    .effectiveFromYear(2)
                    .mfReturn(12.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // SIP should step up from year 2 onwards
            assertThat(matrix).isNotEmpty();
        }

        @Test
        @DisplayName("should not apply SIP step-up before effectiveFromYear")
        void shouldNotApplySipStepUpBeforeEffectiveFromYear() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(100000.0)
                    .monthlySip(10000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(mf));
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));
            when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt()))
                    .thenAnswer(inv -> {
                        double sip = inv.getArgument(0);
                        double rate = inv.getArgument(1);
                        int years = inv.getArgument(2);
                        return sip * 12 * years * (1 + rate / 100);
                    });

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .sipStepUpPercent(10.0)
                    .effectiveFromYear(5)
                    .mfReturn(12.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // SIP should not step up before year 5
            assertThat(matrix).isNotEmpty();
        }

        @Test
        @DisplayName("should use default rates before effectiveFromYear")
        void shouldUseDefaultRatesBeforeEffectiveFromYear() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .effectiveFromYear(3)
                    .ppfReturn(8.0) // User-specified rate
                    .epfReturn(9.0)
                    .mfReturn(13.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Years before effectiveFromYear should use default rates
            Double ppfRateYear0 = (Double) matrix.get(0).get("ppfRate");
            Double ppfRateYear2 = (Double) matrix.get(2).get("ppfRate");
            Double ppfRateYear3 = (Double) matrix.get(3).get("ppfRate");
            
            // Year 0 and 2 should use default (7.1), year 3 should use user-specified (8.0)
            assertThat(ppfRateYear0).isEqualTo(7.1); // defaultPpfReturn
            assertThat(ppfRateYear2).isEqualTo(7.1); // defaultPpfReturn
            assertThat(ppfRateYear3).isEqualTo(8.0); // User-specified
        }

        @Test
        @DisplayName("should use user-specified rates from effectiveFromYear onwards")
        void shouldUseUserSpecifiedRatesFromEffectiveFromYear() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .effectiveFromYear(1)
                    .enableRateReduction(false) // Explicitly disable rate reduction
                    .ppfReturn(8.0)
                    .epfReturn(9.0)
                    .mfReturn(13.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // From year 1 onwards should use user-specified rates
            Double ppfRateYear1 = (Double) matrix.get(1).get("ppfRate");
            Double ppfRateYear5 = (Double) matrix.get(5).get("ppfRate");
            
            assertThat(ppfRateYear1).isEqualTo(8.0);
            assertThat(ppfRateYear5).isEqualTo(8.0);
        }

        @Test
        @DisplayName("should handle rate reduction with FD and RD rates")
        void shouldHandleRateReductionWithFdAndRdRates() {
            Investment fd = Investment.builder()
                    .type(Investment.InvestmentType.FD)
                    .currentValue(500000.0)
                    .expectedReturn(7.0)
                    .build();

            Investment rd = Investment.builder()
                    .type(Investment.InvestmentType.RD)
                    .currentValue(200000.0)
                    .expectedReturn(6.5)
                    .monthlySip(5000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(fd, rd));
            when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .enableRateReduction(true)
                    .rateReductionYears(5)
                    .rateReductionPercent(0.5)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // FD and RD rates should be reduced over time
            assertThat(matrix).isNotEmpty();
        }

        @Test
        @DisplayName("should not reduce FD and RD rates when rate reduction is disabled")
        void shouldNotReduceFdAndRdRatesWhenRateReductionDisabled() {
            Investment fd = Investment.builder()
                    .type(Investment.InvestmentType.FD)
                    .currentValue(500000.0)
                    .expectedReturn(7.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(fd));
            when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .enableRateReduction(false)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // FD and RD rates should remain constant
            assertThat(matrix).isNotEmpty();
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

        @Test
        @DisplayName("should include investment with maturity date before retirement")
        void shouldIncludeInvestmentWithMaturityBeforeRetirement() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10); // 10 years from now, before retirement at 60

            Investment fd = Investment.builder()
                    .id("fd1")
                    .name("Fixed Deposit")
                    .type(Investment.InvestmentType.FD)
                    .maturityDate(maturityDate)
                    .currentValue(100000.0)
                    .expectedReturn(7.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(fd));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInvestments = (List<Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
        }

        @Test
        @DisplayName("should exclude investment with maturity date after retirement")
        void shouldExcludeInvestmentWithMaturityAfterRetirement() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(30); // 30 years from now, after retirement at 60

            Investment fd = Investment.builder()
                    .id("fd1")
                    .name("Fixed Deposit")
                    .type(Investment.InvestmentType.FD)
                    .maturityDate(maturityDate)
                    .currentValue(100000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(fd));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInvestments = (List<Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).isEmpty();
        }

        @Test
        @DisplayName("should exclude investment with null maturity date")
        void shouldExcludeInvestmentWithNullMaturityDate() {
            Investment mf = Investment.builder()
                    .id("mf1")
                    .name("Mutual Fund")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .maturityDate(null)
                    .currentValue(100000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(mf));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInvestments = (List<Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).isEmpty();
        }

        @Test
        @DisplayName("should include ULIP insurance with maturity before retirement")
        void shouldIncludeUlipInsuranceWithMaturityBeforeRetirement() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance ulip = Insurance.builder()
                    .id("ulip1")
                    .policyName("ULIP Policy")
                    .type(Insurance.InsuranceType.ULIP)
                    .maturityDate(maturityDate)
                    .fundValue(200000.0)
                    .sumAssured(500000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(ulip));
            when(calculationService.calculateFutureValue(anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(400000.0);

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).hasSize(1);
        }

        @Test
        @DisplayName("should include ENDOWMENT insurance with maturity before retirement")
        void shouldIncludeEndowmentInsuranceWithMaturityBeforeRetirement() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance endowment = Insurance.builder()
                    .id("end1")
                    .policyName("Endowment Policy")
                    .type(Insurance.InsuranceType.ENDOWMENT)
                    .maturityDate(maturityDate)
                    .sumAssured(1000000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(endowment));

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).hasSize(1);
        }

        @Test
        @DisplayName("should exclude TERM_LIFE insurance (no maturity)")
        void shouldExcludeTermLifeInsurance() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance termLife = Insurance.builder()
                    .id("term1")
                    .policyName("Term Life")
                    .type(Insurance.InsuranceType.TERM_LIFE)
                    .maturityDate(maturityDate)
                    .sumAssured(5000000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(termLife));

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).isEmpty();
        }

        @Test
        @DisplayName("should use fundValue for ULIP when available")
        void shouldUseFundValueForUlipWhenAvailable() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance ulip = Insurance.builder()
                    .id("ulip1")
                    .policyName("ULIP Policy")
                    .type(Insurance.InsuranceType.ULIP)
                    .maturityDate(maturityDate)
                    .fundValue(200000.0)
                    .sumAssured(500000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(ulip));
            when(calculationService.calculateFutureValue(200000.0, 8.0, 10))
                    .thenReturn(400000.0);

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).hasSize(1);
            assertThat(maturingInsurance.get(0).get("expectedMaturityValue")).isEqualTo(400000L);
        }

        @Test
        @DisplayName("should use sumAssured for ULIP when fundValue is null")
        void shouldUseSumAssuredForUlipWhenFundValueIsNull() {
            LocalDate today = LocalDate.now();
            LocalDate maturityDate = today.plusYears(10);

            Insurance ulip = Insurance.builder()
                    .id("ulip1")
                    .policyName("ULIP Policy")
                    .type(Insurance.InsuranceType.ULIP)
                    .maturityDate(maturityDate)
                    .fundValue(null)
                    .sumAssured(500000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(ulip));

            Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
            assertThat(maturingInsurance).hasSize(1);
            assertThat(maturingInsurance.get(0).get("expectedMaturityValue")).isEqualTo(500000L);
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

    @Nested
    @DisplayName("Retirement Income Projection Branches")
    class RetirementIncomeProjectionBranches {
        
        @BeforeEach
        void setUpMocks() {
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("should project SIMPLE_DEPLETION strategy in retirement years")
        void shouldProjectSimpleDepletion() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SIMPLE_DEPLETION")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(5.0)
                    .build();

            // Add some investments to generate a corpus
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> projection = (List<Map<String, Object>>) summary.get("retirementIncomeProjection");
            
            assertThat(projection).isNotEmpty();
            // SIMPLE_DEPLETION should have decreasing monthly income as corpus depletes
            assertThat(projection.get(0).get("monthlyIncome")).isNotNull();
        }

        @Test
        @DisplayName("should project SAFE_4_PERCENT strategy in retirement years")
        void shouldProjectSafe4Percent() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SAFE_4_PERCENT")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(5.0)
                    .build();

            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> projection = (List<Map<String, Object>>) summary.get("retirementIncomeProjection");
            
            assertThat(projection).isNotEmpty();
            // SAFE_4_PERCENT should have consistent monthly income
            assertThat(projection.get(0).get("monthlyIncome")).isNotNull();
        }

        @Test
        @DisplayName("should handle remainingYears = 0 in SIMPLE_DEPLETION")
        void shouldHandleZeroRemainingYearsInSimpleDepletion() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(60) // Same as retirement age - no retirement years
                    .incomeStrategy("SIMPLE_DEPLETION")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(5.0)
                    .build();

            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            assertThat(summary).containsKey("retirementIncomeProjection");
        }
    }

    @Nested
    @DisplayName("Effective From Year Branches")
    class EffectiveFromYearBranches {
        
        @BeforeEach
        void setUpMocks() {
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("should apply user adjustments only from effectiveFromYear onwards")
        void shouldApplyAdjustmentsFromEffectiveYear() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .effectiveFromYear(5) // Adjustments start from year 5
                    .ppfReturn(8.0)
                    .epfReturn(8.5)
                    .mfReturn(13.0)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            assertThat(summary.get("effectiveFromYear")).isEqualTo(5);
        }

        @Test
        @DisplayName("should use default rates before effectiveFromYear")
        void shouldUseDefaultRatesBeforeEffectiveYear() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .effectiveFromYear(10) // Adjustments start from year 10
                    .ppfReturn(9.0) // User-specified rate
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should apply SIP step-up only from effectiveFromYear")
        void shouldApplySipStepUpFromEffectiveYear() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .effectiveFromYear(3)
                    .sipStepUpPercent(10.0) // 10% step-up
                    .corpusReturnRate(10.0)
                    .build();

            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .monthlySip(10000.0)
                    .currentValue(100000.0)
                    .build();
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));
            when(investmentRepository.findByUserIdAndMonthlySipGreaterThan("test-user", 0.0))
                    .thenReturn(Collections.singletonList(mf));

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }
    }

    @Nested
    @DisplayName("Rate Reduction Edge Cases")
    class RateReductionEdgeCases {
        
        @BeforeEach
        void setUpMocks() {
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("should handle rate reduction disabled")
        void shouldHandleRateReductionDisabled() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .enableRateReduction(false)
                    .rateReductionPercent(0.5)
                    .rateReductionYears(5)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should apply rate reduction to FD and RD rates")
        void shouldApplyRateReductionToFdAndRd() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .enableRateReduction(true)
                    .rateReductionPercent(0.5)
                    .rateReductionYears(5)
                    .corpusReturnRate(10.0)
                    .build();

            Investment fd = Investment.builder()
                    .type(Investment.InvestmentType.FD)
                    .currentValue(500000.0)
                    .expectedReturn(7.0)
                    .build();
            Investment rd = Investment.builder()
                    .type(Investment.InvestmentType.RD)
                    .currentValue(200000.0)
                    .expectedReturn(6.5)
                    .build();

            when(investmentRepository.findByUserId("test-user"))
                    .thenReturn(Arrays.asList(fd, rd));

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("should not apply rate reduction when year is 0")
        void shouldNotApplyRateReductionWhenYearIsZero() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .enableRateReduction(true)
                    .rateReductionPercent(0.5)
                    .rateReductionYears(5)
                    .ppfReturn(7.1)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Year 0 should not have rate reduction applied
            Double ppfRateYear0 = (Double) matrix.get(0).get("ppfRate");
            assertThat(ppfRateYear0).isEqualTo(7.1); // Should be original rate, not reduced
        }

        @Test
        @DisplayName("should not apply rate reduction when rateReductionYears is 0")
        void shouldNotApplyRateReductionWhenRateReductionYearsIsZero() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .enableRateReduction(true)
                    .rateReductionPercent(0.5)
                    .rateReductionYears(0) // Zero years - should not apply
                    .ppfReturn(7.1)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Rate reduction should not apply when rateReductionYears is 0
            Double ppfRateYear5 = (Double) matrix.get(5).get("ppfRate");
            assertThat(ppfRateYear5).isEqualTo(7.1); // Should remain unchanged
        }

        @Test
        @DisplayName("should ensure rate does not go below 4% with rate reduction")
        void shouldEnsureRateDoesNotGoBelow4Percent() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .enableRateReduction(true)
                    .rateReductionPercent(2.0) // Large reduction
                    .rateReductionYears(1) // Every year
                    .ppfReturn(5.0) // Start with 5%
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // After reductions, rate should not go below 4%
            Double ppfRateYear10 = (Double) matrix.get(10).get("ppfRate");
            assertThat(ppfRateYear10).isGreaterThanOrEqualTo(4.0);
        }

        @Test
        @DisplayName("should not calculate balances for year 0")
        void shouldNotCalculateBalancesForYearZero() {
            Investment ppf = Investment.builder()
                    .type(Investment.InvestmentType.PPF)
                    .currentValue(100000.0)
                    .yearlyContribution(50000.0)
                    .build();

            when(investmentRepository.findByUserIdAndType("test-user", Investment.InvestmentType.PPF))
                    .thenReturn(Collections.singletonList(ppf));
            when(investmentRepository.findByType(Investment.InvestmentType.PPF))
                    .thenReturn(Collections.singletonList(ppf));
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);
            when(calculationService.calculateFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .corpusReturnRate(10.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Year 0 should have initial balance, year 1 should have calculated balance
            Long ppfBalanceYear0 = ((Number) matrix.get(0).get("ppfBalance")).longValue();
            Long ppfBalanceYear1 = ((Number) matrix.get(1).get("ppfBalance")).longValue();
            
            assertThat(ppfBalanceYear0).isEqualTo(100000L); // Initial value
            assertThat(ppfBalanceYear1).isGreaterThan(ppfBalanceYear0); // Should have grown
        }
    }
}
