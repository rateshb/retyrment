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

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportService Tests")
class ExportServiceTest {

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
    private CalendarEntryRepository calendarEntryRepository;
    @Mock
    private RetirementScenarioRepository scenarioRepository;
    @Mock
    private RetirementService retirementService;
    @Mock
    private AnalysisService analysisService;

    @InjectMocks
    private ExportService exportService;

    @Nested
    @DisplayName("exportAllData")
    class ExportAllData {
        @Test
        @DisplayName("should export all data from all repositories")
        void shouldExportAllData() {
            // Setup mocks
            when(incomeRepository.findByUserId("test-user")).thenReturn(Arrays.asList(Income.builder().source("Salary").build()));
            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(Investment.builder().name("PPF").build()));
            when(loanRepository.findByUserId("test-user")).thenReturn(Arrays.asList(Loan.builder().name("Home Loan").build()));
            when(insuranceRepository.findByUserId("test-user")).thenReturn(Arrays.asList(Insurance.builder().policyName("LIC").build()));
            when(expenseRepository.findByUserId("test-user")).thenReturn(Arrays.asList(Expense.builder().name("Rent").build()));
            when(goalRepository.findByUserId("test-user")).thenReturn(Arrays.asList(Goal.builder().name("Car").build()));
            when(calendarEntryRepository.findByUserId("test-user")).thenReturn(Arrays.asList(CalendarEntry.builder().description("SIP").build()));
            when(scenarioRepository.findByUserId("test-user")).thenReturn(Arrays.asList(RetirementScenario.builder().name("Default").build()));

            Map<String, Object> result = exportService.exportAllData("test-user");

