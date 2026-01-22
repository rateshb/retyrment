package com.retyrment.service;

import com.retyrment.model.*;
import com.retyrment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * Branch coverage tests for AnalysisService
 */
@ExtendWith(MockitoExtension.class)
class AnalysisServiceBranchCoverageTest {

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
        
        // Default empty lists - lenient to avoid UnnecessaryStubbingException
        lenient().when(incomeRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());
        lenient().when(investmentRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());
        lenient().when(loanRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());
        lenient().when(insuranceRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());
        lenient().when(expenseRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());
        lenient().when(goalRepository.findByUserIdOrderByTargetYearAsc(anyString())).thenReturn(Collections.emptyList());
        
        lenient().when(calculationService.calculateFutureValue(any(Double.class), any(Double.class), any(Integer.class)))
                .thenReturn(100000.0);
        lenient().when(calculationService.calculateSIPFutureValue(any(Double.class), any(Double.class), any(Integer.class)))
                .thenReturn(50000.0);
        lenient().when(calculationService.calculateInflatedValue(any(Double.class), any(Double.class), any(Integer.class)))
                .thenReturn(120000.0);
    }

    @Test
    @DisplayName("Calculate net worth with no investments")
    void testNetWorthWithNoInvestments() {
        Map<String, Object> result = analysisService.calculateNetWorth("user1");

        assertThat(result).containsKeys("totalAssets", "totalInvestments", "netWorth");
        assertThat(result.get("totalAssets")).isEqualTo(0L);
    }

    @Test
    @DisplayName("Calculate net worth with investments having null currentValue")
    void testNetWorthWithNullCurrentValue() {
        Investment inv = new Investment();
        inv.setId("inv1");
        inv.setCurrentValue(null);
        inv.setInvestedAmount(10000.0);
        
        when(investmentRepository.findByUserId("user1")).thenReturn(Collections.singletonList(inv));

        Map<String, Object> result = analysisService.calculateNetWorth("user1");

        assertThat(result.get("totalInvestments")).isEqualTo(10000L);
    }

    @Test
    @DisplayName("Calculate net worth with investments having null currentValue and investedAmount")
    void testNetWorthWithAllNullValues() {
        Investment inv = new Investment();
        inv.setId("inv1");
        inv.setCurrentValue(null);
        inv.setInvestedAmount(null);
        
        when(investmentRepository.findByUserId("user1")).thenReturn(Collections.singletonList(inv));

        Map<String, Object> result = analysisService.calculateNetWorth("user1");

        assertThat(result.get("totalInvestments")).isEqualTo(0L);
    }

    @Test
    @DisplayName("Calculate net worth with insurance fund value")
    void testNetWorthWithInsuranceFundValue() {
        Insurance ins = new Insurance();
        ins.setId("ins1");
        ins.setType(Insurance.InsuranceType.ULIP);
        ins.setFundValue(50000.0);
        
        when(insuranceRepository.findByUserId("user1")).thenReturn(Collections.singletonList(ins));

        Map<String, Object> result = analysisService.calculateNetWorth("user1");

        assertThat(result.get("insuranceFundValue")).isEqualTo(50000L);
    }

    @Test
    @DisplayName("Calculate net worth with liabilities")
    void testNetWorthWithLiabilities() {
        Loan loan = new Loan();
        loan.setId("loan1");
        loan.setOutstandingAmount(200000.0);
        
        when(loanRepository.findByUserId("user1")).thenReturn(Collections.singletonList(loan));

        Map<String, Object> result = analysisService.calculateNetWorth("user1");

        assertThat(result.get("totalLiabilities")).isEqualTo(200000L);
        assertThat(result.get("netWorth")).isEqualTo(-200000L);
    }

    @Test
    @DisplayName("Calculate net worth with liabilities having null outstanding amount")
    void testNetWorthWithNullOutstandingAmount() {
        Loan loan = new Loan();
        loan.setId("loan1");
        loan.setOutstandingAmount(null);
        
        when(loanRepository.findByUserId("user1")).thenReturn(Collections.singletonList(loan));

        Map<String, Object> result = analysisService.calculateNetWorth("user1");

        assertThat(result.get("totalLiabilities")).isEqualTo(0L);
    }

    @Test
    @DisplayName("Asset breakdown with null type")
    void testAssetBreakdownWithNullType() {
        Investment inv = new Investment();
        inv.setId("inv1");
        inv.setType(null);
        inv.setCurrentValue(10000.0);
        
        when(investmentRepository.findByUserId("user1")).thenReturn(Collections.singletonList(inv));

        Map<String, Object> result = analysisService.calculateNetWorth("user1");

        @SuppressWarnings("unchecked")
        Map<String, Double> breakdown = (Map<String, Double>) result.get("assetBreakdown");
        assertThat(breakdown).containsKey("OTHER");
    }

    @Test
    @DisplayName("Calculate projections with null expected return")
    void testProjectionsWithNullExpectedReturn() {
        Investment inv = new Investment();
        inv.setId("inv1");
        inv.setCurrentValue(100000.0);
        inv.setExpectedReturn(null);
        
        when(investmentRepository.findByUserId("user1")).thenReturn(Collections.singletonList(inv));

        Map<String, Object> result = analysisService.calculateProjections("user1", 5);

        assertThat(result).containsKeys("projections", "finalValue");
    }

    @Test
    @DisplayName("Calculate projections with SIP")
    void testProjectionsWithSIP() {
        Investment inv = new Investment();
        inv.setId("inv1");
        inv.setCurrentValue(100000.0);
        inv.setMonthlySip(5000.0);
        inv.setExpectedReturn(12.0);
        
        when(investmentRepository.findByUserId("user1")).thenReturn(Collections.singletonList(inv));

        Map<String, Object> result = analysisService.calculateProjections("user1", 5);

        assertThat(result).containsKeys("projections", "finalValue");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> projections = (List<Map<String, Object>>) result.get("projections");
        assertThat(projections).isNotEmpty();
    }

    @Test
    @DisplayName("Calculate projections with zero SIP")
    void testProjectionsWithZeroSIP() {
        Investment inv = new Investment();
        inv.setId("inv1");
        inv.setCurrentValue(100000.0);
        inv.setMonthlySip(0.0);
        inv.setExpectedReturn(12.0);
        
        when(investmentRepository.findByUserId("user1")).thenReturn(Collections.singletonList(inv));

        Map<String, Object> result = analysisService.calculateProjections("user1", 5);

        assertThat(result).containsKey("projections");
    }

    @Test
    @DisplayName("Calculate projections with no investments")
    void testProjectionsWithNoInvestments() {
        Map<String, Object> result = analysisService.calculateProjections("user1", 5);

        assertThat(result).containsKey("projections");
        assertThat(result).containsKey("finalValue");
    }

    @Test
    @DisplayName("Analyze goals with FUNDED status")
    void testAnalyzeGoalsWithFundedStatus() {
        Goal goal = new Goal();
        goal.setId("goal1");
        goal.setName("Test Goal");
        goal.setTargetAmount(100000.0);
        goal.setTargetYear(2030);
        goal.setPriority(Goal.Priority.HIGH);
        
        when(goalRepository.findByUserIdOrderByTargetYearAsc("user1")).thenReturn(Collections.singletonList(goal));
        when(calculationService.calculateInflatedValue(any(Double.class), any(Double.class), any(Integer.class)))
                .thenReturn(100000.0);

        Map<String, Object> result = analysisService.analyzeGoals("user1");

        assertThat(result).containsKey("goals");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> goals = (List<Map<String, Object>>) result.get("goals");
        assertThat(goals).isNotEmpty();
    }

    @Test
    @DisplayName("Analyze goals with PARTIAL status")
    void testAnalyzeGoalsWithPartialStatus() {
        Goal goal = new Goal();
        goal.setId("goal1");
        goal.setName("Test Goal");
        goal.setTargetAmount(100000.0);
        goal.setTargetYear(2030);
        goal.setPriority(Goal.Priority.MEDIUM);
        
        when(goalRepository.findByUserIdOrderByTargetYearAsc("user1")).thenReturn(Collections.singletonList(goal));
        when(calculationService.calculateInflatedValue(any(Double.class), any(Double.class), any(Integer.class)))
                .thenReturn(200000.0); // Double the target, so funding percent will be 50%

        Map<String, Object> result = analysisService.analyzeGoals("user1");

        assertThat(result).containsKey("goals");
    }

    @Test
    @DisplayName("Analyze goals with UNFUNDED status")
    void testAnalyzeGoalsWithUnfundedStatus() {
        Goal goal = new Goal();
        goal.setId("goal1");
        goal.setName("Test Goal");
        goal.setTargetAmount(100000.0);
        goal.setTargetYear(2030);
        goal.setPriority(Goal.Priority.LOW);
        
        when(goalRepository.findByUserIdOrderByTargetYearAsc("user1")).thenReturn(Collections.singletonList(goal));
        when(calculationService.calculateInflatedValue(any(Double.class), any(Double.class), any(Integer.class)))
                .thenReturn(500000.0); // 5x target, so funding percent will be 20%

        Map<String, Object> result = analysisService.analyzeGoals("user1");

        assertThat(result).containsKey("goals");
    }

    @Test
    @DisplayName("Analyze goals with null target amount")
    void testAnalyzeGoalsWithNullTargetAmount() {
        Goal goal = new Goal();
        goal.setId("goal1");
        goal.setName("Test Goal");
        goal.setTargetAmount(null);
        goal.setTargetYear(2030);
        goal.setPriority(Goal.Priority.HIGH);
        
        when(goalRepository.findByUserIdOrderByTargetYearAsc("user1")).thenReturn(Collections.singletonList(goal));

        Map<String, Object> result = analysisService.analyzeGoals("user1");

        assertThat(result).containsKey("goals");
    }

    @Test
    @DisplayName("Analyze goals with zero inflated amount")
    void testAnalyzeGoalsWithZeroInflatedAmount() {
        Goal goal = new Goal();
        goal.setId("goal1");
        goal.setName("Test Goal");
        goal.setTargetAmount(100000.0);
        goal.setTargetYear(2030);
        goal.setPriority(Goal.Priority.HIGH);
        
        when(goalRepository.findByUserIdOrderByTargetYearAsc("user1")).thenReturn(Collections.singletonList(goal));
        when(calculationService.calculateInflatedValue(any(Double.class), any(Double.class), any(Integer.class)))
                .thenReturn(0.0);

        Map<String, Object> result = analysisService.analyzeGoals("user1");

        assertThat(result).containsKey("goals");
    }

    @Test
    @DisplayName("Analyze goals with zero total goals value")
    void testAnalyzeGoalsWithZeroTotalGoalsValue() {
        Goal goal = new Goal();
        goal.setId("goal1");
        goal.setName("Test Goal");
        goal.setTargetAmount(0.0);
        goal.setTargetYear(2030);
        goal.setPriority(Goal.Priority.HIGH);
        
        when(goalRepository.findByUserIdOrderByTargetYearAsc("user1")).thenReturn(Collections.singletonList(goal));

        Map<String, Object> result = analysisService.analyzeGoals("user1");

        assertThat(result).containsKey("goals");
    }

    @Test
    @DisplayName("Analyze goals with multiple goals")
    void testAnalyzeGoalsWithMultipleGoals() {
        Goal goal1 = new Goal();
        goal1.setId("goal1");
        goal1.setName("Goal 1");
        goal1.setTargetAmount(100000.0);
        goal1.setTargetYear(2030);
        goal1.setPriority(Goal.Priority.HIGH);

        Goal goal2 = new Goal();
        goal2.setId("goal2");
        goal2.setName("Goal 2");
        goal2.setTargetAmount(200000.0);
        goal2.setTargetYear(2035);
        goal2.setPriority(Goal.Priority.MEDIUM);
        
        when(goalRepository.findByUserIdOrderByTargetYearAsc("user1")).thenReturn(Arrays.asList(goal1, goal2));

        Map<String, Object> result = analysisService.analyzeGoals("user1");

        assertThat(result).containsKey("goals");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> goals = (List<Map<String, Object>>) result.get("goals");
        assertThat(goals).hasSize(2);
    }

    @Test
    @DisplayName("Calculate recommendations with no data")
    void testRecommendationsWithNoData() {
        Map<String, Object> result = analysisService.generateRecommendations("user1");

        assertThat(result).containsKey("recommendations");
    }

    @Test
    @DisplayName("Calculate recommendations with investments")
    void testRecommendationsWithInvestments() {
        Investment inv = new Investment();
        inv.setId("inv1");
        inv.setType(Investment.InvestmentType.MUTUAL_FUND);
        inv.setCurrentValue(100000.0);
        
        when(investmentRepository.findByUserId("user1")).thenReturn(Collections.singletonList(inv));

        Map<String, Object> result = analysisService.generateRecommendations("user1");

        assertThat(result).containsKey("recommendations");
    }

    @Test
    @DisplayName("Calculate recommendations with active income")
    void testRecommendationsWithActiveIncome() {
        Income income = new Income();
        income.setId("inc1");
        income.setSource("Salary");
        income.setMonthlyAmount(50000.0);
        income.setIsActive(true);
        
        lenient().when(incomeRepository.findByUserId("user1")).thenReturn(Collections.singletonList(income));

        Map<String, Object> result = analysisService.generateRecommendations("user1");

        assertThat(result).containsKey("recommendations");
    }

    @Test
    @DisplayName("Calculate recommendations with inactive income")
    void testRecommendationsWithInactiveIncome() {
        Income income = new Income();
        income.setId("inc1");
        income.setSource("Salary");
        income.setMonthlyAmount(50000.0);
        income.setIsActive(false);
        
        lenient().when(incomeRepository.findByUserId("user1")).thenReturn(Collections.singletonList(income));

        Map<String, Object> result = analysisService.generateRecommendations("user1");

        assertThat(result).containsKey("recommendations");
    }

    @Test
    @DisplayName("Calculate recommendations with null income isActive")
    void testRecommendationsWithNullIncomeActive() {
        Income income = new Income();
        income.setId("inc1");
        income.setSource("Salary");
        income.setMonthlyAmount(50000.0);
        income.setIsActive(null);
        
        lenient().when(incomeRepository.findByUserId("user1")).thenReturn(Collections.singletonList(income));

        Map<String, Object> result = analysisService.generateRecommendations("user1");

        assertThat(result).containsKey("recommendations");
    }

    @Test
    @DisplayName("Calculate recommendations with loans")
    void testRecommendationsWithLoans() {
        Loan loan = new Loan();
        loan.setId("loan1");
        loan.setType(Loan.LoanType.HOME);
        loan.setOutstandingAmount(500000.0);
        loan.setEmi(10000.0);
        
        lenient().when(loanRepository.findByUserId("user1")).thenReturn(Collections.singletonList(loan));

        Map<String, Object> result = analysisService.generateRecommendations("user1");

        assertThat(result).containsKey("recommendations");
    }

    @Test
    @DisplayName("Calculate recommendations with expenses")
    void testRecommendationsWithExpenses() {
        Expense expense = new Expense();
        expense.setId("exp1");
        expense.setCategory(Expense.ExpenseCategory.RENT);
        expense.setMonthlyAmount(15000.0);
        
        when(expenseRepository.findByUserId("user1")).thenReturn(Collections.singletonList(expense));

        Map<String, Object> result = analysisService.generateRecommendations("user1");

        assertThat(result).containsKey("recommendations");
    }

    @Test
    @DisplayName("Calculate recommendations with emergency fund tagged investments")
    void testRecommendationsWithEmergencyFund() {
        Investment fd = new Investment();
        fd.setId("fd1");
        fd.setType(Investment.InvestmentType.FD);
        fd.setCurrentValue(50000.0);
        fd.setIsEmergencyFund(true);
        
        when(investmentRepository.findByUserId("user1")).thenReturn(Collections.singletonList(fd));

        Map<String, Object> result = analysisService.generateRecommendations("user1");

        assertThat(result).containsKey("recommendations");
    }
}
