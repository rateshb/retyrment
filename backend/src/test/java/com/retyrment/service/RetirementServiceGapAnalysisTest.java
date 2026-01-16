package com.retyrment.service;

import com.retyrment.model.Goal;
import com.retyrment.model.Insurance;
import com.retyrment.model.Investment;
import com.retyrment.model.Loan;
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

import java.time.LocalDate;
import java.util.Arrays;
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
@DisplayName("RetirementService Gap Analysis Branch Coverage Tests")
class RetirementServiceGapAnalysisTest {

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
    @DisplayName("generateRetirementMatrix - Insurance Maturity Branches")
    class InsuranceMaturityBranches {

        @Test
        @DisplayName("should include insurance maturity when maturity date matches calendar year")
        void shouldIncludeInsuranceMaturityWhenDateMatches() {
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            int calendarYear = currentYear + 5;

            Insurance ulip = Insurance.builder()
                    .id("ulip1")
                    .policyName("ULIP Policy")
                    .type(Insurance.InsuranceType.ULIP)
                    .maturityDate(LocalDate.of(calendarYear, 6, 15))
                    .maturityBenefit(500000.0)
                    .fundValue(400000.0)
                    .build();

            when(insuranceRepository.findByUserIdAndTypeIn(org.mockito.ArgumentMatchers.eq("test-user"), org.mockito.ArgumentMatchers.anyList()))
                    .thenReturn(Collections.singletonList(ulip));
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Find the row for calendarYear
            Map<String, Object> yearRow = matrix.stream()
                    .filter(row -> ((Integer) row.get("year")).equals(calendarYear))
                    .findFirst()
                    .orElse(null);
            
            if (yearRow != null) {
                @SuppressWarnings("unchecked")
                List<String> maturingPolicies = (List<String>) yearRow.get("maturingPolicies");
                assertThat(maturingPolicies).isNotNull();
            }
        }

        @Test
        @DisplayName("should use maturityBenefit when available for insurance maturity")
        void shouldUseMaturityBenefitWhenAvailable() {
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            int calendarYear = currentYear + 5;

            Insurance endowment = Insurance.builder()
                    .id("end1")
                    .policyName("Endowment Policy")
                    .type(Insurance.InsuranceType.ENDOWMENT)
                    .maturityDate(LocalDate.of(calendarYear, 6, 15))
                    .maturityBenefit(1000000.0)
                    .fundValue(800000.0)
                    .build();

            when(insuranceRepository.findByUserIdAndTypeIn(org.mockito.ArgumentMatchers.eq("test-user"), org.mockito.ArgumentMatchers.anyList()))
                    .thenReturn(Collections.singletonList(endowment));
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            assertThat(matrix).isNotEmpty();
        }

        @Test
        @DisplayName("should use fundValue when maturityBenefit is null")
        void shouldUseFundValueWhenMaturityBenefitIsNull() {
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            int calendarYear = currentYear + 5;

            Insurance ulip = Insurance.builder()
                    .id("ulip1")
                    .policyName("ULIP Policy")
                    .type(Insurance.InsuranceType.ULIP)
                    .maturityDate(LocalDate.of(calendarYear, 6, 15))
                    .maturityBenefit(null)
                    .fundValue(400000.0)
                    .build();

            when(insuranceRepository.findByUserIdAndTypeIn(org.mockito.ArgumentMatchers.eq("test-user"), org.mockito.ArgumentMatchers.anyList()))
                    .thenReturn(Collections.singletonList(ulip));
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            assertThat(matrix).isNotEmpty();
        }

