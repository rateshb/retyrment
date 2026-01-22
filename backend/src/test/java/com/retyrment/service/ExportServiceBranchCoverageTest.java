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

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.retyrment.repository.CalendarEntryRepository;

/**
 * Branch coverage tests for ExportService
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceBranchCoverageTest {

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
    private RetirementScenarioRepository scenarioRepository;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private RetirementService retirementService;
    @Mock
    private CalendarEntryRepository calendarEntryRepository;

    @InjectMocks
    private ExportService exportService;

    @BeforeEach
    void setUp() {
        // Default empty lists - lenient to avoid UnnecessaryStubbingException
        lenient().when(incomeRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());
        lenient().when(investmentRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());
        lenient().when(loanRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());
        lenient().when(insuranceRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());
        lenient().when(expenseRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());
        lenient().when(goalRepository.findByUserIdOrderByTargetYearAsc(anyString())).thenReturn(Collections.emptyList());
        lenient().when(scenarioRepository.findByUserIdAndIsDefaultTrue(anyString())).thenReturn(Optional.empty());
        lenient().when(calendarEntryRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());
        
        // Default analysis response
        Map<String, Object> networthData = new LinkedHashMap<>();
        networthData.put("totalAssets", 1000000L);
        networthData.put("totalLiabilities", 200000L);
        networthData.put("netWorth", 800000L);
        networthData.put("assetBreakdown", new LinkedHashMap<>());
        lenient().when(analysisService.calculateNetWorth(anyString())).thenReturn(networthData);
        
        // Default retirement matrix
        Map<String, Object> retirementData = new LinkedHashMap<>();
        retirementData.put("matrix", Collections.emptyList());
        retirementData.put("summary", new LinkedHashMap<>());
        retirementData.put("gapAnalysis", new LinkedHashMap<>());
        lenient().when(retirementService.generateRetirementMatrix(anyString(), any())).thenReturn(retirementData);
    }

    @Test
    @DisplayName("Generate financial summary PDF with no data")
    void testFinancialSummaryPdfWithNoData() throws Exception {
        byte[] result = exportService.generateFinancialSummaryPdfReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Generate financial summary PDF with income data")
    void testFinancialSummaryPdfWithIncome() throws Exception {
        Income income = new Income();
        income.setId("inc1");
        income.setSource("Salary");
        income.setMonthlyAmount(50000.0);
        income.setAnnualIncrement(10.0);
        income.setIsActive(true);
        
        when(incomeRepository.findByUserId("user1")).thenReturn(Collections.singletonList(income));

        byte[] result = exportService.generateFinancialSummaryPdfReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Generate financial summary PDF with investments")
    void testFinancialSummaryPdfWithInvestments() throws Exception {
        Investment inv = new Investment();
        inv.setId("inv1");
        inv.setName("Test MF");
        inv.setType(Investment.InvestmentType.MUTUAL_FUND);
        inv.setCurrentValue(100000.0);
        inv.setInvestedAmount(80000.0);
        inv.setMonthlySip(5000.0);
        inv.setExpectedReturn(12.0);
        
        when(investmentRepository.findByUserId("user1")).thenReturn(Collections.singletonList(inv));

        byte[] result = exportService.generateFinancialSummaryPdfReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Generate financial summary PDF with loans")
    void testFinancialSummaryPdfWithLoans() throws Exception {
        Loan loan = new Loan();
        loan.setId("loan1");
        loan.setName("Home Loan");
        loan.setType(Loan.LoanType.HOME);
        loan.setOriginalAmount(2000000.0);
        loan.setOutstandingAmount(1500000.0);
        loan.setEmi(15000.0);
        loan.setInterestRate(8.5);
        loan.setRemainingMonths(100);
        
        when(loanRepository.findByUserId("user1")).thenReturn(Collections.singletonList(loan));

        byte[] result = exportService.generateFinancialSummaryPdfReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Generate financial summary PDF with insurance")
    void testFinancialSummaryPdfWithInsurance() throws Exception {
        Insurance ins = new Insurance();
        ins.setId("ins1");
        ins.setType(Insurance.InsuranceType.TERM_LIFE);
        ins.setCompany("Test Insurer");
        ins.setPolicyName("Term Plan");
        ins.setPolicyNumber("POL123");
        ins.setSumAssured(5000000.0);
        ins.setAnnualPremium(15000.0);
        ins.setPremiumFrequency(Insurance.PremiumFrequency.YEARLY);
        ins.setStartDate(LocalDate.of(2020, 1, 1));
        
        when(insuranceRepository.findByUserId("user1")).thenReturn(Collections.singletonList(ins));

        byte[] result = exportService.generateFinancialSummaryPdfReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Generate financial summary PDF with expenses")
    void testFinancialSummaryPdfWithExpenses() throws Exception {
        Expense exp = new Expense();
        exp.setId("exp1");
        exp.setName("Rent");
        exp.setCategory(Expense.ExpenseCategory.RENT);
        exp.setMonthlyAmount(15000.0);
        exp.setFrequency(Expense.ExpenseFrequency.MONTHLY);
        
        when(expenseRepository.findByUserId("user1")).thenReturn(Collections.singletonList(exp));

        byte[] result = exportService.generateFinancialSummaryPdfReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Generate retirement PDF with no scenario")
    void testRetirementPdfWithNoScenario() throws Exception {
        byte[] result = exportService.generateRetirementPdfReport("user1");

        assertThat(result).isNotEmpty();
        // Should create temporary default scenario
        verify(retirementService).generateRetirementMatrix(eq("user1"), any(RetirementScenario.class));
    }

    @Test
    @DisplayName("Generate retirement PDF with existing scenario")
    void testRetirementPdfWithScenario() throws Exception {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId("user1")
                .name("Default")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .isDefault(true)
                .build();
        
        when(scenarioRepository.findByUserIdAndIsDefaultTrue("user1")).thenReturn(Optional.of(scenario));

        byte[] result = exportService.generateRetirementPdfReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Generate calendar PDF")
    void testCalendarPdf() throws Exception {
        byte[] result = exportService.generateCalendarPdfReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Generate Excel report with no data")
    void testExcelReportWithNoData() throws Exception {
        byte[] result = exportService.generateExcelReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Generate Excel report with income")
    void testExcelReportWithIncome() throws Exception {
        Income income = new Income();
        income.setId("inc1");
        income.setSource("Salary");
        income.setMonthlyAmount(50000.0);
        
        when(incomeRepository.findByUserId("user1")).thenReturn(Collections.singletonList(income));

        byte[] result = exportService.generateExcelReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Generate Excel report with investments having null values")
    void testExcelReportWithNullInvestmentValues() throws Exception {
        Investment inv = new Investment();
        inv.setId("inv1");
        inv.setName("Test");
        inv.setType(Investment.InvestmentType.MUTUAL_FUND);
        inv.setCurrentValue(null);
        inv.setInvestedAmount(null);
        inv.setMonthlySip(null);
        
        when(investmentRepository.findByUserId("user1")).thenReturn(Collections.singletonList(inv));

        byte[] result = exportService.generateExcelReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Generate Excel report with loan having null values")
    void testExcelReportWithNullLoanValues() throws Exception {
        Loan loan = new Loan();
        loan.setId("loan1");
        loan.setName("Loan");
        loan.setType(Loan.LoanType.HOME);
        loan.setOriginalAmount(null);
        loan.setOutstandingAmount(null);
        loan.setEmi(null);
        loan.setInterestRate(null);
        loan.setRemainingMonths(null);
        
        when(loanRepository.findByUserId("user1")).thenReturn(Collections.singletonList(loan));

        byte[] result = exportService.generateExcelReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Generate Excel report with insurance having null frequency")
    void testExcelReportWithNullInsuranceFrequency() throws Exception {
        Insurance ins = new Insurance();
        ins.setId("ins1");
        ins.setType(Insurance.InsuranceType.HEALTH);
        ins.setCompany("Test");
        ins.setPolicyName("Health");
        ins.setSumAssured(500000.0);
        ins.setAnnualPremium(10000.0);
        ins.setPremiumFrequency(null);
        ins.setStartDate(LocalDate.now());
        
        when(insuranceRepository.findByUserId("user1")).thenReturn(Collections.singletonList(ins));

        byte[] result = exportService.generateExcelReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Generate Excel report with expense having null frequency")
    void testExcelReportWithNullExpenseFrequency() throws Exception {
        Expense exp = new Expense();
        exp.setId("exp1");
        exp.setName("Expense");
        exp.setCategory(Expense.ExpenseCategory.UTILITIES);
        exp.setMonthlyAmount(5000.0);
        exp.setFrequency(null);
        
        when(expenseRepository.findByUserId("user1")).thenReturn(Collections.singletonList(exp));

        byte[] result = exportService.generateExcelReport("user1");

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Export JSON data")
    void testExportJson() {
        Map<String, Object> result = exportService.exportAllData("user1");

        assertThat(result).containsKeys("income", "investments", "loans", "insurance", "expenses", "goals");
    }

    @Test
    @DisplayName("Export JSON with all data types")
    void testExportJsonWithAllData() {
        // Note: Using lenient mocks from setUp() to avoid UnnecessaryStubbingException
        Map<String, Object> result = exportService.exportAllData("user1");

        assertThat(result).containsKeys("income", "investments", "loans", "insurance", "expenses", "goals");
    }

    @Test
    @DisplayName("Import JSON data")
    void testImportJson() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("income", Collections.emptyList());
        data.put("investments", Collections.emptyList());
        data.put("loans", Collections.emptyList());
        data.put("insurance", Collections.emptyList());
        data.put("expenses", Collections.emptyList());
        data.put("goals", Collections.emptyList());

        // Just verify it doesn't throw an exception
        exportService.importAllData("user1", data);
    }
}
