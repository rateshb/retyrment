package com.retyrment.service;

import com.retyrment.model.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RetirementService - Withdrawal Strategy Tests")
class RetirementServiceWithdrawalStrategyTest {

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
                    double monthlyRate = rate / 100 / 12;
                    int months = years * 12;
                    return sip * ((Math.pow(1 + monthlyRate, months) - 1) / monthlyRate) * (1 + monthlyRate);
                });
    }

    @Nested
    @DisplayName("Generate Withdrawal Strategy")
    class GenerateWithdrawalStrategyTests {

        @Test
        @DisplayName("Should generate withdrawal strategy with all investment types")
        void shouldGenerateWithdrawalStrategyWithAllTypes() {
            // Given
            List<Investment> investments = Arrays.asList(
                    createInvestment("Cash Account", Investment.InvestmentType.CASH, 100000.0, 3.5),
                    createInvestment("Fixed Deposit", Investment.InvestmentType.FD, 500000.0, 6.5),
                    createInvestment("Stocks", Investment.InvestmentType.STOCK, 200000.0, 12.0),
                    createInvestment("EPF", Investment.InvestmentType.EPF, 1000000.0, 8.1),
                    createInvestment("NPS", Investment.InvestmentType.NPS, 500000.0, 10.0),
                    createInvestment("Mutual Fund", Investment.InvestmentType.MUTUAL_FUND, 300000.0, 10.0),
                    createInvestment("PPF", Investment.InvestmentType.PPF, 400000.0, 7.1),
                    createInvestment("Gold", Investment.InvestmentType.GOLD, 200000.0, 8.0),
                    createInvestment("Property", Investment.InvestmentType.REAL_ESTATE, 5000000.0, 7.0)
            );
            
            List<Expense> expenses = Arrays.asList(
                    createExpense("Rent", 30000.0, Expense.ExpenseFrequency.MONTHLY),
                    createExpense("Groceries", 15000.0, Expense.ExpenseFrequency.MONTHLY)
            );
            
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(investments);
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(expenses);

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).containsKey("phases");
            assertThat(result).containsKey("summary");
            assertThat(result).containsKey("withdrawalSchedule");
            assertThat(result).containsKey("taxOptimizationTips");
            assertThat(result).containsKey("importantNotes");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> phases = (List<Map<String, Object>>) result.get("phases");
            assertThat(phases).hasSize(3);
            
            // Verify phase 1 (Taxable & Liquid)
            Map<String, Object> phase1 = phases.get(0);
            assertThat(phase1.get("name")).isEqualTo("Taxable & Liquid Assets");
            assertThat(phase1.get("priority")).isEqualTo(1);
            
            // Verify phase 2 (Tax-Deferred)
            Map<String, Object> phase2 = phases.get(1);
            assertThat(phase2.get("name")).isEqualTo("Tax-Deferred Accounts");
            assertThat(phase2.get("priority")).isEqualTo(2);
            
            // Verify phase 3 (Tax-Free)
            Map<String, Object> phase3 = phases.get(2);
            assertThat(phase3.get("name")).isEqualTo("Tax-Free & Long-Term");
            assertThat(phase3.get("priority")).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle empty investments")
        void shouldHandleEmptyInvestments() {
            // Given
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            assertThat(result).isNotNull();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            assertThat(summary.get("totalCorpusAtRetirement")).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should categorize crypto into phase 1")
        void shouldCategorizeCryptoIntoPhase1() {
            // Given
            List<Investment> investments = Arrays.asList(
                    createInvestment("Bitcoin", Investment.InvestmentType.CRYPTO, 100000.0, 15.0)
            );
            
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(investments);
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> phases = (List<Map<String, Object>>) result.get("phases");
            
            Map<String, Object> phase1 = phases.get(0);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> phase1Assets = (List<Map<String, Object>>) phase1.get("assets");
            
            assertThat(phase1Assets).anyMatch(a -> "CRYPTO".equals(a.get("type")));
        }

        @Test
        @DisplayName("Should handle different expense frequencies")
        void shouldHandleDifferentExpenseFrequencies() {
            // Given
            List<Investment> investments = Arrays.asList(
                    createInvestment("FD", Investment.InvestmentType.FD, 1000000.0, 6.5)
            );
            
            List<Expense> expenses = Arrays.asList(
                    createExpense("Monthly", 10000.0, Expense.ExpenseFrequency.MONTHLY),
                    createExpense("Quarterly", 30000.0, Expense.ExpenseFrequency.QUARTERLY),
                    createExpense("Half Yearly", 60000.0, Expense.ExpenseFrequency.HALF_YEARLY),
                    createExpense("Yearly", 120000.0, Expense.ExpenseFrequency.YEARLY),
                    createExpense("One Time", 50000.0, Expense.ExpenseFrequency.ONE_TIME)
            );
            
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(investments);
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(expenses);

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            assertThat(result).isNotNull();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            // Monthly expenses: 10000 + 30000/3 + 60000/6 + 120000/12 + 0 = 10000 + 10000 + 10000 + 10000 = 40000
            assertThat(((Number) summary.get("monthlyExpenseAtRetirement")).longValue()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should calculate sustainability status")
        void shouldCalculateSustainabilityStatus() {
            // Given - large corpus, small expenses -> sustainable
            List<Investment> investments = Arrays.asList(
                    createInvestment("PPF", Investment.InvestmentType.PPF, 10000000.0, 7.1)
            );
            
            List<Expense> expenses = Arrays.asList(
                    createExpense("Basic", 10000.0, Expense.ExpenseFrequency.MONTHLY)
            );
            
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(investments);
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(expenses);

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            assertThat(summary.get("isSustainable")).isEqualTo(true);
        }

        @Test
        @DisplayName("Should generate withdrawal schedule")
        void shouldGenerateWithdrawalSchedule() {
            // Given
            List<Investment> investments = Arrays.asList(
                    createInvestment("FD", Investment.InvestmentType.FD, 500000.0, 6.5),
                    createInvestment("EPF", Investment.InvestmentType.EPF, 1000000.0, 8.1)
            );
            
            List<Expense> expenses = Arrays.asList(
                    createExpense("Basic", 50000.0, Expense.ExpenseFrequency.MONTHLY)
            );
            
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(investments);
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(expenses);

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> schedule = (List<Map<String, Object>>) result.get("withdrawalSchedule");
            assertThat(schedule).isNotEmpty();
            
            Map<String, Object> year1 = schedule.get(0);
            assertThat(year1).containsKey("year");
            assertThat(year1).containsKey("age");
            assertThat(year1).containsKey("yearlyExpense");
            assertThat(year1).containsKey("withdrawFrom");
            assertThat(year1.get("year")).isEqualTo(1);
            assertThat(year1.get("age")).isEqualTo(60);
        }

        @Test
        @DisplayName("Should include tax optimization tips")
        void shouldIncludeTaxOptimizationTips() {
            // Given
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tips = (List<Map<String, Object>>) result.get("taxOptimizationTips");
            assertThat(tips).hasSize(4);
            assertThat(tips.get(0).get("title")).isEqualTo("Stay under ₹7L taxable income");
        }

        @Test
        @DisplayName("Should include important notes")
        void shouldIncludeImportantNotes() {
            // Given
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            @SuppressWarnings("unchecked")
            List<String> notes = (List<String>) result.get("importantNotes");
            assertThat(notes).hasSize(5);
            assertThat(notes.get(0)).contains("1-2 years expenses");
        }

        @Test
        @DisplayName("Should handle investment with null type")
        void shouldHandleInvestmentWithNullType() {
            // Given
            Investment inv = new Investment();
            inv.setId("inv-1");
            inv.setName("Unknown");
            inv.setType(null);
            inv.setCurrentValue(100000.0);
            inv.setExpectedReturn(7.0);
            
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(inv));
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> phases = (List<Map<String, Object>>) result.get("phases");
            Map<String, Object> phase2 = phases.get(1);
            assertThat(((Number) phase2.get("total")).longValue()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle RD investments")
        void shouldHandleRdInvestments() {
            // Given
            List<Investment> investments = Arrays.asList(
                    createInvestment("RD Account", Investment.InvestmentType.RD, 50000.0, 6.0)
            );
            
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(investments);
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> phases = (List<Map<String, Object>>) result.get("phases");
            Map<String, Object> phase1 = phases.get(0);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> phase1Assets = (List<Map<String, Object>>) phase1.get("assets");
            assertThat(phase1Assets).anyMatch(a -> "RD".equals(a.get("type")));
        }

        @Test
        @DisplayName("Should handle investments with SIP")
        void shouldHandleInvestmentsWithSip() {
            // Given
            Investment inv = new Investment();
            inv.setId("inv-1");
            inv.setName("MF with SIP");
            inv.setType(Investment.InvestmentType.MUTUAL_FUND);
            inv.setCurrentValue(100000.0);
            inv.setMonthlySip(5000.0);
            inv.setExpectedReturn(10.0);
            
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(inv));
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            assertThat(((Number) summary.get("totalCorpusAtRetirement")).longValue()).isGreaterThan(100000);
        }

        @Test
        @DisplayName("Should handle investment with only invested amount")
        void shouldHandleInvestmentWithOnlyInvestedAmount() {
            // Given
            Investment inv = new Investment();
            inv.setId("inv-1");
            inv.setName("New Investment");
            inv.setType(Investment.InvestmentType.FD);
            inv.setCurrentValue(null);
            inv.setInvestedAmount(100000.0);
            inv.setExpectedReturn(6.5);
            
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(inv));
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");
            assertThat(((Number) summary.get("totalCorpusAtRetirement")).longValue()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle null expense frequency")
        void shouldHandleNullExpenseFrequency() {
            // Given
            Expense expense = new Expense();
            expense.setAmount(10000.0);
            expense.setFrequency(null);
            
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Arrays.asList(expense));

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Asset Phase Categorization")
    class AssetPhaseCategorization {

        @Test
        @DisplayName("Should put NPS in phase 2 with mandatory annuity info")
        void shouldPutNpsInPhase2WithAnnuityInfo() {
            // Given
            List<Investment> investments = Arrays.asList(
                    createInvestment("NPS", Investment.InvestmentType.NPS, 500000.0, 10.0)
            );
            
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(investments);
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> phases = (List<Map<String, Object>>) result.get("phases");
            Map<String, Object> phase2 = phases.get(1);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> phase2Assets = (List<Map<String, Object>>) phase2.get("assets");
            
            assertThat(phase2Assets).anyMatch(a -> 
                "NPS".equals(a.get("type")) && 
                Integer.valueOf(40).equals(a.get("mandatoryAnnuityPercent"))
            );
        }

        @Test
        @DisplayName("Should include withdrawal tips for each asset")
        void shouldIncludeWithdrawalTipsForEachAsset() {
            // Given
            List<Investment> investments = Arrays.asList(
                    createInvestment("PPF", Investment.InvestmentType.PPF, 100000.0, 7.1),
                    createInvestment("Stocks", Investment.InvestmentType.STOCK, 100000.0, 12.0)
            );
            
            when(investmentRepository.findByUserId(USER_ID)).thenReturn(investments);
            when(expenseRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = retirementService.generateWithdrawalStrategy(USER_ID, 35, 60, 85);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> phases = (List<Map<String, Object>>) result.get("phases");
            
            // Check PPF has withdrawal tip
            Map<String, Object> phase3 = phases.get(2);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> phase3Assets = (List<Map<String, Object>>) phase3.get("assets");
            assertThat(phase3Assets).anyMatch(a -> 
                "PPF".equals(a.get("type")) && 
                a.get("withdrawalTip") != null &&
                a.get("withdrawalTip").toString().contains("5-year blocks")
            );
        }
    }

    // Helper methods
    private Investment createInvestment(String name, Investment.InvestmentType type, double currentValue, double expectedReturn) {
        Investment inv = new Investment();
        inv.setId(UUID.randomUUID().toString());
        inv.setName(name);
        inv.setType(type);
        inv.setCurrentValue(currentValue);
        inv.setExpectedReturn(expectedReturn);
        return inv;
    }

    private Expense createExpense(String name, double amount, Expense.ExpenseFrequency frequency) {
        Expense expense = new Expense();
        expense.setId(UUID.randomUUID().toString());
        expense.setName(name);
        expense.setAmount(amount);
        expense.setFrequency(frequency);
        return expense;
    }
}