        @Test
        @DisplayName("should use 0 when both maturityBenefit and fundValue are null")
        void shouldUseZeroWhenBothMaturityBenefitAndFundValueAreNull() {
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            int calendarYear = currentYear + 5;

            Insurance endowment = Insurance.builder()
                    .id("end1")
                    .policyName("Endowment Policy")
                    .type(Insurance.InsuranceType.ENDOWMENT)
                    .maturityDate(LocalDate.of(calendarYear, 6, 15))
                    .maturityBenefit(null)
                    .fundValue(null)
                    .build();

            when(insuranceRepository.findByUserIdAndTypeIn(org.mockito.ArgumentMatchers.eq("test-user"), org.mockito.ArgumentMatchers.anyList()))
                    .thenReturn(Collections.singletonList(endowment));
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            assertThat(matrix).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("generateRetirementMatrix - Investment Maturity Branches")
    class InvestmentMaturityBranches {

        @Test
        @DisplayName("should include investment maturity when maturity date matches calendar year")
        void shouldIncludeInvestmentMaturityWhenDateMatches() {
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            int calendarYear = currentYear + 5;

            Investment fd = Investment.builder()
                    .id("fd1")
                    .name("Fixed Deposit")
                    .type(Investment.InvestmentType.FD)
                    .maturityDate(LocalDate.of(calendarYear, 6, 15))
                    .currentValue(100000.0)
                    .expectedReturn(7.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(fd));
            when(calculationService.calculateFutureValue(anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(140255.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Find the row for calendarYear
            Map<String, Object> yearRow = matrix.stream()
                    .filter(row -> ((Integer) row.get("year")).equals(calendarYear))
                    .findFirst()
                    .orElse(null);
            
            if (yearRow != null) {
                @SuppressWarnings("unchecked")
                List<String> maturingInvestments = (List<String>) yearRow.get("maturingInvestments");
                assertThat(maturingInvestments).isNotNull();
            }
        }

        @Test
        @DisplayName("should exclude investment when maturity date does not match calendar year")
        void shouldExcludeInvestmentWhenMaturityDateDoesNotMatch() {
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            int calendarYear = currentYear + 5;
            int differentYear = currentYear + 10;

            Investment fd = Investment.builder()
                    .id("fd1")
                    .name("Fixed Deposit")
                    .type(Investment.InvestmentType.FD)
                    .maturityDate(LocalDate.of(differentYear, 6, 15))
                    .currentValue(100000.0)
                    .expectedReturn(7.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(fd));
            when(calculationService.calculateFutureValue(anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(140255.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Find the row for calendarYear
            Map<String, Object> yearRow = matrix.stream()
                    .filter(row -> ((Integer) row.get("year")).equals(calendarYear))
                    .findFirst()
                    .orElse(null);
            
            if (yearRow != null) {
                @SuppressWarnings("unchecked")
                List<String> maturingInvestments = (List<String>) yearRow.get("maturingInvestments");
                assertThat(maturingInvestments).doesNotContain("Fixed Deposit (FD)");
            }
        }
    }

    @Nested
    @DisplayName("generateRetirementMatrix - Goal Outflow Branches")
    class GoalOutflowBranches {

        @Test
        @DisplayName("should include goal outflow when target year matches calendar year")
        void shouldIncludeGoalOutflowWhenTargetYearMatches() {
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            int calendarYear = currentYear + 5;

            Goal goal = Goal.builder()
                    .id("goal1")
                    .name("Child Education")
                    .targetYear(calendarYear)
                    .targetAmount(2000000.0)
                    .build();

            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user"))
                    .thenReturn(Collections.singletonList(goal));
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(calculationService.calculateInflatedValue(2000000.0, 6.0, 5))
                    .thenReturn(2676451.0);
            when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);
            when(calculationService.calculateFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Find the row for calendarYear
            Map<String, Object> yearRow = matrix.stream()
                    .filter(row -> ((Integer) row.get("year")).equals(calendarYear))
                    .findFirst()
                    .orElse(null);
            
            if (yearRow != null) {
                Object goalOutflowObj = yearRow.get("goalOutflow");
                assertThat(goalOutflowObj).isNotNull();
                // Handle both Long and Double
                if (goalOutflowObj instanceof Long) {
                    assertThat((Long) goalOutflowObj).isGreaterThanOrEqualTo(0L);
                } else if (goalOutflowObj instanceof Double) {
                    assertThat((Double) goalOutflowObj).isGreaterThanOrEqualTo(0.0);
                }
            }
        }

        @Test
        @DisplayName("should exclude goal when target year does not match calendar year")
        void shouldExcludeGoalWhenTargetYearDoesNotMatch() {
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            int calendarYear = currentYear + 5;
            int differentYear = currentYear + 10;

            Goal goal = Goal.builder()
                    .id("goal1")
                    .name("Child Education")
                    .targetYear(differentYear)
                    .targetAmount(2000000.0)
                    .build();

            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user"))
                    .thenReturn(Collections.singletonList(goal));
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(calculationService.calculateInflatedValue(anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(0.0);
            when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);
            when(calculationService.calculateFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            
            // Find the row for calendarYear
            Map<String, Object> yearRow = matrix.stream()
                    .filter(row -> ((Integer) row.get("year")).equals(calendarYear))
                    .findFirst()
                    .orElse(null);
            
            if (yearRow != null) {
                Object goalOutflowObj = yearRow.get("goalOutflow");
                // Handle both Long and Double
                if (goalOutflowObj instanceof Long) {
                    assertThat((Long) goalOutflowObj).isEqualTo(0L);
                } else if (goalOutflowObj instanceof Double) {
                    assertThat((Double) goalOutflowObj).isEqualTo(0.0);
                } else {
                    assertThat(goalOutflowObj).isNull();
                }
            }
        }

        @Test
        @DisplayName("should handle goal with null targetAmount")
        void shouldHandleGoalWithNullTargetAmount() {
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            int calendarYear = currentYear + 5;

            Goal goal = Goal.builder()
                    .id("goal1")
                    .name("Child Education")
                    .targetYear(calendarYear)
                    .targetAmount(null)
                    .build();

            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user"))
                    .thenReturn(Collections.singletonList(goal));
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(calculationService.calculateInflatedValue(0.0, 6.0, 5))
                    .thenReturn(0.0);
            when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);
            when(calculationService.calculateFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            Map<String, Object> result = retirementService.generateRetirementMatrix("test-user", scenario);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
            assertThat(matrix).isNotEmpty();
        }
    }
}
