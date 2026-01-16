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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private CalculationService calculationService;

    @InjectMocks
    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(analysisService, "defaultInflation", 6.0);
        ReflectionTestUtils.setField(analysisService, "defaultMFReturn", 12.0);
    }

    @Nested
    @DisplayName("calculateNetWorth")
    class CalculateNetWorth {

        @Test
        @DisplayName("should calculate net worth with investments and loans")
        void shouldCalculateNetWorth() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .investedAmount(100000.0)
                    .currentValue(120000.0)
                    .build();

            Investment ppf = Investment.builder()
                    .type(Investment.InvestmentType.PPF)
                    .investedAmount(500000.0)
                    .currentValue(600000.0)
                    .build();

            Loan homeLoan = Loan.builder()
                    .outstandingAmount(4000000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(mf, ppf));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(homeLoan));

            Map<String, Object> result = analysisService.calculateNetWorth("test-user");

            assertThat(result).containsKey("netWorth");
            assertThat(result).containsKey("totalAssets");
            assertThat(result).containsKey("totalLiabilities");

            Long totalAssets = (Long) result.get("totalAssets");
            Long totalLiabilities = (Long) result.get("totalLiabilities");
            Long netWorth = (Long) result.get("netWorth");

            assertThat(totalAssets).isEqualTo(720000L);
            assertThat(totalLiabilities).isEqualTo(4000000L);
            assertThat(netWorth).isEqualTo(-3280000L);
        }

        @Test
        @DisplayName("should include insurance fund value in assets")
        void shouldIncludeInsuranceFundValue() {
            Insurance ulip = Insurance.builder()
                    .type(Insurance.InsuranceType.ULIP)
                    .fundValue(200000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(ulip));
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = analysisService.calculateNetWorth("test-user");

            Long insuranceFundValue = (Long) result.get("insuranceFundValue");
            assertThat(insuranceFundValue).isEqualTo(200000L);
        }

        @Test
        @DisplayName("should handle empty repositories")
        void shouldHandleEmptyRepositories() {
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = analysisService.calculateNetWorth("test-user");

            assertThat((Long) result.get("netWorth")).isEqualTo(0L);
        }

        @Test
        @DisplayName("should calculate asset breakdown by type")
        void shouldCalculateAssetBreakdown() {
            Investment mf1 = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(100000.0)
                    .build();

            Investment mf2 = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(50000.0)
                    .build();

            Investment stock = Investment.builder()
                    .type(Investment.InvestmentType.STOCK)
                    .currentValue(200000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(mf1, mf2, stock));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = analysisService.calculateNetWorth("test-user");

            @SuppressWarnings("unchecked")
            Map<String, Double> breakdown = (Map<String, Double>) result.get("assetBreakdown");
            assertThat(breakdown).containsEntry("MUTUAL_FUND", 150000.0);
            assertThat(breakdown).containsEntry("STOCK", 200000.0);
        }
    }

    @Nested
    @DisplayName("calculateProjections")
    class CalculateProjections {

        @Test
        @DisplayName("should project investment growth over years")
        void shouldProjectGrowth() {
            Investment mf = Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .currentValue(100000.0)
                    .monthlySip(10000.0)
                    .expectedReturn(12.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(mf));
            when(calculationService.calculateFutureValue(anyDouble(), anyDouble(), anyInt()))
                    .thenAnswer(inv -> {
                        double pv = inv.getArgument(0);
                        double rate = inv.getArgument(1);
                        int years = inv.getArgument(2);
                        return pv * Math.pow(1 + rate / 100, years);
                    });
            when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(500000.0);

            Map<String, Object> result = analysisService.calculateProjections("test-user", 5);

            assertThat(result).containsKey("projections");
            assertThat(result).containsKey("finalValue");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> projections = (List<Map<String, Object>>) result.get("projections");
            assertThat(projections).hasSize(6); // 0 to 5 years
        }

        @Test
        @DisplayName("should handle investments without SIP")
        void shouldHandleWithoutSIP() {
            Investment fd = Investment.builder()
                    .type(Investment.InvestmentType.FD)
                    .currentValue(100000.0)
                    .expectedReturn(7.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(fd));
            when(calculationService.calculateFutureValue(anyDouble(), anyDouble(), anyInt()))
                    .thenAnswer(inv -> {
                        double pv = inv.getArgument(0);
                        double rate = inv.getArgument(1);
                        int years = inv.getArgument(2);
                        return pv * Math.pow(1 + rate / 100, years);
                    });

            Map<String, Object> result = analysisService.calculateProjections("test-user", 3);

            assertThat(result).containsKey("finalValue");
        }
    }

    @Nested
    @DisplayName("generateRecommendations")
    class GenerateRecommendations {

        @Test
        @DisplayName("should recommend emergency fund if cash is low")
        void shouldRecommendEmergencyFund() {
            Investment cash = Investment.builder()
                    .type(Investment.InvestmentType.CASH)
                    .currentValue(50000.0)
                    .build();

            Expense rent = Expense.builder()
                    .monthlyAmount(25000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(cash));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(rent));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserIdAndIsActiveTrue("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = analysisService.generateRecommendations("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) result.get("recommendations");
            boolean hasEmergencyFundRec = recommendations.stream()
                    .anyMatch(r -> r.get("title").toString().contains("Emergency Fund"));
            assertThat(hasEmergencyFundRec).isTrue();
        }

        @Test
        @DisplayName("should recommend health insurance if missing")
        void shouldRecommendHealthInsurance() {
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserIdAndIsActiveTrue("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = analysisService.generateRecommendations("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) result.get("recommendations");
            boolean hasHealthInsuranceRec = recommendations.stream()
                    .anyMatch(r -> r.get("title").toString().contains("Health Insurance"));
            assertThat(hasHealthInsuranceRec).isTrue();
        }

        @Test
        @DisplayName("should recommend SIP if no investments")
        void shouldRecommendSIP() {
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserIdAndIsActiveTrue("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = analysisService.generateRecommendations("test-user");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) result.get("recommendations");
            boolean hasSIPRec = recommendations.stream()
                    .anyMatch(r -> r.get("title").toString().contains("SIP"));
            assertThat(hasSIPRec).isTrue();
        }

        @Test
        @DisplayName("should calculate savings rate")
        void shouldCalculateSavingsRate() {
            Income salary = Income.builder()
                    .source("Salary")
                    .monthlyAmount(100000.0)
                    .isActive(true)
                    .build();

            Expense rent = Expense.builder()
                    .monthlyAmount(30000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(rent));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            when(incomeRepository.findByUserIdAndIsActiveTrue("test-user")).thenReturn(Collections.singletonList(salary));

            Map<String, Object> result = analysisService.generateRecommendations("test-user");

            Long savingsRate = (Long) result.get("savingsRate");
            assertThat(savingsRate).isEqualTo(70L); // (100000-30000)/100000 * 100
        }
    }

    @Nested
    @DisplayName("runMonteCarloSimulation")
    class MonteCarloSimulation {

        @Test
        @DisplayName("should run simulation and return percentiles")
        void shouldReturnPercentiles() {
            Investment mf = Investment.builder()
                    .currentValue(1000000.0)
                    .monthlySip(20000.0)
                    .build();

            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.singletonList(mf));

            Map<String, Object> result = analysisService.runMonteCarloSimulation("test-user", 100, 10);

            assertThat(result).containsKey("percentile10");
            assertThat(result).containsKey("percentile25");
            assertThat(result).containsKey("percentile50");
            assertThat(result).containsKey("percentile75");
            assertThat(result).containsKey("percentile90");
            assertThat(result).containsKey("average");

            // Percentiles should be in increasing order
            Long p10 = (Long) result.get("percentile10");
            Long p50 = (Long) result.get("percentile50");
            Long p90 = (Long) result.get("percentile90");
            assertThat(p10).isLessThan(p50);
            assertThat(p50).isLessThan(p90);
        }
    }

    @Nested
    @DisplayName("getFullSummary")
    class GetFullSummary {

        @Test
        @DisplayName("should return complete summary")
        void shouldReturnCompleteSummary() {
            lenient().when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            lenient().when(insuranceRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            lenient().when(loanRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            lenient().when(expenseRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());
            lenient().when(incomeRepository.findByUserIdAndIsActiveTrue("test-user")).thenReturn(Collections.emptyList());
            lenient().when(goalRepository.findByUserIdOrderByTargetYearAsc("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> result = analysisService.getFullSummary("test-user");

            assertThat(result).containsKeys("netWorth", "projections", "goals", "recommendations");
        }
    }
}
