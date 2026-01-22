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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Additional tests to improve branch coverage for RetirementService
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetirementServiceBranchCoverageTest {

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
    @Mock
    private CalculationService calculationService;

    @InjectMocks
    private RetirementService retirementService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(retirementService, "defaultEpfReturn", 8.15);
        ReflectionTestUtils.setField(retirementService, "defaultPpfReturn", 7.1);
        ReflectionTestUtils.setField(retirementService, "defaultMfReturn", 12.0);
        ReflectionTestUtils.setField(retirementService, "calculationService", calculationService);
        
        // Default mocks - lenient to avoid UnnecessaryStubbingException
        lenient().when(investmentRepository.findByType(any())).thenReturn(Collections.emptyList());
        lenient().when(goalRepository.findByUserIdOrderByTargetYearAsc(any())).thenReturn(Collections.emptyList());
        lenient().when(insuranceRepository.findByTypeIn(any())).thenReturn(Collections.emptyList());
        lenient().when(loanRepository.findByUserId(any())).thenReturn(Collections.emptyList());
        lenient().when(incomeRepository.findByUserId(any())).thenReturn(Collections.emptyList());
        lenient().when(expenseRepository.findByUserId(any())).thenReturn(Collections.emptyList());
        lenient().when(scenarioRepository.findByUserIdAndIsDefaultTrue(any())).thenReturn(Optional.empty());
        lenient().when(calculationService.calculateSIPFutureValue(anyDouble(), anyDouble(), anyInt())).thenReturn(100000.0);
    }

    @Test
    @DisplayName("Test SIMPLE_DEPLETION income strategy")
    void testSimpleDepletionStrategy() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .incomeStrategy("SIMPLE_DEPLETION")
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKeys("matrix", "summary", "gapAnalysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary.get("incomeStrategy")).isEqualTo("SIMPLE_DEPLETION");
    }

    @Test
    @DisplayName("Test SAFE_4_PERCENT income strategy")
    void testSafe4PercentStrategy() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .incomeStrategy("SAFE_4_PERCENT")
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKeys("matrix", "summary", "gapAnalysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary.get("incomeStrategy")).isEqualTo("SAFE_4_PERCENT");
    }

    @Test
    @DisplayName("Test SUSTAINABLE income strategy (default)")
    void testSustainableStrategy() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .incomeStrategy("SUSTAINABLE")
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKeys("matrix", "summary", "gapAnalysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary.get("incomeStrategy")).isEqualTo("SUSTAINABLE");
    }

    @Test
    @DisplayName("Test with null income strategy (should default to SUSTAINABLE)")
    void testNullIncomeStrategy() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .incomeStrategy(null)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKeys("matrix", "summary", "gapAnalysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary.get("incomeStrategy")).isEqualTo("SUSTAINABLE");
    }

    @Test
    @DisplayName("Test rate reduction enabled")
    void testRateReductionEnabled() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .enableRateReduction(true)
                .rateReductionPercent(0.5)
                .rateReductionYears(5)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKey("matrix");
    }

    @Test
    @DisplayName("Test rate reduction disabled")
    void testRateReductionDisabled() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .enableRateReduction(false)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKey("matrix");
    }

    @Test
    @DisplayName("Test SIP step-up with zero SIP amount")
    void testSipStepUpWithZeroSip() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .sipStepUpPercent(10.0)
                .build();

        // No MF investments, so SIP = 0
        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKeys("matrix", "summary", "gapAnalysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary).containsKey("sipStepUpOptimization");
    }

    @Test
    @DisplayName("Test SIP step-up with negative step-up percent")
    void testSipStepUpWithNegativePercent() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .sipStepUpPercent(-5.0)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKeys("matrix", "summary", "gapAnalysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary).containsKey("sipStepUpOptimization");
    }

    @Test
    @DisplayName("Test effective from year in the past")
    void testEffectiveFromYearInPast() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .effectiveFromYear(0)
                .sipStepUpPercent(10.0)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKey("matrix");
    }

    @Test
    @DisplayName("Test effective from year in the future")
    void testEffectiveFromYearInFuture() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .effectiveFromYear(10)
                .sipStepUpPercent(10.0)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKey("matrix");
    }

    @Test
    @DisplayName("Test with MF investments and SIP")
    void testWithMfInvestmentsAndSip() {
        Investment mf = new Investment();
        mf.setId("mf1");
        mf.setName("Test MF");
        mf.setType(Investment.InvestmentType.MUTUAL_FUND);
        mf.setCurrentValue(100000.0);
        mf.setMonthlySip(5000.0);
        mf.setExpectedReturn(12.0);

        when(investmentRepository.findByType(Investment.InvestmentType.MUTUAL_FUND))
                .thenReturn(Collections.singletonList(mf));

        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .sipStepUpPercent(10.0)
                .effectiveFromYear(1)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKeys("matrix", "summary", "gapAnalysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary).containsKey("sipStepUpOptimization");
        @SuppressWarnings("unchecked")
        Map<String, Object> optimization = (Map<String, Object>) summary.get("sipStepUpOptimization");
        assertThat(optimization).containsKey("sipAtStart");
        assertThat(optimization).containsKey("sipAtFullStepUp");
    }

    @Test
    @DisplayName("Test with EPF investments")
    void testWithEpfInvestments() {
        Investment epf = new Investment();
        epf.setId("epf1");
        epf.setName("EPF");
        epf.setType(Investment.InvestmentType.EPF);
        epf.setCurrentValue(200000.0);
        epf.setYearlyContribution(50000.0);

        when(investmentRepository.findByType(Investment.InvestmentType.EPF))
                .thenReturn(Collections.singletonList(epf));

        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKey("matrix");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary).containsKey("startingBalances");
    }

    @Test
    @DisplayName("Test with PPF investments")
    void testWithPpfInvestments() {
        Investment ppf = new Investment();
        ppf.setId("ppf1");
        ppf.setName("PPF");
        ppf.setType(Investment.InvestmentType.PPF);
        ppf.setCurrentValue(150000.0);
        ppf.setYearlyContribution(100000.0);

        when(investmentRepository.findByType(Investment.InvestmentType.PPF))
                .thenReturn(Collections.singletonList(ppf));

        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKey("matrix");
    }

    @Test
    @DisplayName("Test with NPS investments")
    void testWithNpsInvestments() {
        Investment nps = new Investment();
        nps.setId("nps1");
        nps.setName("NPS");
        nps.setType(Investment.InvestmentType.NPS);
        nps.setCurrentValue(100000.0);
        nps.setYearlyContribution(50000.0);

        when(investmentRepository.findByType(Investment.InvestmentType.NPS))
                .thenReturn(Collections.singletonList(nps));

        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKey("matrix");
    }

    @Test
    @DisplayName("Test with goals")
    void testWithGoals() {
        Goal goal = new Goal();
        goal.setId("goal1");
        goal.setName("Test Goal");
        goal.setTargetAmount(500000.0);
        goal.setTargetYear(2030);
        goal.setPriority(Goal.Priority.HIGH);

        when(goalRepository.findByUserIdOrderByTargetYearAsc("user1"))
                .thenReturn(Collections.singletonList(goal));

        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKey("matrix");
    }

    @Test
    @DisplayName("Test with recurring goals")
    void testWithRecurringGoals() {
        Goal recurringGoal = new Goal();
        recurringGoal.setId("goal2");
        recurringGoal.setName("Recurring Goal");
        recurringGoal.setTargetAmount(100000.0);
        recurringGoal.setTargetYear(2030);
        recurringGoal.setIsRecurring(true);
        recurringGoal.setRecurrenceInterval(5);
        recurringGoal.setRecurrenceEndYear(2050);
        recurringGoal.setPriority(Goal.Priority.MEDIUM);

        when(goalRepository.findByUserIdOrderByTargetYearAsc("user1"))
                .thenReturn(Collections.singletonList(recurringGoal));

        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKey("matrix");
    }

    @Test
    @DisplayName("Test with annuity insurance")
    void testWithAnnuityInsurance() {
        Insurance annuity = new Insurance();
        annuity.setId("ann1");
        annuity.setType(Insurance.InsuranceType.ANNUITY);
        annuity.setCompany("Test Company");
        annuity.setPolicyName("Annuity Policy");
        annuity.setStartDate(LocalDate.of(2020, 1, 1));
        annuity.setAnnuityStartYear(2035);
        annuity.setMonthlyAnnuityAmount(10000.0);
        annuity.setAnnuityGrowthRate(3.0);

        when(insuranceRepository.findByTypeIn(any()))
                .thenReturn(Collections.singletonList(annuity));

        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKeys("matrix", "summary", "gapAnalysis");
    }

    @Test
    @DisplayName("Test with null lumpsum amount")
    void testWithNullLumpsumAmount() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .lumpsumAmount(null)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKeys("matrix", "summary", "gapAnalysis");
    }

    @Test
    @DisplayName("Test with custom corpus return rate")
    void testWithCustomCorpusReturnRate() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .corpusReturnRate(8.0)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKeys("matrix", "summary", "gapAnalysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary.get("corpusReturnRate")).isEqualTo(8.0);
    }

    @Test
    @DisplayName("Test with custom withdrawal rate")
    void testWithCustomWithdrawalRate() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .withdrawalRate(6.0)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKeys("matrix", "summary", "gapAnalysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertThat(summary.get("withdrawalRate")).isEqualTo(6.0);
    }

    @Test
    @DisplayName("Test maturing investments before retirement")
    void testMaturingInvestments() {
        Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("user1", 35, 60);

        assertThat(result).containsKeys("maturingInvestments", "maturingInsurance", "moneyBackPayouts", 
                "totalMaturingBeforeRetirement", "investmentCount", "insuranceCount", "moneyBackCount", "retirementDate");
    }

    @Test
    @DisplayName("Test maturing with PPF investments")
    void testMaturingWithPpf() {
        Investment ppf = new Investment();
        ppf.setId("ppf1");
        ppf.setName("PPF");
        ppf.setType(Investment.InvestmentType.PPF);
        ppf.setCurrentValue(150000.0);
        ppf.setMaturityDate(LocalDate.of(2030, 12, 31));

        lenient().when(investmentRepository.findByUserId("user1"))
                .thenReturn(Collections.singletonList(ppf));

        Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("user1", 35, 60);

        assertThat(result).containsKey("maturingInvestments");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> maturingInvestments = (List<Map<String, Object>>) result.get("maturingInvestments");
        assertThat(maturingInvestments).isNotEmpty();
    }

    @Test
    @DisplayName("Test maturing with insurance policies")
    void testMaturingWithInsurance() {
        Insurance ulip = new Insurance();
        ulip.setId("ulip1");
        ulip.setType(Insurance.InsuranceType.ULIP);
        ulip.setCompany("Test Company");
        ulip.setPolicyName("ULIP");
        ulip.setStartDate(LocalDate.of(2020, 1, 1));
        ulip.setMaturityDate(LocalDate.of(2035, 12, 31));
        ulip.setMaturityBenefit(500000.0);
        ulip.setPolicyTerm(15);

        lenient().when(insuranceRepository.findByUserId("user1"))
                .thenReturn(Collections.singletonList(ulip));

        Map<String, Object> result = retirementService.calculateMaturingBeforeRetirement("user1", 35, 60);

        assertThat(result).containsKey("maturingInsurance");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> maturingInsurance = (List<Map<String, Object>>) result.get("maturingInsurance");
        assertThat(maturingInsurance).isNotEmpty();
    }

    @Test
    @DisplayName("Test with zero years to retirement")
    void testWithZeroYearsToRetirement() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(60)
                .retirementAge(60)
                .lifeExpectancy(85)
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKey("matrix");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matrix = (List<Map<String, Object>>) result.get("matrix");
        // Matrix should still have post-retirement years
        assertThat(matrix).isNotEmpty();
    }

    @Test
    @DisplayName("Test with very short life expectancy")
    void testWithShortLifeExpectancy() {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(65) // Only 5 years post-retirement
                .build();

        Map<String, Object> result = retirementService.generateRetirementMatrix("user1", scenario);

        assertThat(result).containsKey("matrix");
    }
}
