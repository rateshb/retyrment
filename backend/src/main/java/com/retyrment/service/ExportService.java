package com.retyrment.service;

import com.retyrment.model.*;
import com.retyrment.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final IncomeRepository incomeRepository;
    private final InvestmentRepository investmentRepository;
    private final LoanRepository loanRepository;
    private final InsuranceRepository insuranceRepository;
    private final ExpenseRepository expenseRepository;
    private final GoalRepository goalRepository;
    private final CalendarEntryRepository calendarEntryRepository;
    private final RetirementScenarioRepository scenarioRepository;
    private final RetirementService retirementService;
    private final AnalysisService analysisService;

    public Map<String, Object> exportAllData(String userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportDate", LocalDate.now().toString());
        data.put("version", "1.0");
        data.put("income", incomeRepository.findByUserId(userId));
        data.put("investments", investmentRepository.findByUserId(userId));
        data.put("loans", loanRepository.findByUserId(userId));
        data.put("insurance", insuranceRepository.findByUserId(userId));
        data.put("expenses", expenseRepository.findByUserId(userId));
        data.put("goals", goalRepository.findByUserId(userId));
        data.put("calendarEntries", calendarEntryRepository.findByUserId(userId));
        data.put("retirementScenarios", scenarioRepository.findByUserId(userId));
        return data;
    }

    @SuppressWarnings("unchecked")
    public void importAllData(String userId, Map<String, Object> data) {
        // Import income
        if (data.containsKey("income")) {
            List<Map<String, Object>> incomeList = (List<Map<String, Object>>) data.get("income");
            for (Map<String, Object> item : incomeList) {
                Income income = mapToIncome(item);
                income.setUserId(userId);
                incomeRepository.save(income);
            }
        }

        // Import investments
        if (data.containsKey("investments")) {
            List<Map<String, Object>> invList = (List<Map<String, Object>>) data.get("investments");
            for (Map<String, Object> item : invList) {
                Investment inv = mapToInvestment(item);
                inv.setUserId(userId);
                investmentRepository.save(inv);
            }
        }

        // Import loans
        if (data.containsKey("loans")) {
            List<Map<String, Object>> loanList = (List<Map<String, Object>>) data.get("loans");
            for (Map<String, Object> item : loanList) {
                Loan loan = mapToLoan(item);
                loan.setUserId(userId);
                loanRepository.save(loan);
            }
        }

        // Import insurance
        if (data.containsKey("insurance")) {
            List<Map<String, Object>> insList = (List<Map<String, Object>>) data.get("insurance");
            for (Map<String, Object> item : insList) {
                Insurance ins = mapToInsurance(item);
                ins.setUserId(userId);
                insuranceRepository.save(ins);
            }
        }

        // Import expenses
        if (data.containsKey("expenses")) {
            List<Map<String, Object>> expList = (List<Map<String, Object>>) data.get("expenses");
            for (Map<String, Object> item : expList) {
                Expense exp = mapToExpense(item);
                exp.setUserId(userId);
                expenseRepository.save(exp);
            }
        }

        // Import goals
        if (data.containsKey("goals")) {
            List<Map<String, Object>> goalList = (List<Map<String, Object>>) data.get("goals");
            for (Map<String, Object> item : goalList) {
                Goal goal = mapToGoal(item);
                goal.setUserId(userId);
                goalRepository.save(goal);
            }
        }
    }

    private Income mapToIncome(Map<String, Object> item) {
        return Income.builder()
                .source((String) item.get("source"))
                .monthlyAmount(getDouble(item, "monthlyAmount"))
                .annualIncrement(getDouble(item, "annualIncrement"))
                .isActive((Boolean) item.getOrDefault("isActive", true))
                .build();
    }

    private Investment mapToInvestment(Map<String, Object> item) {
        return Investment.builder()
                .type(item.get("type") != null ? Investment.InvestmentType.valueOf((String) item.get("type")) : null)
                .name((String) item.get("name"))
                .investedAmount(getDouble(item, "investedAmount"))
                .currentValue(getDouble(item, "currentValue"))
                .monthlySip(getDouble(item, "monthlySip"))
                .expectedReturn(getDouble(item, "expectedReturn"))
                .build();
    }

    private Loan mapToLoan(Map<String, Object> item) {
        return Loan.builder()
                .type(item.get("type") != null ? Loan.LoanType.valueOf((String) item.get("type")) : null)
                .name((String) item.get("name"))
                .outstandingAmount(getDouble(item, "outstandingAmount"))
                .emi(getDouble(item, "emi"))
                .interestRate(getDouble(item, "interestRate"))
                .remainingMonths(getInt(item, "remainingMonths"))
                .build();
    }

    private Insurance mapToInsurance(Map<String, Object> item) {
        return Insurance.builder()
                .type(item.get("type") != null ? Insurance.InsuranceType.valueOf((String) item.get("type")) : null)
                .company((String) item.get("company"))
                .policyName((String) item.get("policyName"))
                .sumAssured(getDouble(item, "sumAssured"))
                .annualPremium(getDouble(item, "annualPremium"))
                .renewalMonth(getInt(item, "renewalMonth"))
                .fundValue(getDouble(item, "fundValue"))
                .build();
    }

    private Expense mapToExpense(Map<String, Object> item) {
        return Expense.builder()
                .category(item.get("category") != null ? Expense.ExpenseCategory.valueOf((String) item.get("category")) : null)
                .name((String) item.get("name"))
                .monthlyAmount(getDouble(item, "monthlyAmount"))
                .isFixed((Boolean) item.getOrDefault("isFixed", true))
                .build();
    }

    private Goal mapToGoal(Map<String, Object> item) {
        return Goal.builder()
                .icon((String) item.get("icon"))
                .name((String) item.get("name"))
                .targetAmount(getDouble(item, "targetAmount"))
                .targetYear(getInt(item, "targetYear"))
                .priority(item.get("priority") != null ? Goal.Priority.valueOf((String) item.get("priority")) : Goal.Priority.MEDIUM)
                .isRecurring((Boolean) item.getOrDefault("isRecurring", false))
                .build();
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    public byte[] generatePdfReport(String userId) {
        // Basic PDF generation - for production use a proper PDF library
        StringBuilder content = new StringBuilder();
        content.append("RETYRMENT FINANCIAL REPORT\n");
        content.append("Generated: ").append(LocalDate.now()).append("\n\n");

        Map<String, Object> netWorth = analysisService.calculateNetWorth(userId);
        content.append("NET WORTH SUMMARY\n");
        content.append("================\n");
        content.append("Total Assets: ₹").append(netWorth.get("totalAssets")).append("\n");
        content.append("Total Liabilities: ₹").append(netWorth.get("totalLiabilities")).append("\n");
        content.append("Net Worth: ₹").append(netWorth.get("netWorth")).append("\n\n");

        // For now, return as text (in production, use iText or similar)
        return content.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] generateExcelReport(String userId) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("₹#,##0"));

            // Summary Sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            Map<String, Object> netWorth = analysisService.calculateNetWorth(userId);
            int rowNum = 0;
            
            Row titleRow = summarySheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("RETYRMENT FINANCIAL SUMMARY");
            titleCell.setCellStyle(headerStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            
            rowNum++;
            createSummaryRow(summarySheet, rowNum++, "Report Date", LocalDate.now().toString());
            createSummaryRow(summarySheet, rowNum++, "Total Assets", formatNumber(netWorth.get("totalAssets")));
            createSummaryRow(summarySheet, rowNum++, "Total Liabilities", formatNumber(netWorth.get("totalLiabilities")));
            createSummaryRow(summarySheet, rowNum++, "Net Worth", formatNumber(netWorth.get("netWorth")));

            // Investments Sheet
            Sheet invSheet = workbook.createSheet("Investments");
            List<Investment> investments = investmentRepository.findByUserId(userId);
            rowNum = 0;
            
            Row invHeader = invSheet.createRow(rowNum++);
            String[] invHeaders = {"Type", "Name", "Invested", "Current Value", "Gain/Loss", "SIP"};
            for (int i = 0; i < invHeaders.length; i++) {
                Cell cell = invHeader.createCell(i);
                cell.setCellValue(invHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            
            for (Investment inv : investments) {
                Row row = invSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(inv.getType() != null ? inv.getType().name() : "");
                row.createCell(1).setCellValue(inv.getName() != null ? inv.getName() : "");
                row.createCell(2).setCellValue(inv.getInvestedAmount() != null ? inv.getInvestedAmount() : 0);
                row.createCell(3).setCellValue(inv.getCurrentValue() != null ? inv.getCurrentValue() : 0);
                double gain = (inv.getCurrentValue() != null ? inv.getCurrentValue() : 0) - 
                             (inv.getInvestedAmount() != null ? inv.getInvestedAmount() : 0);
                row.createCell(4).setCellValue(gain);
                row.createCell(5).setCellValue(inv.getMonthlySip() != null ? inv.getMonthlySip() : 0);
            }

            // Retirement Matrix Sheet
            Sheet retSheet = workbook.createSheet("Retirement Matrix");
            // Get default scenario for user
            RetirementScenario defaultScenario = scenarioRepository.findByUserIdAndIsDefaultTrue(userId)
                    .orElse(null);
            Map<String, Object> matrixData = defaultScenario != null 
                    ? retirementService.generateRetirementMatrix(userId, defaultScenario)
                    : Map.of("matrix", Collections.emptyList());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) matrixData.get("matrix");
            
            rowNum = 0;
            Row retHeader = retSheet.createRow(rowNum++);
            String[] retHeaders = {"Year", "Age", "PPF", "EPF", "MF", "SIP/mo", "Goal Outflow", "Net Corpus"};
            for (int i = 0; i < retHeaders.length; i++) {
                Cell cell = retHeader.createCell(i);
                cell.setCellValue(retHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            
            for (Map<String, Object> row : matrix) {
                Row excelRow = retSheet.createRow(rowNum++);
                excelRow.createCell(0).setCellValue(((Number) row.get("year")).intValue());
                excelRow.createCell(1).setCellValue(((Number) row.get("age")).intValue());
                excelRow.createCell(2).setCellValue(((Number) row.get("ppfBalance")).doubleValue());
                excelRow.createCell(3).setCellValue(((Number) row.get("epfBalance")).doubleValue());
                excelRow.createCell(4).setCellValue(((Number) row.get("mfBalance")).doubleValue());
                excelRow.createCell(5).setCellValue(((Number) row.get("mfSip")).doubleValue());
                excelRow.createCell(6).setCellValue(((Number) row.get("goalOutflow")).doubleValue());
                excelRow.createCell(7).setCellValue(((Number) row.get("netCorpus")).doubleValue());
            }

            // Auto-size columns
            for (int i = 0; i < 8; i++) {
                summarySheet.autoSizeColumn(i);
                invSheet.autoSizeColumn(i);
                retSheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createSummaryRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private String formatNumber(Object num) {
        if (num == null) return "0";
        return String.format("₹%,.0f", ((Number) num).doubleValue());
    }
}
