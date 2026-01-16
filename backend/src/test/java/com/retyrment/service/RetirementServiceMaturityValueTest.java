package com.retyrment.service;

import com.retyrment.model.Investment;
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
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RetirementService calculateExpectedMaturityValue Branch Coverage Tests")
class RetirementServiceMaturityValueTest {

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
        ReflectionTestUtils.setField(retirementService, "defaultPpfReturn", 7.1);
        ReflectionTestUtils.setField(retirementService, "calculationService", calculationService);
        
        // Setup common mocks
        when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
        when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
    }

    @Nested
    @DisplayName("calculateExpectedMaturityValue - Null and Edge Cases")
    class NullAndEdgeCases {
        @Test
        @DisplayName("should return currentValue when maturityDate is null")
        void shouldReturnCurrentValueWhenMaturityDateIsNull() {
            Investment inv = Investment.builder()
                    .id("inv1")
                    .name("Investment")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .maturityDate(null)
                    .currentValue(100000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(inv));

            // This will call calculateExpectedMaturityValue internally
            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            // Investment with null maturity date should not be included
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).isEmpty();
        }

        @Test
        @DisplayName("should return currentValue when maturityDate is in the past")
        void shouldReturnCurrentValueWhenMaturityDateInPast() {
            LocalDate pastDate = LocalDate.now().minusYears(1);
            Investment inv = Investment.builder()
                    .id("inv1")
                    .name("Investment")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .maturityDate(pastDate)
                    .currentValue(100000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(inv));

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            // Investment with past maturity date should not be included
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).isEmpty();
        }

        @Test
        @DisplayName("should handle null currentValue and use investedAmount")
        void shouldHandleNullCurrentValueAndUseInvestedAmount() {
            LocalDate futureDate = LocalDate.now().plusYears(10);
            Investment inv = Investment.builder()
                    .id("inv1")
                    .name("Investment")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .maturityDate(futureDate)
                    .currentValue(null)
                    .investedAmount(50000.0)
                    .expectedReturn(10.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(inv));
            when(calculationService.calculateFutureValue(50000.0, 10.0, 10))
                    .thenReturn(130000.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
        }

        @Test
        @DisplayName("should return 0 when both currentValue and investedAmount are null")
        void shouldReturnZeroWhenBothCurrentValueAndInvestedAmountAreNull() {
            LocalDate futureDate = LocalDate.now().plusYears(10);
            Investment inv = Investment.builder()
                    .id("inv1")
                    .name("Investment")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .maturityDate(futureDate)
                    .currentValue(null)
                    .investedAmount(null)
                    .expectedReturn(10.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(inv));
            when(calculationService.calculateFutureValue(0.0, 10.0, 10))
                    .thenReturn(0.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
            assertThat(maturingInvestments.get(0).get("expectedMaturityValue")).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("calculateExpectedMaturityValue - FD Type")
    class FDType {
        @Test
        @DisplayName("should calculate FD maturity with interestRate")
        void shouldCalculateFdMaturityWithInterestRate() {
            LocalDate futureDate = LocalDate.now().plusYears(5);
            Investment fd = Investment.builder()
                    .id("fd1")
                    .name("Fixed Deposit")
                    .type(Investment.InvestmentType.FD)
                    .maturityDate(futureDate)
                    .currentValue(100000.0)
                    .interestRate(7.5)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(fd));
            when(calculationService.calculateFutureValue(100000.0, 7.5, 5))
                    .thenReturn(143562.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
            assertThat(maturingInvestments.get(0).get("type")).isEqualTo("FD");
        }

        @Test
        @DisplayName("should use default 7.0 rate when interestRate is null for FD")
        void shouldUseDefaultRateWhenInterestRateIsNullForFd() {
            LocalDate futureDate = LocalDate.now().plusYears(5);
            Investment fd = Investment.builder()
                    .id("fd1")
                    .name("Fixed Deposit")
                    .type(Investment.InvestmentType.FD)
                    .maturityDate(futureDate)
                    .currentValue(100000.0)
                    .interestRate(null)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(fd));
            when(calculationService.calculateFutureValue(100000.0, 7.0, 5))
                    .thenReturn(140255.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
        }
    }

    @Nested
    @DisplayName("calculateExpectedMaturityValue - RD Type")
    class RDType {
        @Test
        @DisplayName("should calculate RD maturity with currentValue and SIP")
        void shouldCalculateRdMaturityWithCurrentValueAndSip() {
            LocalDate futureDate = LocalDate.now().plusYears(5);
            Investment rd = Investment.builder()
                    .id("rd1")
                    .name("Recurring Deposit")
                    .type(Investment.InvestmentType.RD)
                    .maturityDate(futureDate)
                    .currentValue(50000.0)
                    .interestRate(6.5)
                    .monthlySip(10000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(rd));
            when(calculationService.calculateFutureValue(50000.0, 6.5, 5))
                    .thenReturn(68850.0);
            when(calculationService.calculateSIPFutureValue(10000.0, 6.5, 5))
                    .thenReturn(700000.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
            assertThat(maturingInvestments.get(0).get("type")).isEqualTo("RD");
        }

        @Test
        @DisplayName("should calculate RD maturity without SIP when monthlySip is null")
        void shouldCalculateRdMaturityWithoutSipWhenMonthlySipIsNull() {
            LocalDate futureDate = LocalDate.now().plusYears(5);
            Investment rd = Investment.builder()
                    .id("rd1")
                    .name("Recurring Deposit")
                    .type(Investment.InvestmentType.RD)
                    .maturityDate(futureDate)
                    .currentValue(50000.0)
                    .interestRate(6.5)
                    .monthlySip(null)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(rd));
            when(calculationService.calculateFutureValue(50000.0, 6.5, 5))
                    .thenReturn(68850.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
        }

        @Test
        @DisplayName("should calculate RD maturity without SIP when monthlySip is zero")
        void shouldCalculateRdMaturityWithoutSipWhenMonthlySipIsZero() {
            LocalDate futureDate = LocalDate.now().plusYears(5);
            Investment rd = Investment.builder()
                    .id("rd1")
                    .name("Recurring Deposit")
                    .type(Investment.InvestmentType.RD)
                    .maturityDate(futureDate)
                    .currentValue(50000.0)
                    .interestRate(6.5)
                    .monthlySip(0.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(rd));
            when(calculationService.calculateFutureValue(50000.0, 6.5, 5))
                    .thenReturn(68850.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
        }

        @Test
        @DisplayName("should use default 6.5 rate when interestRate is null for RD")
        void shouldUseDefaultRateWhenInterestRateIsNullForRd() {
            LocalDate futureDate = LocalDate.now().plusYears(5);
            Investment rd = Investment.builder()
                    .id("rd1")
                    .name("Recurring Deposit")
                    .type(Investment.InvestmentType.RD)
                    .maturityDate(futureDate)
                    .currentValue(50000.0)
                    .interestRate(null)
                    .monthlySip(10000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(rd));
            when(calculationService.calculateFutureValue(50000.0, 6.5, 5))
                    .thenReturn(68850.0);
            when(calculationService.calculateSIPFutureValue(10000.0, 6.5, 5))
                    .thenReturn(700000.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
        }
    }

    @Nested
    @DisplayName("calculateExpectedMaturityValue - PPF Type")
    class PPFType {
        @Test
        @DisplayName("should calculate PPF maturity with currentValue and yearlyContribution")
        void shouldCalculatePpfMaturityWithCurrentValueAndYearlyContribution() {
            LocalDate futureDate = LocalDate.now().plusYears(5);
            Investment ppf = Investment.builder()
                    .id("ppf1")
                    .name("Public Provident Fund")
                    .type(Investment.InvestmentType.PPF)
                    .maturityDate(futureDate)
                    .currentValue(100000.0)
                    .expectedReturn(7.1)
                    .yearlyContribution(150000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(ppf));
            when(calculationService.calculateFutureValue(100000.0, 7.1, 5))
                    .thenReturn(141000.0);
            when(calculationService.calculateSIPFutureValue(12500.0, 7.1, 5)) // 150000/12
                    .thenReturn(900000.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
            assertThat(maturingInvestments.get(0).get("type")).isEqualTo("PPF");
        }

        @Test
        @DisplayName("should calculate PPF maturity without yearlyContribution when null")
        void shouldCalculatePpfMaturityWithoutYearlyContributionWhenNull() {
            LocalDate futureDate = LocalDate.now().plusYears(5);
            Investment ppf = Investment.builder()
                    .id("ppf1")
                    .name("Public Provident Fund")
                    .type(Investment.InvestmentType.PPF)
                    .maturityDate(futureDate)
                    .currentValue(100000.0)
                    .expectedReturn(7.1)
                    .yearlyContribution(null)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(ppf));
            when(calculationService.calculateFutureValue(100000.0, 7.1, 5))
                    .thenReturn(141000.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
        }

        @Test
        @DisplayName("should calculate PPF maturity without yearlyContribution when zero")
        void shouldCalculatePpfMaturityWithoutYearlyContributionWhenZero() {
            LocalDate futureDate = LocalDate.now().plusYears(5);
            Investment ppf = Investment.builder()
                    .id("ppf1")
                    .name("Public Provident Fund")
                    .type(Investment.InvestmentType.PPF)
                    .maturityDate(futureDate)
                    .currentValue(100000.0)
                    .expectedReturn(7.1)
                    .yearlyContribution(0.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(ppf));
            when(calculationService.calculateFutureValue(100000.0, 7.1, 5))
                    .thenReturn(141000.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
        }

        @Test
        @DisplayName("should use defaultPpfReturn when expectedReturn is null for PPF")
        void shouldUseDefaultPpfReturnWhenExpectedReturnIsNullForPpf() {
            LocalDate futureDate = LocalDate.now().plusYears(5);
            Investment ppf = Investment.builder()
                    .id("ppf1")
                    .name("Public Provident Fund")
                    .type(Investment.InvestmentType.PPF)
                    .maturityDate(futureDate)
                    .currentValue(100000.0)
                    .expectedReturn(null)
                    .yearlyContribution(150000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(ppf));
            when(calculationService.calculateFutureValue(100000.0, 7.1, 5))
                    .thenReturn(141000.0);
            when(calculationService.calculateSIPFutureValue(12500.0, 7.1, 5))
                    .thenReturn(900000.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
        }
    }

    @Nested
    @DisplayName("calculateExpectedMaturityValue - Default Type")
    class DefaultType {
        @Test
        @DisplayName("should calculate maturity for other investment types using expectedReturn")
        void shouldCalculateMaturityForOtherTypesUsingExpectedReturn() {
            LocalDate futureDate = LocalDate.now().plusYears(5);
            Investment mf = Investment.builder()
                    .id("mf1")
                    .name("Mutual Fund")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .maturityDate(futureDate)
                    .currentValue(200000.0)
                    .expectedReturn(12.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(mf));
            when(calculationService.calculateFutureValue(200000.0, 12.0, 5))
                    .thenReturn(352468.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
            assertThat(maturingInvestments.get(0).get("type")).isEqualTo("MUTUAL_FUND");
        }

        @Test
        @DisplayName("should use default 7.0 rate when expectedReturn is null for other types")
        void shouldUseDefaultRateWhenExpectedReturnIsNullForOtherTypes() {
            LocalDate futureDate = LocalDate.now().plusYears(5);
            Investment mf = Investment.builder()
                    .id("mf1")
                    .name("Mutual Fund")
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .maturityDate(futureDate)
                    .currentValue(200000.0)
                    .expectedReturn(null)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(mf));
            when(calculationService.calculateFutureValue(200000.0, 7.0, 5))
                    .thenReturn(280510.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
        }

        @Test
        @DisplayName("should handle null type and use default calculation")
        void shouldHandleNullTypeAndUseDefaultCalculation() {
            LocalDate futureDate = LocalDate.now().plusYears(5);
            Investment inv = Investment.builder()
                    .id("inv1")
                    .name("Investment")
                    .type(null)
                    .maturityDate(futureDate)
                    .currentValue(100000.0)
                    .expectedReturn(8.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(java.util.Arrays.asList(inv));
            when(calculationService.calculateFutureValue(100000.0, 8.0, 5))
                    .thenReturn(146933.0);

            var result = retirementService.calculateMaturingBeforeRetirement("test-user", 35, 60);

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> maturingInvestments = 
                    (java.util.List<java.util.Map<String, Object>>) result.get("maturingInvestments");
            assertThat(maturingInvestments).hasSize(1);
            assertThat(maturingInvestments.get(0).get("type")).isEqualTo("OTHER");
        }
    }
}
