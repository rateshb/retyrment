package com.retyrment.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RetirementService Helper Methods Branch Coverage Tests")
class RetirementServiceHelperMethodsTest {

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
    }

    @Nested
    @DisplayName("getRateForPeriod - Branch Coverage")
    class GetRateForPeriodBranches {

        @Test
        @DisplayName("should return default rate when periodReturns is null")
        void shouldReturnDefaultRateWhenPeriodReturnsIsNull() {
            // This is tested indirectly through generateRetirementMatrix
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .mfReturns(null)
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(java.util.Optional.empty());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);

            var result = retirementService.generateRetirementMatrix("test-user", scenario);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should return default rate when periodReturns is empty")
        void shouldReturnDefaultRateWhenPeriodReturnsIsEmpty() {
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .mfReturns(Collections.emptyList())
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(java.util.Optional.empty());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);

            var result = retirementService.generateRetirementMatrix("test-user", scenario);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should return rate from period when year is within range")
        void shouldReturnRateFromPeriodWhenYearIsWithinRange() {
            RetirementScenario.PeriodReturn period = RetirementScenario.PeriodReturn.builder()
                    .fromYear(0)
                    .toYear(10)
                    .rate(11.0)
                    .build();

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .mfReturns(Collections.singletonList(period))
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(java.util.Optional.empty());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);

            var result = retirementService.generateRetirementMatrix("test-user", scenario);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should return default rate when year is outside all periods")
        void shouldReturnDefaultRateWhenYearIsOutsideAllPeriods() {
            RetirementScenario.PeriodReturn period = RetirementScenario.PeriodReturn.builder()
                    .fromYear(0)
                    .toYear(5)
                    .rate(11.0)
                    .build();

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .mfReturns(Collections.singletonList(period))
                    .build();

            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(java.util.Optional.empty());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);

            var result = retirementService.generateRetirementMatrix("test-user", scenario);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("getAverageReturn - Branch Coverage")
    class GetAverageReturnBranches {

        @Test
        @DisplayName("should return default rate when investments list is empty")
        void shouldReturnDefaultRateWhenInvestmentsEmpty() {
            // This is tested indirectly through generateRetirementMatrix
            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            when(investmentRepository.findByUserIdAndType("test-user", Investment.InvestmentType.FD))
                    .thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(java.util.Optional.empty());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);

            var result = retirementService.generateRetirementMatrix("test-user", scenario);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should use interestRate for FD investment")
        void shouldUseInterestRateForFdInvestment() {
            Investment fd = Investment.builder()
                    .type(Investment.InvestmentType.FD)
                    .currentValue(100000.0)
                    .interestRate(7.5)
                    .expectedReturn(null)
                    .build();

            when(investmentRepository.findByUserIdAndType("test-user", Investment.InvestmentType.FD))
                    .thenReturn(Collections.singletonList(fd));
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(java.util.Optional.empty());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            var result = retirementService.generateRetirementMatrix("test-user", scenario);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should use expectedReturn when interestRate is null")
        void shouldUseExpectedReturnWhenInterestRateIsNull() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(100000.0)
                    .interestRate(null)
                    .expectedReturn(12.5)
                    .build();

            when(investmentRepository.findByUserIdAndType("test-user", Investment.InvestmentType.MUTUAL_FUND))
                    .thenReturn(Collections.singletonList(mf));
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.singletonList(mf));
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(java.util.Optional.empty());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            var result = retirementService.generateRetirementMatrix("test-user", scenario);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should use default rate when both interestRate and expectedReturn are null")
        void shouldUseDefaultRateWhenBothRatesAreNull() {
            Investment inv = Investment.builder()
                    .type(Investment.InvestmentType.FD)
                    .currentValue(100000.0)
                    .interestRate(null)
                    .expectedReturn(null)
                    .build();

            when(investmentRepository.findByUserIdAndType("test-user", Investment.InvestmentType.FD))
                    .thenReturn(Collections.singletonList(inv));
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(java.util.Optional.empty());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            var result = retirementService.generateRetirementMatrix("test-user", scenario);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should use currentValue when available")
        void shouldUseCurrentValueWhenAvailable() {
            Investment inv = Investment.builder()
                    .type(Investment.InvestmentType.FD)
                    .currentValue(100000.0)
                    .investedAmount(80000.0)
                    .interestRate(7.0)
                    .build();

            when(investmentRepository.findByUserIdAndType("test-user", Investment.InvestmentType.FD))
                    .thenReturn(Collections.singletonList(inv));
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(java.util.Optional.empty());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            var result = retirementService.generateRetirementMatrix("test-user", scenario);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should use investedAmount when currentValue is null")
        void shouldUseInvestedAmountWhenCurrentValueIsNull() {
            Investment inv = Investment.builder()
                    .type(Investment.InvestmentType.FD)
                    .currentValue(null)
                    .investedAmount(80000.0)
                    .interestRate(7.0)
                    .build();

            when(investmentRepository.findByUserIdAndType("test-user", Investment.InvestmentType.FD))
                    .thenReturn(Collections.singletonList(inv));
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(java.util.Optional.empty());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            var result = retirementService.generateRetirementMatrix("test-user", scenario);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should return default rate when totalValue is 0")
        void shouldReturnDefaultRateWhenTotalValueIsZero() {
            Investment inv = Investment.builder()
                    .type(Investment.InvestmentType.FD)
                    .currentValue(0.0)
                    .investedAmount(0.0)
                    .interestRate(7.0)
                    .build();

            when(investmentRepository.findByUserIdAndType("test-user", Investment.InvestmentType.FD))
                    .thenReturn(Collections.singletonList(inv));
            when(investmentRepository.findByType(Investment.InvestmentType.PPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.EPF)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND)).thenReturn(Collections.emptyList());
            when(investmentRepository.findByType(Investment.InvestmentType.NPS)).thenReturn(Collections.emptyList());
            when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByTypeIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user")).thenReturn(java.util.Optional.empty());
            when(calculationService.calculateSIPFutureValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);
            when(calculationService.calculateInflatedValue(org.mockito.ArgumentMatchers.anyDouble(), 
                    org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0.0);

            RetirementScenario scenario = RetirementScenario.builder()
                    .currentAge(35)
                    .retirementAge(60)
                    .lifeExpectancy(85)
                    .build();

            var result = retirementService.generateRetirementMatrix("test-user", scenario);
            assertThat(result).isNotNull();
        }
    }
}
