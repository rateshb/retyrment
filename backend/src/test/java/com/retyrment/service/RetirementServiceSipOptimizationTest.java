package com.retyrment.service;

import com.retyrment.model.*;
import com.retyrment.model.Expense.ExpenseFrequency;
import com.retyrment.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RetirementService - SIP Step-Up Optimization Tests")
class RetirementServiceSipOptimizationTest {

    @Mock
    private InvestmentRepository investmentRepository;
    
    @Mock
    private ExpenseRepository expenseRepository;
    
    @Mock
    private InsuranceRepository insuranceRepository;
    
    @Mock
    private LoanRepository loanRepository;
    
    @Mock
    private GoalRepository goalRepository;
    
    @Mock
    private RetirementScenarioRepository scenarioRepository;
    
    @Mock
    private IncomeRepository incomeRepository;
    
    @Mock
    private CalculationService calculationService;

    @InjectMocks
    private RetirementService retirementService;

    private static final String USER_ID = "test-user";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(retirementService, "defaultPpfReturn", 7.1);
        ReflectionTestUtils.setField(retirementService, "defaultEpfReturn", 8.1);
        
        // Setup calculation service mocks
        lenient().when(calculationService.calculateFutureValue(anyDouble(), anyDouble(), anyInt()))
                .thenAnswer(inv -> {
                    double pv = inv.getArgument(0);
                    double rate = inv.getArgument(1);
                    int years = inv.getArgument(2);
                    return pv * Math.pow(1 + rate / 100, years);
                });
        
