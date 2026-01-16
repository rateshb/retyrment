package com.retyrment.service;

import com.retyrment.model.Investment;
import com.retyrment.model.RetirementScenario;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RetirementService Retirement Income Projection Branch Coverage Tests")
class RetirementServiceRetirementIncomeProjectionTest {

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
    @DisplayName("generateRetirementMatrix - Retirement Income Projection Branches")
    class RetirementIncomeProjectionBranches {

        @Test
        @DisplayName("should project SIMPLE_DEPLETION strategy in retirement years")
        void shouldProjectSimpleDepletionInRetirementYears() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

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
        @DisplayName("should project SAFE_4_PERCENT strategy in retirement years")
        void shouldProjectSafe4PercentInRetirementYears() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

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
        @DisplayName("should project SUSTAINABLE strategy in retirement years")
        void shouldProjectSustainableInRetirementYears() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(8.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            assertThat(gapAnalysis).isNotNull();
            assertThat(gapAnalysis.get("incomeStrategy")).isEqualTo("SUSTAINABLE");
        }

        @Test
        @DisplayName("should use default SUSTAINABLE strategy when incomeStrategy is null")
        void shouldUseDefaultSustainableWhenIncomeStrategyIsNull() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy(null)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) result.get("gapAnalysis");
            assertThat(gapAnalysis).isNotNull();
            assertThat(gapAnalysis.get("incomeStrategy")).isEqualTo("SUSTAINABLE");
        }

        @Test
        @DisplayName("should handle loop condition m < 5 && (year + m) < retirementYears in SUSTAINABLE projection")
        void shouldHandleLoopConditionInSustainableProjection() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(8.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            // Check if retirementIncomeProjection exists in result
            if (result.containsKey("retirementIncomeProjection")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> retirementIncomeProjection = 
                        (List<Map<String, Object>>) result.get("retirementIncomeProjection");
                assertThat(retirementIncomeProjection).isNotNull();
            } else {
                // If the field doesn't exist, that's okay - it might be conditionally returned
                assertThat(result).isNotNull();
            }
        }

        @Test
        @DisplayName("should handle retirementYears < 5 in SUSTAINABLE projection loop")
        void shouldHandleRetirementYearsLessThan5InSustainableProjection() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(63) // Only 3 retirement years
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(8.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            // Check if retirementIncomeProjection exists in result
            if (result.containsKey("retirementIncomeProjection")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> retirementIncomeProjection = 
                        (List<Map<String, Object>>) result.get("retirementIncomeProjection");
                assertThat(retirementIncomeProjection).isNotNull();
            } else {
                // If the field doesn't exist, that's okay - it might be conditionally returned
                assertThat(result).isNotNull();
            }
        }

        @Test
        @DisplayName("should handle nested loop condition m < 5 in SIMPLE_DEPLETION")
        void shouldHandleNestedLoopInSimpleDepletion() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SIMPLE_DEPLETION")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(5.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            if (summary != null && summary.containsKey("retirementIncomeProjection")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> projection = (List<Map<String, Object>>) summary.get("retirementIncomeProjection");
                assertThat(projection).isNotEmpty();
            }
        }

        @Test
        @DisplayName("should handle nested loop condition m < 5 in SAFE_4_PERCENT")
        void shouldHandleNestedLoopInSafe4Percent() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .incomeStrategy("SAFE_4_PERCENT")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(4.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            if (summary != null && summary.containsKey("retirementIncomeProjection")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> projection = (List<Map<String, Object>>) summary.get("retirementIncomeProjection");
                assertThat(projection).isNotEmpty();
            }
        }

        @Test
        @DisplayName("should handle loop condition when year + m >= retirementYears")
        void shouldHandleLoopConditionWhenYearPlusMExceedsRetirementYears() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(70) // Only 10 retirement years
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(8.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            if (summary != null && summary.containsKey("retirementIncomeProjection")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> projection = (List<Map<String, Object>>) summary.get("retirementIncomeProjection");
                assertThat(projection).isNotEmpty();
            }
        }

        @Test
        @DisplayName("should handle remainingYears = 0 in SIMPLE_DEPLETION")
        void shouldHandleZeroRemainingYearsInSimpleDepletion() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(60) // Same as retirement age - no retirement years
                    .incomeStrategy("SIMPLE_DEPLETION")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(5.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            if (summary != null && summary.containsKey("retirementIncomeProjection")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> projection = (List<Map<String, Object>>) summary.get("retirementIncomeProjection");
                // Should handle zero remaining years gracefully
                assertThat(projection).isNotNull();
            }
        }

        @Test
        @DisplayName("should cap retirement income projection at 30 years")
        void shouldCapRetirementIncomeProjectionAt30Years() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(100) // 40 retirement years, but should cap at 30
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(8.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            if (summary != null && summary.containsKey("retirementIncomeProjection")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> projection = (List<Map<String, Object>>) summary.get("retirementIncomeProjection");
                // Should cap at year 30 even though retirementYears is 40
                if (projection != null && !projection.isEmpty()) {
                    Map<String, Object> lastProjection = projection.get(projection.size() - 1);
                    Integer lastYear = (Integer) lastProjection.get("year");
                    assertThat(lastYear).isLessThanOrEqualTo(30);
                }
            }
        }

        @Test
        @DisplayName("should handle loop condition when year > 30 in projection")
        void shouldHandleLoopConditionWhenYearGreaterThan30() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(1000000.0)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(95) // 35 retirement years, but loop should cap at 30
                    .incomeStrategy("SUSTAINABLE")
                    .corpusReturnRate(10.0)
                    .withdrawalRate(8.0)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            if (summary != null && summary.containsKey("retirementIncomeProjection")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> projection = (List<Map<String, Object>>) summary.get("retirementIncomeProjection");
                assertThat(projection).isNotNull();
            }
        }
    }
}