            assertThat(result).containsKeys("exportDate", "version", "income", "investments", 
                "loans", "insurance", "expenses", "goals", "calendarEntries", "retirementScenarios");
            verify(incomeRepository).findByUserId("test-user");
            verify(investmentRepository).findByUserId("test-user");
        }
    }

    @Nested
    @DisplayName("importAllData")
    class ImportAllData {
        @Test
        @DisplayName("should import income data")
        void shouldImportIncomeData() {
            Map<String, Object> data = new HashMap<>();
            List<Map<String, Object>> incomeList = new ArrayList<>();
            Map<String, Object> income = new HashMap<>();
            income.put("source", "Salary");
            income.put("monthlyAmount", 100000.0);
            income.put("annualIncrement", 10.0);
            incomeList.add(income);
            data.put("income", incomeList);

            when(incomeRepository.save(any(Income.class))).thenAnswer(inv -> inv.getArgument(0));

            exportService.importAllData("test-user", data);

            verify(incomeRepository).save(any(Income.class));
        }

        @Test
        @DisplayName("should import investments data")
        void shouldImportInvestmentsData() {
            Map<String, Object> data = new HashMap<>();
            List<Map<String, Object>> invList = new ArrayList<>();
            Map<String, Object> inv = new HashMap<>();
            inv.put("type", "PPF");
            inv.put("name", "My PPF");
            inv.put("investedAmount", 500000);
            inv.put("currentValue", 600000);
            invList.add(inv);
            data.put("investments", invList);

            when(investmentRepository.save(any(Investment.class))).thenAnswer(i -> i.getArgument(0));

            exportService.importAllData("test-user", data);

            verify(investmentRepository).save(any(Investment.class));
        }

        @Test
        @DisplayName("should import loans data")
        void shouldImportLoansData() {
            Map<String, Object> data = new HashMap<>();
            List<Map<String, Object>> loanList = new ArrayList<>();
            Map<String, Object> loan = new HashMap<>();
            loan.put("type", "HOME");
            loan.put("name", "Home Loan");
            loan.put("outstandingAmount", "5000000");
            loan.put("emi", "45000");
            loan.put("remainingMonths", "180");
            loanList.add(loan);
            data.put("loans", loanList);

            when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));

            exportService.importAllData("test-user", data);

            verify(loanRepository).save(any(Loan.class));
        }

        @Test
        @DisplayName("should import insurance data")
        void shouldImportInsuranceData() {
            Map<String, Object> data = new HashMap<>();
            List<Map<String, Object>> insList = new ArrayList<>();
            Map<String, Object> ins = new HashMap<>();
            ins.put("type", "TERM_LIFE");
            ins.put("company", "LIC");
            ins.put("policyName", "Jeevan Anand");
            ins.put("sumAssured", 5000000);
            insList.add(ins);
            data.put("insurance", insList);

            when(insuranceRepository.save(any(Insurance.class))).thenAnswer(i -> i.getArgument(0));

            exportService.importAllData("test-user", data);

            verify(insuranceRepository).save(any(Insurance.class));
        }

        @Test
        @DisplayName("should import expenses data")
        void shouldImportExpensesData() {
            Map<String, Object> data = new HashMap<>();
            List<Map<String, Object>> expList = new ArrayList<>();
            Map<String, Object> exp = new HashMap<>();
            exp.put("category", "RENT");
            exp.put("name", "House Rent");
            exp.put("monthlyAmount", 25000);
            expList.add(exp);
            data.put("expenses", expList);

            when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

            exportService.importAllData("test-user", data);

            verify(expenseRepository).save(any(Expense.class));
        }

        @Test
        @DisplayName("should import goals data")
        void shouldImportGoalsData() {
            Map<String, Object> data = new HashMap<>();
            List<Map<String, Object>> goalList = new ArrayList<>();
            Map<String, Object> goal = new HashMap<>();
            goal.put("name", "Buy Car");
            goal.put("targetAmount", 1000000);
            goal.put("targetYear", 2027);
            goal.put("priority", "HIGH");
            goalList.add(goal);
            data.put("goals", goalList);

            when(goalRepository.save(any(Goal.class))).thenAnswer(i -> i.getArgument(0));

            exportService.importAllData("test-user", data);

            verify(goalRepository).save(any(Goal.class));
        }

        @Test
        @DisplayName("should handle null values in import")
        void shouldHandleNullValues() {
            Map<String, Object> data = new HashMap<>();
            List<Map<String, Object>> incomeList = new ArrayList<>();
            Map<String, Object> income = new HashMap<>();
            income.put("source", "Freelance");
            // No other fields
            incomeList.add(income);
            data.put("income", incomeList);

            when(incomeRepository.save(any(Income.class))).thenAnswer(inv -> inv.getArgument(0));

            exportService.importAllData("test-user", data);

            verify(incomeRepository).save(any(Income.class));
        }
    }

    @Nested
    @DisplayName("PDF/Excel generation")
    class ReportGeneration {
        @Test
        @DisplayName("generateFinancialSummaryPdfReport with asset breakdown")
        void generateFinancialSummaryPdfReport_withAssetBreakdown() {
            Map<String, Object> netWorth = new HashMap<>();
            netWorth.put("totalAssets", 100000.0);
            netWorth.put("totalLiabilities", 20000.0);
            netWorth.put("netWorth", 80000.0);
            Map<String, Object> breakdown = new HashMap<>();
            breakdown.put("cash", 50000.0);
            breakdown.put("mutual_funds", 50000.0);
            netWorth.put("assetBreakdown", breakdown);
            when(analysisService.calculateNetWorth("test-user")).thenReturn(netWorth);

            when(incomeRepository.findByUserId("test-user"))
                    .thenReturn(Arrays.asList(
                            Income.builder().monthlyAmount(10000.0).isActive(true).build(),
                            Income.builder().monthlyAmount(5000.0).isActive(false).build()
                    ));
            when(expenseRepository.findByUserId("test-user"))
                    .thenReturn(List.of(Expense.builder().amount(1200.0).frequency(Expense.ExpenseFrequency.MONTHLY).build()));
            when(loanRepository.findByUserId("test-user"))
                    .thenReturn(List.of(Loan.builder().emi(500.0).build()));
            when(insuranceRepository.findByUserId("test-user"))
                    .thenReturn(List.of(Insurance.builder().annualPremium(1200.0).build()));
            when(investmentRepository.findByUserId("test-user"))
                    .thenReturn(Arrays.asList(
                            Investment.builder().type(Investment.InvestmentType.PPF).name("PPF").build(),
                            Investment.builder().type(null).name("Unknown").build()
                    ));
            byte[] pdf = exportService.generateFinancialSummaryPdfReport("test-user");

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
        }

        @Test
        @DisplayName("generateFinancialSummaryPdfReport without asset breakdown")
        void generateFinancialSummaryPdfReport_withoutAssetBreakdown() {
            Map<String, Object> netWorth = new HashMap<>();
            netWorth.put("totalAssets", 0.0);
            netWorth.put("totalLiabilities", 0.0);
            netWorth.put("netWorth", 0.0);
            netWorth.put("assetBreakdown", new HashMap<>());
            when(analysisService.calculateNetWorth("test-user")).thenReturn(netWorth);
            when(incomeRepository.findByUserId("test-user")).thenReturn(List.of());
            when(expenseRepository.findByUserId("test-user")).thenReturn(List.of());
            when(loanRepository.findByUserId("test-user")).thenReturn(List.of());
            when(insuranceRepository.findByUserId("test-user")).thenReturn(List.of());
            when(investmentRepository.findByUserId("test-user")).thenReturn(List.of());
            byte[] pdf = exportService.generateFinancialSummaryPdfReport("test-user");

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
        }

        @Test
        @DisplayName("generateRetirementPdfReport creates default scenario")
        void generateRetirementPdfReport_defaultScenario() {
            when(scenarioRepository.findByUserIdAndIsDefaultTrue("test-user"))
                    .thenReturn(Optional.empty());

            Map<String, Object> summary = new HashMap<>();
            summary.put("currentAge", 35);
            summary.put("retirementAge", 60);
            summary.put("yearsToRetirement", 25);
            summary.put("finalCorpus", 1000000.0);

            Map<String, Object> gap = new HashMap<>();
            gap.put("corpusGap", 100000.0);
            gap.put("requiredCorpus", 1100000.0);
            gap.put("projectedCorpus", 1000000.0);
            gap.put("monthlySIP", 5000.0);
            gap.put("netMonthlySavings", 3000.0);

            Map<String, Object> row = new HashMap<>();
            row.put("year", 2030);
            row.put("age", 40);
            row.put("ppfBalance", 10000.0);
            row.put("epfBalance", 20000.0);
            row.put("mfBalance", 30000.0);
            row.put("goalOutflow", 0.0);
            row.put("netCorpus", 60000.0);

            Map<String, Object> retirementData = new HashMap<>();
            retirementData.put("summary", summary);
            retirementData.put("gapAnalysis", gap);
            retirementData.put("matrix", List.of(row));

            when(retirementService.generateRetirementMatrix(eq("test-user"), any(RetirementScenario.class)))
                    .thenReturn(retirementData);

            byte[] pdf = exportService.generateRetirementPdfReport("test-user");

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
        }

        @Test
        @DisplayName("generateCalendarPdfReport returns bytes")
        void generateCalendarPdfReport_returnsBytes() {
            byte[] pdf = exportService.generateCalendarPdfReport("test-user");
            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
        }

        @Test
        @DisplayName("generateExcelReport returns workbook bytes")
        void generateExcelReport_returnsBytes() throws Exception {
            Map<String, Object> netWorth = new HashMap<>();
            netWorth.put("totalAssets", 100000.0);
            netWorth.put("totalLiabilities", 20000.0);
            netWorth.put("netWorth", 80000.0);
            when(analysisService.calculateNetWorth("test-user")).thenReturn(netWorth);

            when(incomeRepository.findByUserId("test-user"))
                    .thenReturn(List.of(Income.builder().monthlyAmount(10000.0).isActive(true).build()));
            when(expenseRepository.findByUserId("test-user"))
                    .thenReturn(Arrays.asList(
                            Expense.builder().name("Rent").amount(1200.0).frequency(Expense.ExpenseFrequency.MONTHLY).build(),
                            Expense.builder().name("Fees").amount(24000.0).frequency(null).build()
                    ));
            when(loanRepository.findByUserId("test-user"))
                    .thenReturn(Arrays.asList(
                            Loan.builder().type(Loan.LoanType.HOME).name("Home").emi(1000.0).build(),
                            Loan.builder().type(null).name("Other").emi(0.0).build()
                    ));
            when(insuranceRepository.findByUserId("test-user"))
                    .thenReturn(Arrays.asList(
                            Insurance.builder().type(Insurance.InsuranceType.HEALTH).annualPremium(1200.0).build(),
                            Insurance.builder().type(null).annualPremium(0.0).build()
                    ));
            when(investmentRepository.findByUserId("test-user"))
                    .thenReturn(Arrays.asList(
                            Investment.builder().type(Investment.InvestmentType.PPF).name("PPF").build(),
                            Investment.builder().type(null).name("Other").build()
                    ));
            byte[] excel = exportService.generateExcelReport("test-user");
            assertThat(excel).isNotNull();
            assertThat(excel.length).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("generatePdfReport")
    class GeneratePdfReport {
        @Test
        @DisplayName("should generate PDF report with net worth data")
        void shouldGeneratePdfReport() {
            Map<String, Object> netWorth = new HashMap<>();
            netWorth.put("totalAssets", 5000000.0);
            netWorth.put("totalLiabilities", 1000000.0);
            netWorth.put("netWorth", 4000000.0);
            when(analysisService.calculateNetWorth("test-user")).thenReturn(netWorth);

            byte[] result = exportService.generateFinancialSummaryPdfReport("test-user");

            assertThat(result).isNotNull();
            assertThat(result.length).isGreaterThan(100); // PDF should have substantial content
            
            // Verify it's a valid PDF by checking the header
            String pdfHeader = new String(result, 0, Math.min(10, result.length));
            assertThat(pdfHeader).startsWith("%PDF-");
        }
    }

    @Nested
    @DisplayName("generateExcelReport")
    class GenerateExcelReport {
        @Test
        @DisplayName("should generate Excel report")
        void shouldGenerateExcelReport() throws Exception {
            // Setup mocks
            Map<String, Object> netWorth = new HashMap<>();
            netWorth.put("totalAssets", 5000000.0);
            netWorth.put("totalLiabilities", 1000000.0);
            netWorth.put("netWorth", 4000000.0);
            when(analysisService.calculateNetWorth("test-user")).thenReturn(netWorth);

            when(investmentRepository.findByUserId("test-user")).thenReturn(Arrays.asList(
                Investment.builder()
                    .type(Investment.InvestmentType.MUTUAL_FUND)
                    .name("HDFC Equity")
                    .investedAmount(100000.0)
                    .currentValue(120000.0)
                    .monthlySip(5000.0)
                    .build()
            ));

            Map<String, Object> matrixData = new HashMap<>();
            List<Map<String, Object>> matrix = new ArrayList<>();
            Map<String, Object> row = new HashMap<>();
            row.put("year", 2026);
            row.put("age", 35);
            row.put("ppfBalance", 100000.0);
            row.put("epfBalance", 200000.0);
            row.put("mfBalance", 500000.0);
            row.put("mfSip", 10000.0);
            row.put("goalOutflow", 0.0);
            row.put("netCorpus", 800000.0);
            matrix.add(row);
            matrixData.put("matrix", matrix);
            lenient().when(retirementService.generateRetirementMatrix("test-user", null)).thenReturn(matrixData);

            byte[] result = exportService.generateExcelReport("test-user");

            assertThat(result).isNotNull();
            assertThat(result.length).isGreaterThan(0);
        }

        @Test
        @DisplayName("should handle empty investments")
        void shouldHandleEmptyInvestments() throws Exception {
            Map<String, Object> netWorth = new HashMap<>();
            netWorth.put("totalAssets", 0.0);
            netWorth.put("totalLiabilities", 0.0);
            netWorth.put("netWorth", 0.0);
            when(analysisService.calculateNetWorth("test-user")).thenReturn(netWorth);
            when(investmentRepository.findByUserId("test-user")).thenReturn(Collections.emptyList());

            Map<String, Object> matrixData = new HashMap<>();
            matrixData.put("matrix", Collections.emptyList());
            lenient().when(retirementService.generateRetirementMatrix("test-user", null)).thenReturn(matrixData);

            byte[] result = exportService.generateExcelReport("test-user");

            assertThat(result).isNotNull();
        }
    }
}