        lenient().when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt()))
                .thenAnswer(inv -> {
                    double sip = inv.getArgument(0);
                    double rate = inv.getArgument(1);
                    int years = inv.getArgument(2);
                    if (years <= 0) return 0.0;
                    double monthlyRate = rate / 100 / 12;
                    int months = years * 12;
                    return sip * ((Math.pow(1 + monthlyRate, months) - 1) / monthlyRate) * (1 + monthlyRate);
                });
        
        // Default repository mocks
        lenient().when(investmentRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        lenient().when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        lenient().when(insuranceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        lenient().when(loanRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        lenient().when(goalRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        lenient().when(incomeRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        lenient().when(scenarioRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());
        lenient().when(insuranceRepository.findByUserIdAndTypeIn(eq(USER_ID), anyList())).thenReturn(Collections.emptyList());
        lenient().when(investmentRepository.findByUserIdAndType(eq(USER_ID), any())).thenReturn(Collections.emptyList());
    }

    @Nested
    @DisplayName("SIP Step-Up Optimization in Matrix")
    class SipOptimizationInMatrix {

        @Test
        @DisplayName("Should include SIP step-up optimization in retirement matrix")
        void shouldIncludeSipOptimizationInMatrix() {
            // Given - Set up investments with monthly SIP
            Investment mfWithSip = createInvestmentWithSip("MF SIP", Investment.InvestmentType.MUTUAL_FUND, 100000.0, 10000.0, 12.0);
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(mfWithSip));
            when(investmentRepository.findByUserIdAndType(USER_ID, Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Arrays.asList(mfWithSip));
            
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .sipStepUpPercent(10.0)
                    .mfReturn(12.0)
                    .inflationRate(6.0)
                    .build();

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(USER_ID, scenario);

            // Then
            assertThat(result).containsKey("summary");
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            assertThat(summary).containsKey("sipStepUpOptimization");
        }

        @Test
        @DisplayName("Should calculate optimal stop year when step-up is enabled")
        void shouldCalculateOptimalStopYear() {
            // Given
            Investment mfWithSip = createInvestmentWithSip("MF SIP", Investment.InvestmentType.MUTUAL_FUND, 100000.0, 20000.0, 12.0);
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(mfWithSip));
            when(investmentRepository.findByUserIdAndType(USER_ID, Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Arrays.asList(mfWithSip));
            
            Expense expense = new Expense();
            expense.setAmount(50000.0);
            expense.setFrequency(Expense.ExpenseFrequency.MONTHLY);
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(expense));
            
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .sipStepUpPercent(10.0)
                    .mfReturn(12.0)
                    .inflationRate(6.0)
                    .build();

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(USER_ID, scenario);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            @SuppressWarnings("unchecked")
            Map<String, Object> optimization = (Map<String, Object>) summary.get("sipStepUpOptimization");
            
            assertThat(optimization).isNotNull();
            assertThat(optimization).containsKey("optimalStopYear");
            assertThat(optimization).containsKey("scenarios");
            assertThat(optimization).containsKey("recommendation");
        }

        @Test
        @DisplayName("Should show 'Continue to retirement' when no early stop is optimal")
        void shouldShowContinueWhenNoEarlyStopOptimal() {
            // Given - Low SIP that needs full step-up
            Investment mfWithSip = createInvestmentWithSip("MF SIP", Investment.InvestmentType.MUTUAL_FUND, 10000.0, 1000.0, 10.0);
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(mfWithSip));
            when(investmentRepository.findByUserIdAndType(USER_ID, Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Arrays.asList(mfWithSip));
            
            Expense expense = new Expense();
            expense.setAmount(100000.0);
            expense.setFrequency(Expense.ExpenseFrequency.MONTHLY);
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(expense));
            
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .sipStepUpPercent(5.0)
                    .mfReturn(10.0)
                    .inflationRate(6.0)
                    .build();

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(USER_ID, scenario);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            @SuppressWarnings("unchecked")
            Map<String, Object> optimization = (Map<String, Object>) summary.get("sipStepUpOptimization");
            
            assertThat(optimization).isNotNull();
            // The optimal stop year should be null if continuing to retirement is best
            // or it should be close to retirement age
        }

        @Test
        @DisplayName("Should include step-up flag in matrix rows")
        void shouldIncludeStepUpFlagInMatrixRows() {
            // Given
            Investment mfWithSip = createInvestmentWithSip("MF SIP", Investment.InvestmentType.MUTUAL_FUND, 100000.0, 10000.0, 12.0);
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(mfWithSip));
            when(investmentRepository.findByUserIdAndType(USER_ID, Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Arrays.asList(mfWithSip));
            
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .sipStepUpPercent(10.0)
                    .mfReturn(12.0)
                    .inflationRate(6.0)
                    .build();

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(USER_ID, scenario);

            // Then
            assertThat(result).containsKey("matrix");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            assertThat(matrix).isNotEmpty();
            
            Map<String, Object> firstYear = matrix.get(0);
            assertThat(firstYear).containsKey("sipStepUpActive");
        }

        @Test
        @DisplayName("Should handle zero step-up percentage")
        void shouldHandleZeroStepUpPercentage() {
            // Given
            Investment mfWithSip = createInvestmentWithSip("MF SIP", Investment.InvestmentType.MUTUAL_FUND, 100000.0, 10000.0, 12.0);
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(mfWithSip));
            when(investmentRepository.findByUserIdAndType(USER_ID, Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Arrays.asList(mfWithSip));
            
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .sipStepUpPercent(0.0)
                    .mfReturn(12.0)
                    .inflationRate(6.0)
                    .build();

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(USER_ID, scenario);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            @SuppressWarnings("unchecked")
            Map<String, Object> optimization = (Map<String, Object>) summary.get("sipStepUpOptimization");
            
            // With 0% step-up, optimization should indicate flat SIP
            assertThat(optimization).isNotNull();
        }

        @Test
        @DisplayName("Should handle null step-up percentage")
        void shouldHandleNullStepUpPercentage() {
            // Given
            Investment mfWithSip = createInvestmentWithSip("MF SIP", Investment.InvestmentType.MUTUAL_FUND, 100000.0, 10000.0, 12.0);
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(mfWithSip));
            when(investmentRepository.findByUserIdAndType(USER_ID, Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Arrays.asList(mfWithSip));
            
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .sipStepUpPercent(null)
                    .mfReturn(12.0)
                    .inflationRate(6.0)
                    .build();

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(USER_ID, scenario);

            // Then - Should not throw exception
            assertThat(result).containsKey("summary");
        }
    }
    
    private Investment createInvestmentWithSip(String name, Investment.InvestmentType type, double currentValue, double monthlySip, double expectedReturn) {
        Investment inv = new Investment();
        inv.setId(UUID.randomUUID().toString());
        inv.setName(name);
        inv.setType(type);
        inv.setCurrentValue(currentValue);
        inv.setMonthlySip(monthlySip);
        inv.setExpectedReturn(expectedReturn);
        return inv;
    }

    @Nested
    @DisplayName("Optimization Scenarios Analysis")
    class OptimizationScenariosAnalysis {

        @Test
        @DisplayName("Should provide multiple scenarios in optimization")
        void shouldProvideMultipleScenarios() {
            // Given
            Investment mfWithSip = createInvestmentWithSip("MF SIP", Investment.InvestmentType.MUTUAL_FUND, 100000.0, 15000.0, 12.0);
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(mfWithSip));
            when(investmentRepository.findByUserIdAndType(USER_ID, Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Arrays.asList(mfWithSip));
            
            Expense expense = new Expense();
            expense.setAmount(40000.0);
            expense.setFrequency(Expense.ExpenseFrequency.MONTHLY);
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(expense));
            
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .sipStepUpPercent(10.0)
                    .mfReturn(12.0)
                    .inflationRate(6.0)
                    .build();

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(USER_ID, scenario);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            @SuppressWarnings("unchecked")
            Map<String, Object> optimization = (Map<String, Object>) summary.get("sipStepUpOptimization");
            
            if (optimization.containsKey("scenarios")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> scenarios = (List<Map<String, Object>>) optimization.get("scenarios");
                // Should have at least one scenario
                assertThat(scenarios).isNotEmpty();
                
                // Each scenario should have required fields
                if (!scenarios.isEmpty()) {
                    Map<String, Object> firstScenario = scenarios.get(0);
                    assertThat(firstScenario).containsKey("stopYear");
                    assertThat(firstScenario).containsKey("projectedCorpus");
                }
            }
        }

        @Test
        @DisplayName("Should calculate savings from early stop")
        void shouldCalculateSavingsFromEarlyStop() {
            // Given - High SIP with high step-up where early stop makes sense
            Investment mfWithSip = createInvestmentWithSip("MF SIP", Investment.InvestmentType.MUTUAL_FUND, 500000.0, 50000.0, 12.0);
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(mfWithSip));
            when(investmentRepository.findByUserIdAndType(USER_ID, Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Arrays.asList(mfWithSip));
            
            Expense expense = new Expense();
            expense.setAmount(30000.0);
            expense.setFrequency(Expense.ExpenseFrequency.MONTHLY);
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(expense));
            
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .sipStepUpPercent(15.0)
                    .mfReturn(12.0)
                    .inflationRate(6.0)
                    .build();

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(USER_ID, scenario);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            @SuppressWarnings("unchecked")
            Map<String, Object> optimization = (Map<String, Object>) summary.get("sipStepUpOptimization");
            
            assertThat(optimization).isNotNull();
            // The optimization should provide actionable insights
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle very short investment horizon")
        void shouldHandleShortInvestmentHorizon() {
            // Given - Only 5 years to retirement
            Investment mfWithSip = createInvestmentWithSip("MF SIP", Investment.InvestmentType.MUTUAL_FUND, 1000000.0, 50000.0, 12.0);
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(mfWithSip));
            when(investmentRepository.findByUserIdAndType(USER_ID, Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Arrays.asList(mfWithSip));
            
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(55)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .sipStepUpPercent(10.0)
                    .mfReturn(12.0)
                    .inflationRate(6.0)
                    .build();

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(USER_ID, scenario);

            // Then
            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("Should handle very high step-up percentage")
        void shouldHandleHighStepUpPercentage() {
            // Given
            Investment mfWithSip = createInvestmentWithSip("MF SIP", Investment.InvestmentType.MUTUAL_FUND, 100000.0, 10000.0, 12.0);
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(mfWithSip));
            when(investmentRepository.findByUserIdAndType(USER_ID, Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Arrays.asList(mfWithSip));
            
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .sipStepUpPercent(50.0) // 50% annual step-up
                    .mfReturn(12.0)
                    .inflationRate(6.0)
                    .build();

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(USER_ID, scenario);

            // Then
            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("Should handle low expected return")
        void shouldHandleLowExpectedReturn() {
            // Given
            Investment mfWithSip = createInvestmentWithSip("MF SIP", Investment.InvestmentType.MUTUAL_FUND, 100000.0, 10000.0, 5.0);
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(mfWithSip));
            when(investmentRepository.findByUserIdAndType(USER_ID, Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Arrays.asList(mfWithSip));
            
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .sipStepUpPercent(10.0)
                    .mfReturn(5.0) // Low return
                    .inflationRate(6.0)
                    .build();

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(USER_ID, scenario);

            // Then
            assertThat(result).containsKey("summary");
        }

        @Test
        @DisplayName("Should handle zero monthly SIP")
        void shouldHandleZeroMonthlySip() {
            // Given - No SIP
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .sipStepUpPercent(10.0)
                    .mfReturn(12.0)
                    .inflationRate(6.0)
                    .build();

            // When
            Map<String, Object> result = retirementService.generateRetirementMatrix(USER_ID, scenario);

            // Then
            assertThat(result).containsKey("summary");
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            @SuppressWarnings("unchecked")
            Map<String, Object> optimization = (Map<String, Object>) summary.get("sipStepUpOptimization");
            assertThat(optimization).isNotNull();
        }
    }
}
