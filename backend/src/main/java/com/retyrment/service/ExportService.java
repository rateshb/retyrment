package com.retyrment.service;

import com.retyrment.model.*;
import com.retyrment.repository.*;
// iText PDF imports
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
// Apache POI Excel imports
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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

    public byte[] generateFinancialSummaryPdfReport(String userId) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Standard font
            float bodyFontSize = 10f;
            float headerFontSize = 14f;
            float titleFontSize = 20f;
            
            // Colors
            DeviceRgb primaryColor = new DeviceRgb(79, 70, 229); // Indigo
            DeviceRgb successColor = new DeviceRgb(16, 185, 129); // Green
            DeviceRgb dangerColor = new DeviceRgb(239, 68, 68); // Red
            DeviceRgb amberColor = new DeviceRgb(245, 158, 11); // Amber
            
            // Title
            document.add(new Paragraph("FINANCIAL SUMMARY REPORT")
                    .setFontSize(titleFontSize)
                    .setBold()
                    .setFontColor(primaryColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5));
            
            document.add(new Paragraph("Generated: " + LocalDate.now())
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));
            
            // Net Worth Overview
        Map<String, Object> netWorth = analysisService.calculateNetWorth(userId);
            
            document.add(new Paragraph("NET WORTH OVERVIEW")
                    .setFontSize(headerFontSize)
                    .setBold()
                    .setFontColor(primaryColor)
                    .setMarginBottom(10));
            
            Table netWorthTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(15);
            
            addTableRow(netWorthTable, "Total Assets", formatCurrency(netWorth.get("totalAssets")), successColor, bodyFontSize);
            addTableRow(netWorthTable, "Total Liabilities", formatCurrency(netWorth.get("totalLiabilities")), dangerColor, bodyFontSize);
            addTableRow(netWorthTable, "Net Worth", formatCurrency(netWorth.get("netWorth")), primaryColor, bodyFontSize);
            
            document.add(netWorthTable);
            
            // Asset Breakdown - Visual representation with percentages
            @SuppressWarnings("unchecked")
            Map<String, Object> assetBreakdown = (Map<String, Object>) netWorth.get("assetBreakdown");
            if (assetBreakdown != null && !assetBreakdown.isEmpty()) {
                document.add(new Paragraph("ASSET BREAKDOWN")
                        .setFontSize(headerFontSize)
                        .setBold()
                        .setFontColor(primaryColor)
                        .setMarginBottom(10));
                
                // Calculate total for percentage
                double totalAssets = netWorth.get("totalAssets") != null ? 
                        ((Number) netWorth.get("totalAssets")).doubleValue() : 0;
                
                // Define colors for different asset types
                Map<String, DeviceRgb> assetColors = new java.util.HashMap<>();
                assetColors.put("cash", new DeviceRgb(34, 197, 94)); // Green
                assetColors.put("ppf", new DeviceRgb(59, 130, 246)); // Blue
                assetColors.put("epf", new DeviceRgb(147, 51, 234)); // Purple
                assetColors.put("mutual_funds", new DeviceRgb(249, 115, 22)); // Orange
                assetColors.put("nps", new DeviceRgb(236, 72, 153)); // Pink
                assetColors.put("real_estate", new DeviceRgb(168, 85, 247)); // Violet
                assetColors.put("gold", new DeviceRgb(234, 179, 8)); // Yellow
                assetColors.put("other_liquid", new DeviceRgb(107, 114, 128)); // Gray
                assetColors.put("other_illiquid", new DeviceRgb(75, 85, 99)); // Dark Gray
                
                Table assetTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 1}))
                        .setWidth(UnitValue.createPercentValue(100))
                        .setMarginBottom(15);
                
                assetTable.addHeaderCell(createHeaderCell("Asset Type", bodyFontSize));
                assetTable.addHeaderCell(createHeaderCell("Amount", bodyFontSize));
                assetTable.addHeaderCell(createHeaderCell("% of Total", bodyFontSize));
                
                assetBreakdown.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof Number && ((Number) entry.getValue()).doubleValue() > 0)
                    .sorted((e1, e2) -> Double.compare(
                        ((Number) e2.getValue()).doubleValue(),
                        ((Number) e1.getValue()).doubleValue()
                    ))
                    .forEach(entry -> {
                        String assetName = entry.getKey().replace("_", " ").toUpperCase();
                        double value = ((Number) entry.getValue()).doubleValue();
                        double percentage = totalAssets > 0 ? (value / totalAssets) * 100 : 0;
                        DeviceRgb color = assetColors.getOrDefault(entry.getKey(), new DeviceRgb(100, 116, 139));
                        
                        assetTable.addCell(new com.itextpdf.layout.element.Cell()
                                .add(new Paragraph(assetName).setFontSize(bodyFontSize))
                                .setBackgroundColor(color)
                                .setFontColor(ColorConstants.WHITE)
                                .setPadding(5));
                        assetTable.addCell(createBodyCell(formatCurrency(value), bodyFontSize));
                        assetTable.addCell(createBodyCell(String.format("%.1f%%", percentage), bodyFontSize).setBold());
                    });
                
                document.add(assetTable);
            }
            
            // Monthly Cash Flow Overview
            document.add(new Paragraph("MONTHLY CASH FLOW OVERVIEW")
                    .setFontSize(headerFontSize)
                    .setBold()
                    .setFontColor(primaryColor)
                    .setMarginBottom(10));
            
            // Calculate monthly cash flow
            List<Income> incomesForCalc = incomeRepository.findByUserId(userId);
            List<Expense> expensesForCalc = expenseRepository.findByUserId(userId);
            List<Loan> loansForCalc = loanRepository.findByUserId(userId);
            List<Insurance> insurancesForCalc = insuranceRepository.findByUserId(userId);
            
            double monthlyIncome = incomesForCalc.stream()
                    .filter(i -> i.getIsActive() != null && i.getIsActive())
                    .mapToDouble(i -> i.getMonthlyAmount() != null ? i.getMonthlyAmount() : 0)
                    .sum();
            
            double monthlyExpenses = expensesForCalc.stream()
                    .mapToDouble(e -> e.getMonthlyEquivalent() != null ? e.getMonthlyEquivalent() : 0)
                    .sum();
            
            double monthlyEMI = loansForCalc.stream()
                    .mapToDouble(l -> l.getEmi() != null ? l.getEmi() : 0)
                    .sum();
            
            double monthlyPremium = insurancesForCalc.stream()
                    .mapToDouble(ins -> {
                        double annual = ins.getAnnualPremium() != null ? ins.getAnnualPremium() : 0;
                        return annual / 12;
                    })
                    .sum();
            
            double monthlySavings = monthlyIncome - monthlyExpenses - monthlyEMI - monthlyPremium;
            
            Table cashFlowTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(15);
            
            addTableRow(cashFlowTable, "Total Income", formatCurrency(monthlyIncome), successColor, bodyFontSize);
            addTableRow(cashFlowTable, "Expenses", formatCurrency(monthlyExpenses), dangerColor, bodyFontSize);
            addTableRow(cashFlowTable, "EMIs", formatCurrency(monthlyEMI), dangerColor, bodyFontSize);
            addTableRow(cashFlowTable, "Insurance Premiums", formatCurrency(monthlyPremium), amberColor, bodyFontSize);
            addTableRow(cashFlowTable, "Monthly Savings", formatCurrency(monthlySavings), monthlySavings > 0 ? successColor : dangerColor, bodyFontSize);
            
            document.add(cashFlowTable);
            
            // Income Sources
            List<Income> incomes = incomeRepository.findByUserId(userId);
            if (!incomes.isEmpty()) {
                document.add(new Paragraph("INCOME SOURCES")
                        .setFontSize(headerFontSize)
                        .setBold()
                        .setFontColor(primaryColor)
                        .setMarginBottom(10));
                
                Table incomeTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 1}))
                        .setWidth(UnitValue.createPercentValue(100))
                        .setMarginBottom(15);
                
                incomeTable.addHeaderCell(createHeaderCell("Source", bodyFontSize));
                incomeTable.addHeaderCell(createHeaderCell("Monthly Amount", bodyFontSize));
                incomeTable.addHeaderCell(createHeaderCell("Annual Increment", bodyFontSize));
                
                for (Income inc : incomes) {
                    incomeTable.addCell(createBodyCell(inc.getSource() != null ? inc.getSource() : "-", bodyFontSize));
                    incomeTable.addCell(createBodyCell(formatCurrency(inc.getMonthlyAmount()), bodyFontSize));
                    incomeTable.addCell(createBodyCell((inc.getAnnualIncrement() != null ? inc.getAnnualIncrement() : 0) + "%", bodyFontSize));
                }
                
                document.add(incomeTable);
            }
            
            // Expenses
            List<Expense> expenses = expenseRepository.findByUserId(userId);
            if (!expenses.isEmpty()) {
                document.add(new Paragraph("EXPENSES")
                        .setFontSize(headerFontSize)
                        .setBold()
                        .setFontColor(primaryColor)
                        .setMarginBottom(10));
                
                Table expenseTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 1.5f, 1, 1}))
                        .setWidth(UnitValue.createPercentValue(100))
                        .setMarginBottom(15);
                
                expenseTable.addHeaderCell(createHeaderCell("Category", bodyFontSize));
                expenseTable.addHeaderCell(createHeaderCell("Name", bodyFontSize));
                expenseTable.addHeaderCell(createHeaderCell("Amount", bodyFontSize));
                expenseTable.addHeaderCell(createHeaderCell("Frequency", bodyFontSize));
                expenseTable.addHeaderCell(createHeaderCell("Type", bodyFontSize));
                
                for (Expense exp : expenses) {
                    expenseTable.addCell(createBodyCell(exp.getCategory() != null ? exp.getCategory().name() : "-", bodyFontSize));
                    expenseTable.addCell(createBodyCell(exp.getName() != null ? exp.getName() : "-", bodyFontSize));
                    expenseTable.addCell(createBodyCell(formatCurrency(exp.getAmount()), bodyFontSize));
                    expenseTable.addCell(createBodyCell(exp.getFrequency() != null ? exp.getFrequency().name() : "MONTHLY", bodyFontSize));
                    String type = (exp.getIsFixed() != null && exp.getIsFixed() ? "Fixed" : "Variable") + 
                                  (exp.getIsTimeBound() != null && exp.getIsTimeBound() ? ", Time-Bound" : "");
                    expenseTable.addCell(createBodyCell(type, bodyFontSize));
                }
                
                document.add(expenseTable);
            }
            
            // Page break
            document.add(new AreaBreak());
            
            // Investments Detail
            List<Investment> investments2 = investmentRepository.findByUserId(userId);
            if (!investments2.isEmpty()) {
                document.add(new Paragraph("INVESTMENTS DETAIL")
                        .setFontSize(headerFontSize)
                        .setBold()
                        .setFontColor(primaryColor)
                        .setMarginBottom(10));
                
                Table invTable = new Table(UnitValue.createPercentArray(new float[]{1.5f, 2, 1.5f, 1.5f, 1, 1}))
                        .setWidth(UnitValue.createPercentValue(100))
                        .setMarginBottom(15);
                
                invTable.addHeaderCell(createHeaderCell("Type", bodyFontSize));
                invTable.addHeaderCell(createHeaderCell("Name", bodyFontSize));
                invTable.addHeaderCell(createHeaderCell("Invested", bodyFontSize));
                invTable.addHeaderCell(createHeaderCell("Current Value", bodyFontSize));
                invTable.addHeaderCell(createHeaderCell("Gain/Loss", bodyFontSize));
                invTable.addHeaderCell(createHeaderCell("Monthly SIP", bodyFontSize));
                
                for (Investment inv : investments2) {
                    double invested = inv.getInvestedAmount() != null ? inv.getInvestedAmount() : 0;
                    double current = inv.getCurrentValue() != null ? inv.getCurrentValue() : 0;
                    double gain = current - invested;
                    
                    invTable.addCell(createBodyCell(inv.getType() != null ? inv.getType().name() : "-", bodyFontSize));
                    invTable.addCell(createBodyCell(inv.getName() != null ? inv.getName() : "-", bodyFontSize));
                    invTable.addCell(createBodyCell(formatCurrency(invested), bodyFontSize));
                    invTable.addCell(createBodyCell(formatCurrency(current), bodyFontSize));
                    invTable.addCell(createBodyCell(formatCurrency(gain), bodyFontSize).setFontColor(gain >= 0 ? successColor : dangerColor));
                    invTable.addCell(createBodyCell(formatCurrency(inv.getMonthlySip()), bodyFontSize));
                }
                
                document.add(invTable);
            }
            
            // Loans Detail
            List<Loan> loans2 = loanRepository.findByUserId(userId);
            if (!loans2.isEmpty()) {
                document.add(new Paragraph("LOANS DETAIL")
                        .setFontSize(headerFontSize)
                        .setBold()
                        .setFontColor(dangerColor)
                        .setMarginBottom(10));
                
                Table loanTable = new Table(UnitValue.createPercentArray(new float[]{1.5f, 2, 1.5f, 1, 1, 1}))
                        .setWidth(UnitValue.createPercentValue(100))
                        .setMarginBottom(15);
                
                loanTable.addHeaderCell(createHeaderCell("Type", bodyFontSize));
                loanTable.addHeaderCell(createHeaderCell("Name", bodyFontSize));
                loanTable.addHeaderCell(createHeaderCell("Outstanding", bodyFontSize));
                loanTable.addHeaderCell(createHeaderCell("EMI", bodyFontSize));
                loanTable.addHeaderCell(createHeaderCell("Interest Rate", bodyFontSize));
                loanTable.addHeaderCell(createHeaderCell("Remaining", bodyFontSize));
                
                for (Loan loan : loans2) {
                    loanTable.addCell(createBodyCell(loan.getType() != null ? loan.getType().name() : "-", bodyFontSize));
                    loanTable.addCell(createBodyCell(loan.getName() != null ? loan.getName() : "-", bodyFontSize));
                    loanTable.addCell(createBodyCell(formatCurrency(loan.getOutstandingAmount()), bodyFontSize));
                    loanTable.addCell(createBodyCell(formatCurrency(loan.getEmi()), bodyFontSize));
                    loanTable.addCell(createBodyCell((loan.getInterestRate() != null ? loan.getInterestRate() : 0) + "%", bodyFontSize));
                    loanTable.addCell(createBodyCell((loan.getRemainingMonths() != null ? loan.getRemainingMonths() : 0) + " months", bodyFontSize));
                }
                
                document.add(loanTable);
            }
            
            // Insurance Detail
            List<Insurance> insurances = insuranceRepository.findByUserId(userId);
            if (!insurances.isEmpty()) {
                document.add(new Paragraph("INSURANCE POLICIES")
                        .setFontSize(headerFontSize)
                        .setBold()
                        .setFontColor(primaryColor)
                        .setMarginBottom(10));
                
                Table insTable = new Table(UnitValue.createPercentArray(new float[]{1.5f, 2, 1.5f, 1.5f, 1}))
                        .setWidth(UnitValue.createPercentValue(100))
                        .setMarginBottom(15);
                
                insTable.addHeaderCell(createHeaderCell("Type", bodyFontSize));
                insTable.addHeaderCell(createHeaderCell("Policy Name", bodyFontSize));
                insTable.addHeaderCell(createHeaderCell("Company", bodyFontSize));
                insTable.addHeaderCell(createHeaderCell("Sum Assured", bodyFontSize));
                insTable.addHeaderCell(createHeaderCell("Annual Premium", bodyFontSize));
                
                for (Insurance ins : insurances) {
                    insTable.addCell(createBodyCell(ins.getType() != null ? ins.getType().name() : "-", bodyFontSize));
                    insTable.addCell(createBodyCell(ins.getPolicyName() != null ? ins.getPolicyName() : "-", bodyFontSize));
                    insTable.addCell(createBodyCell(ins.getCompany() != null ? ins.getCompany() : "-", bodyFontSize));
                    insTable.addCell(createBodyCell(formatCurrency(ins.getSumAssured()), bodyFontSize));
                    insTable.addCell(createBodyCell(formatCurrency(ins.getAnnualPremium()), bodyFontSize));
                }
                
                document.add(insTable);
            }
            
            // Footer
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Report generated by Retyrment - Your Retirement Planning Partner")
                    .setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));
            
            document.close();
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Financial Summary PDF", e);
        }
    }
    
    public byte[] generateRetirementPdfReport(String userId) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            float bodyFontSize = 9f;
            float headerFontSize = 14f;
            float titleFontSize = 20f;
            
            DeviceRgb primaryColor = new DeviceRgb(79, 70, 229);
            DeviceRgb successColor = new DeviceRgb(16, 185, 129);
            DeviceRgb dangerColor = new DeviceRgb(239, 68, 68);
            
            // Title
            document.add(new Paragraph("RETIREMENT PROJECTION REPORT")
                    .setFontSize(titleFontSize)
                    .setBold()
                    .setFontColor(primaryColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5));
            
            document.add(new Paragraph("Generated: " + LocalDate.now())
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));
            
            // Get retirement data with default scenario or create one
            RetirementScenario defaultScenario = scenarioRepository.findByUserIdAndIsDefaultTrue(userId)
                    .orElse(null);
            
            // If no default scenario exists, create a temporary one with default values
            if (defaultScenario == null) {
                defaultScenario = RetirementScenario.builder()
                        .userId(userId)
                        .name("Default")
                        .isDefault(true)
                        .currentAge(35)
                        .retirementAge(60)
                        .lifeExpectancy(85)
                        .inflation(6.0)
                        .mfReturn(12.0)
                        .ppfReturn(7.1)
                        .epfReturn(8.5)
                        .withdrawalRate(6.0)
                        .sipStepup(10.0)
                        .effectiveFromYear(0)
                        .build();
            }
            
            Map<String, Object> retirementData = retirementService.generateRetirementMatrix(userId, defaultScenario);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) retirementData.get("summary");
            @SuppressWarnings("unchecked")
            Map<String, Object> gapAnalysis = (Map<String, Object>) retirementData.get("gapAnalysis");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matrix = (List<Map<String, Object>>) retirementData.get("matrix");
            
            if (summary != null) {
                // Overview Section
                document.add(new Paragraph("RETIREMENT OVERVIEW")
                        .setFontSize(headerFontSize)
                        .setBold()
                        .setFontColor(primaryColor)
                        .setMarginBottom(10));
                
                Table overviewTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                        .setWidth(UnitValue.createPercentValue(100))
                        .setMarginBottom(15);
                
                addTableRow(overviewTable, "Current Age", summary.get("currentAge") + " years", primaryColor, bodyFontSize);
                addTableRow(overviewTable, "Retirement Age", summary.get("retirementAge") + " years", primaryColor, bodyFontSize);
                addTableRow(overviewTable, "Years to Retirement", summary.get("yearsToRetirement") + " years", primaryColor, bodyFontSize);
                addTableRow(overviewTable, "Projected Corpus", formatCurrency(summary.get("finalCorpus")), successColor, bodyFontSize);
                
                document.add(overviewTable);
            }
            
            // Gap Analysis
            if (gapAnalysis != null) {
                document.add(new Paragraph("GAP ANALYSIS")
                        .setFontSize(headerFontSize)
                        .setBold()
                        .setFontColor(primaryColor)
                        .setMarginBottom(10));
                
                Table gapTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                        .setWidth(UnitValue.createPercentValue(100))
                        .setMarginBottom(15);
                
                double corpusGap = gapAnalysis.get("corpusGap") != null ? ((Number) gapAnalysis.get("corpusGap")).doubleValue() : 0;
                
                addTableRow(gapTable, "Required Corpus", formatCurrency(gapAnalysis.get("requiredCorpus")), primaryColor, bodyFontSize);
                addTableRow(gapTable, "Projected Corpus", formatCurrency(gapAnalysis.get("projectedCorpus")), primaryColor, bodyFontSize);
                addTableRow(gapTable, "Corpus Gap", formatCurrency(Math.abs(corpusGap)), corpusGap > 0 ? dangerColor : successColor, bodyFontSize);
                addTableRow(gapTable, "Monthly SIP Required", formatCurrency(gapAnalysis.get("monthlySIP")), primaryColor, bodyFontSize);
                addTableRow(gapTable, "Monthly Savings Available", formatCurrency(gapAnalysis.get("netMonthlySavings")), primaryColor, bodyFontSize);
                
                document.add(gapTable);
            }
            
            // Year-by-Year Matrix (first 20 years)
            if (matrix != null && !matrix.isEmpty()) {
                document.add(new Paragraph("YEAR-BY-YEAR PROJECTION (First 20 Years)")
                        .setFontSize(headerFontSize)
                        .setBold()
                        .setFontColor(primaryColor)
                        .setMarginBottom(10));
                
                Table matrixTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f}))
                        .setWidth(UnitValue.createPercentValue(100))
                        .setMarginBottom(15)
                        .setFontSize(8);
                
                matrixTable.addHeaderCell(createHeaderCell("Year", 8f));
                matrixTable.addHeaderCell(createHeaderCell("Age", 8f));
                matrixTable.addHeaderCell(createHeaderCell("PPF", 8f));
                matrixTable.addHeaderCell(createHeaderCell("EPF", 8f));
                matrixTable.addHeaderCell(createHeaderCell("MF", 8f));
                matrixTable.addHeaderCell(createHeaderCell("Goals", 8f));
                matrixTable.addHeaderCell(createHeaderCell("Net Corpus", 8f));
                
                int count = 0;
                for (Map<String, Object> row : matrix) {
                    if (count++ >= 20) break;
                    
                    matrixTable.addCell(createBodyCell(row.get("year").toString(), 8f));
                    matrixTable.addCell(createBodyCell(row.get("age").toString(), 8f));
                    matrixTable.addCell(createBodyCell(formatCurrency(row.get("ppfBalance")), 8f));
                    matrixTable.addCell(createBodyCell(formatCurrency(row.get("epfBalance")), 8f));
                    matrixTable.addCell(createBodyCell(formatCurrency(row.get("mfBalance")), 8f));
                    matrixTable.addCell(createBodyCell(formatCurrency(row.get("goalOutflow")), 8f));
                    matrixTable.addCell(createBodyCell(formatCurrency(row.get("netCorpus")), 8f));
                }
                
                document.add(matrixTable);
            }
            
            // Footer
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Report generated by Retyrment - Your Retirement Planning Partner")
                    .setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));
            
            document.close();
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Retirement PDF", e);
        }
    }
    
    public byte[] generateCalendarPdfReport(String userId) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            float bodyFontSize = 10f;
            float titleFontSize = 20f;
            DeviceRgb primaryColor = new DeviceRgb(79, 70, 229);
            
            document.add(new Paragraph("FINANCIAL CALENDAR REPORT")
                    .setFontSize(titleFontSize)
                    .setBold()
                    .setFontColor(primaryColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5));
            
            document.add(new Paragraph("Generated: " + LocalDate.now())
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));
            
            document.add(new Paragraph("Calendar report feature coming soon...")
                    .setFontSize(bodyFontSize)
                    .setTextAlignment(TextAlignment.CENTER));
            
            document.close();
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Calendar PDF", e);
        }
    }
    
    private com.itextpdf.layout.element.Cell createHeaderCell(String text, float fontSize) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text).setBold().setFontSize(fontSize))
                .setBackgroundColor(new DeviceRgb(241, 245, 249))
                .setPadding(8);
    }
    
    private com.itextpdf.layout.element.Cell createBodyCell(String text, float fontSize) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text).setFontSize(fontSize))
                .setPadding(5);
    }
    
    private void addTableRow(Table table, String label, String value, DeviceRgb valueColor, float fontSize) {
        table.addCell(createBodyCell(label, fontSize));
        table.addCell(new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(value).setFontSize(fontSize).setBold().setFontColor(valueColor))
                .setPadding(5));
    }
    
    private String formatCurrency(Object value) {
        if (value == null) return "₹0";
        double num = value instanceof Number ? ((Number) value).doubleValue() : 0;
        return String.format("₹%,.0f", num);
    }

    public byte[] generateExcelReport(String userId) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("₹#,##0"));

            // 1. Net Worth Summary Sheet
            Sheet summarySheet = workbook.createSheet("Net Worth Summary");
            Map<String, Object> netWorth = analysisService.calculateNetWorth(userId);
            int rowNum = 0;
            
            Row titleRow = summarySheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("RETYRMENT FINANCIAL SUMMARY");
            titleCell.setCellStyle(headerStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));
            
            rowNum++;
            createSummaryRow(summarySheet, rowNum++, "Report Date", LocalDate.now().toString());
            rowNum++;
            createSummaryRow(summarySheet, rowNum++, "Total Assets", formatCurrency(netWorth.get("totalAssets")));
            createSummaryRow(summarySheet, rowNum++, "Total Liabilities", formatCurrency(netWorth.get("totalLiabilities")));
            createSummaryRow(summarySheet, rowNum++, "Net Worth", formatCurrency(netWorth.get("netWorth")));
            
            rowNum++;
            Row cashFlowTitle = summarySheet.createRow(rowNum++);
            Cell cashFlowCell = cashFlowTitle.createCell(0);
            cashFlowCell.setCellValue("MONTHLY CASH FLOW");
            cashFlowCell.setCellStyle(headerStyle);
            
            // Calculate monthly cash flow
            List<Income> incomesForExcel = incomeRepository.findByUserId(userId);
            List<Expense> expensesForExcel = expenseRepository.findByUserId(userId);
            List<Loan> loansForExcel = loanRepository.findByUserId(userId);
            List<Insurance> insurancesForExcel = insuranceRepository.findByUserId(userId);
            
            double monthlyIncome = incomesForExcel.stream()
                    .filter(i -> i.getIsActive() != null && i.getIsActive())
                    .mapToDouble(i -> i.getMonthlyAmount() != null ? i.getMonthlyAmount() : 0)
                    .sum();
            
            double monthlyExpenses = expensesForExcel.stream()
                    .mapToDouble(e -> {
                        return e.getMonthlyEquivalent() != null ? e.getMonthlyEquivalent() : 0;
                    })
                    .sum();
            
            double monthlyEMI = loansForExcel.stream()
                    .mapToDouble(l -> l.getEmi() != null ? l.getEmi() : 0)
                    .sum();
            
            double monthlyPremium = insurancesForExcel.stream()
                    .mapToDouble(ins -> {
                        double annual = ins.getAnnualPremium() != null ? ins.getAnnualPremium() : 0;
                        return annual / 12;
                    })
                    .sum();
            
            double monthlySavings = monthlyIncome - monthlyExpenses - monthlyEMI - monthlyPremium;
            
            createSummaryRow(summarySheet, rowNum++, "Total Income", formatCurrency(monthlyIncome));
            createSummaryRow(summarySheet, rowNum++, "Expenses", formatCurrency(monthlyExpenses));
            createSummaryRow(summarySheet, rowNum++, "EMIs", formatCurrency(monthlyEMI));
            createSummaryRow(summarySheet, rowNum++, "Insurance Premiums", formatCurrency(monthlyPremium));
            createSummaryRow(summarySheet, rowNum++, "Monthly Savings", formatCurrency(monthlySavings));
            
            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);

            // 2. Income Sheet
            Sheet incomeSheet = workbook.createSheet("Income");
            List<Income> incomes = incomeRepository.findByUserId(userId);
            rowNum = 0;
            Row incHeader = incomeSheet.createRow(rowNum++);
            String[] incHeaders = {"Source", "Monthly Amount", "Annual Increment %"};
            for (int i = 0; i < incHeaders.length; i++) {
                Cell cell = incHeader.createCell(i);
                cell.setCellValue(incHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            for (Income inc : incomes) {
                Row row = incomeSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(inc.getSource() != null ? inc.getSource() : "");
                row.createCell(1).setCellValue(inc.getMonthlyAmount() != null ? inc.getMonthlyAmount() : 0);
                row.createCell(2).setCellValue(inc.getAnnualIncrement() != null ? inc.getAnnualIncrement() : 0);
            }
            for (int i = 0; i < incHeaders.length; i++) incomeSheet.autoSizeColumn(i);

            // 3. Expenses Sheet
            Sheet expenseSheet = workbook.createSheet("Expenses");
            List<Expense> expenses = expenseRepository.findByUserId(userId);
            rowNum = 0;
            Row expHeader = expenseSheet.createRow(rowNum++);
            String[] expHeaders = {"Category", "Name", "Amount", "Frequency", "Type"};
            for (int i = 0; i < expHeaders.length; i++) {
                Cell cell = expHeader.createCell(i);
                cell.setCellValue(expHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            for (Expense exp : expenses) {
                Row row = expenseSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(exp.getCategory() != null ? exp.getCategory().name() : "");
                row.createCell(1).setCellValue(exp.getName() != null ? exp.getName() : "");
                row.createCell(2).setCellValue(exp.getAmount() != null ? exp.getAmount() : 0);
                row.createCell(3).setCellValue(exp.getFrequency() != null ? exp.getFrequency().name() : "MONTHLY");
                String type = (exp.getIsFixed() != null && exp.getIsFixed() ? "Fixed" : "Variable") + 
                              (exp.getIsTimeBound() != null && exp.getIsTimeBound() ? ", Time-Bound" : "");
                row.createCell(4).setCellValue(type);
            }
            for (int i = 0; i < expHeaders.length; i++) expenseSheet.autoSizeColumn(i);

            // 4. Investments Sheet
            Sheet invSheet = workbook.createSheet("Investments");
            List<Investment> investments = investmentRepository.findByUserId(userId);
            rowNum = 0;
            Row invHeader = invSheet.createRow(rowNum++);
            String[] invHeaders = {"Type", "Name", "Invested", "Current Value", "Gain/Loss", "Monthly SIP"};
            for (int i = 0; i < invHeaders.length; i++) {
                Cell cell = invHeader.createCell(i);
                cell.setCellValue(invHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            for (Investment inv : investments) {
                Row row = invSheet.createRow(rowNum++);
                double invested = inv.getInvestedAmount() != null ? inv.getInvestedAmount() : 0;
                double current = inv.getCurrentValue() != null ? inv.getCurrentValue() : 0;
                double gain = current - invested;
                row.createCell(0).setCellValue(inv.getType() != null ? inv.getType().name() : "");
                row.createCell(1).setCellValue(inv.getName() != null ? inv.getName() : "");
                row.createCell(2).setCellValue(invested);
                row.createCell(3).setCellValue(current);
                row.createCell(4).setCellValue(gain);
                row.createCell(5).setCellValue(inv.getMonthlySip() != null ? inv.getMonthlySip() : 0);
            }
            for (int i = 0; i < invHeaders.length; i++) invSheet.autoSizeColumn(i);

            // 5. Loans Sheet
            Sheet loanSheet = workbook.createSheet("Loans");
            List<Loan> loans = loanRepository.findByUserId(userId);
            rowNum = 0;
            Row loanHeader = loanSheet.createRow(rowNum++);
            String[] loanHeaders = {"Type", "Name", "Outstanding", "EMI", "Interest Rate %", "Remaining Months"};
            for (int i = 0; i < loanHeaders.length; i++) {
                Cell cell = loanHeader.createCell(i);
                cell.setCellValue(loanHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            for (Loan loan : loans) {
                Row row = loanSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(loan.getType() != null ? loan.getType().name() : "");
                row.createCell(1).setCellValue(loan.getName() != null ? loan.getName() : "");
                row.createCell(2).setCellValue(loan.getOutstandingAmount() != null ? loan.getOutstandingAmount() : 0);
                row.createCell(3).setCellValue(loan.getEmi() != null ? loan.getEmi() : 0);
                row.createCell(4).setCellValue(loan.getInterestRate() != null ? loan.getInterestRate() : 0);
                row.createCell(5).setCellValue(loan.getRemainingMonths() != null ? loan.getRemainingMonths() : 0);
            }
            for (int i = 0; i < loanHeaders.length; i++) loanSheet.autoSizeColumn(i);

            // 6. Insurance Sheet
            Sheet insSheet = workbook.createSheet("Insurance");
            List<Insurance> insurances = insuranceRepository.findByUserId(userId);
            rowNum = 0;
            Row insHeader = insSheet.createRow(rowNum++);
            String[] insHeaders = {"Type", "Policy Name", "Company", "Sum Assured", "Annual Premium"};
            for (int i = 0; i < insHeaders.length; i++) {
                Cell cell = insHeader.createCell(i);
                cell.setCellValue(insHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            for (Insurance ins : insurances) {
                Row row = insSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(ins.getType() != null ? ins.getType().name() : "");
                row.createCell(1).setCellValue(ins.getPolicyName() != null ? ins.getPolicyName() : "");
                row.createCell(2).setCellValue(ins.getCompany() != null ? ins.getCompany() : "");
                row.createCell(3).setCellValue(ins.getSumAssured() != null ? ins.getSumAssured() : 0);
                row.createCell(4).setCellValue(ins.getAnnualPremium() != null ? ins.getAnnualPremium() : 0);
            }
            for (int i = 0; i < insHeaders.length; i++) insSheet.autoSizeColumn(i);

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

}
