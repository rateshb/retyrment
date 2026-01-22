package com.retyrment.service;

import com.retyrment.model.*;
import com.retyrment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final InvestmentRepository investmentRepository;
    private final LoanRepository loanRepository;
    private final InsuranceRepository insuranceRepository;
    private final CalendarEntryRepository calendarEntryRepository;

    private static final String[] MONTH_NAMES = {
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
            "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    };

    public Map<String, Object> generateYearCalendar(String userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> entries = new ArrayList<>();
        double[] monthlyTotals = new double[12];

        // 1. Add SIPs from Mutual Funds - filter by userId
        investmentRepository.findByUserIdAndMonthlySipGreaterThan(userId, 0.0).forEach(inv -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", inv.getId());
            entry.put("description", inv.getName() + " SIP");
            entry.put("category", "SIP");
            entry.put("autoLinked", true);
            
            // Include SIP day for calendar display
            Integer sipDay = inv.getSipDay();
            entry.put("sipDay", sipDay != null ? sipDay : 1); // Default to 1st if not set
            
            Map<String, Double> months = new LinkedHashMap<>();
            for (int m = 0; m < 12; m++) {
                months.put(MONTH_NAMES[m], inv.getMonthlySip());
                monthlyTotals[m] += inv.getMonthlySip();
            }
            entry.put("months", months);
            entry.put("yearlyTotal", inv.getMonthlySip() * 12);
            entries.add(entry);
        });
        
        // 1b. Add RD (Recurring Deposits) - filter by userId
        investmentRepository.findByUserId(userId).stream()
                .filter(inv -> inv.getType() == Investment.InvestmentType.RD && 
                              inv.getMonthlySip() != null && inv.getMonthlySip() > 0)
                .forEach(inv -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", inv.getId());
                    entry.put("description", inv.getName() + " RD");
                    entry.put("category", "RD");
                    entry.put("autoLinked", true);
                    
                    // Include RD day for calendar display
                    Integer rdDay = inv.getRdDay() != null ? inv.getRdDay() : 
                                   (inv.getSipDay() != null ? inv.getSipDay() : 1);
                    entry.put("sipDay", rdDay);
                    
                    Map<String, Double> months = new LinkedHashMap<>();
                    for (int m = 0; m < 12; m++) {
                        months.put(MONTH_NAMES[m], inv.getMonthlySip());
                        monthlyTotals[m] += inv.getMonthlySip();
                    }
                    entry.put("months", months);
                    entry.put("yearlyTotal", inv.getMonthlySip() * 12);
                    entries.add(entry);
                });

        // 2. Add EMIs from Loans - filter by userId
        loanRepository.findByUserIdAndRemainingMonthsGreaterThan(userId, 0).forEach(loan -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", loan.getId());
            entry.put("description", loan.getName() + " EMI");
            entry.put("category", "EMI");
            entry.put("autoLinked", true);
            entry.put("emiDay", loan.getEmiDay() != null ? loan.getEmiDay() : 1);
            
            Map<String, Double> months = new LinkedHashMap<>();
            int remainingMonths = Math.min(loan.getRemainingMonths(), 12);
            for (int m = 0; m < 12; m++) {
                double amount = m < remainingMonths ? loan.getEmi() : 0;
                months.put(MONTH_NAMES[m], amount);
                monthlyTotals[m] += amount;
            }
            entry.put("months", months);
            entry.put("yearlyTotal", loan.getEmi() * remainingMonths);
            entries.add(entry);
        });

        // 3. Add Insurance Premiums - filter by userId
        insuranceRepository.findByUserId(userId).forEach(ins -> {
            if (ins.getAnnualPremium() != null && ins.getAnnualPremium() > 0) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", ins.getId());
                entry.put("description", ins.getPolicyName() + " Premium");
                entry.put("category", "INSURANCE");
                entry.put("autoLinked", true);
                
                Map<String, Double> months = new LinkedHashMap<>();
                int renewalMonth = ins.getRenewalMonth() != null ? ins.getRenewalMonth() - 1 : 0;
                
                for (int m = 0; m < 12; m++) {
                    double amount = 0;
                    if (ins.getPremiumFrequency() == Insurance.PremiumFrequency.YEARLY) {
                        amount = (m == renewalMonth) ? ins.getAnnualPremium() : 0;
                    } else if (ins.getPremiumFrequency() == Insurance.PremiumFrequency.HALF_YEARLY) {
                        amount = (m == renewalMonth || m == (renewalMonth + 6) % 12) ? 
                                ins.getAnnualPremium() / 2 : 0;
                    } else if (ins.getPremiumFrequency() == Insurance.PremiumFrequency.QUARTERLY) {
                        if (m == renewalMonth || m == (renewalMonth + 3) % 12 || 
                            m == (renewalMonth + 6) % 12 || m == (renewalMonth + 9) % 12) {
                            amount = ins.getAnnualPremium() / 4;
                        }
                    } else if (ins.getPremiumFrequency() == Insurance.PremiumFrequency.MONTHLY) {
                        amount = ins.getAnnualPremium() / 12;
                    }
                    months.put(MONTH_NAMES[m], amount);
                    monthlyTotals[m] += amount;
                }
                entry.put("months", months);
                entry.put("yearlyTotal", ins.getAnnualPremium());
                entries.add(entry);
            }
        });

        // 4. Add PPF/RD contributions - filter by userId
        investmentRepository.findByUserId(userId).stream()
                .filter(inv -> inv.getType() == Investment.InvestmentType.PPF && 
                              inv.getYearlyContribution() != null && inv.getYearlyContribution() > 0)
                .forEach(inv -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", inv.getId());
                    entry.put("description", "PPF Contribution");
                    entry.put("category", "PPF");
                    entry.put("autoLinked", true);
                    
                    // Assume yearly contribution in March (financial year end)
                    Map<String, Double> months = new LinkedHashMap<>();
                    for (int m = 0; m < 12; m++) {
                        double amount = (m == 2) ? inv.getYearlyContribution() : 0; // March
                        months.put(MONTH_NAMES[m], amount);
                        monthlyTotals[m] += amount;
                    }
                    entry.put("months", months);
                    entry.put("yearlyTotal", inv.getYearlyContribution());
                    entries.add(entry);
                });

        // 5. Add manual calendar entries - filter by userId
        calendarEntryRepository.findByUserIdAndIsActiveTrue(userId).forEach(calEntry -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", calEntry.getId());
            entry.put("description", calEntry.getDescription());
            entry.put("category", calEntry.getCategory().name());
            entry.put("autoLinked", false);
            
            Map<String, Double> months = new LinkedHashMap<>();
            double yearlyTotal = 0;
            for (int m = 0; m < 12; m++) {
                double amount = 0;
                if (calEntry.getDueMonths() != null && calEntry.getDueMonths().contains(m + 1)) {
                    amount = calEntry.getAmount();
                }
                months.put(MONTH_NAMES[m], amount);
                monthlyTotals[m] += amount;
                yearlyTotal += amount;
            }
            entry.put("months", months);
            entry.put("yearlyTotal", yearlyTotal);
            entries.add(entry);
        });

        // Build monthly totals map
        Map<String, Double> totals = new LinkedHashMap<>();
        for (int m = 0; m < 12; m++) {
            totals.put(MONTH_NAMES[m], monthlyTotals[m]);
        }

        result.put("entries", entries);
        result.put("monthlyTotals", totals);
        result.put("yearlyGrandTotal", Arrays.stream(monthlyTotals).sum());
        result.put("year", LocalDate.now().getYear());

        return result;
    }

    public Map<String, Object> getMonthCalendar(String userId, int month) {
        Map<String, Object> fullCalendar = generateYearCalendar(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        
        String monthName = MONTH_NAMES[month - 1];
        List<Map<String, Object>> monthEntries = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allEntries = (List<Map<String, Object>>) fullCalendar.get("entries");

        for (Map<String, Object> entry : allEntries) {
            @SuppressWarnings("unchecked")
            Map<String, Double> months = (Map<String, Double>) entry.get("months");
            Double amount = months.get(monthName);
            if (amount != null && amount > 0) {
                Map<String, Object> monthEntry = new LinkedHashMap<>();
                monthEntry.put("id", entry.get("id"));
                monthEntry.put("description", entry.get("description"));
                monthEntry.put("category", entry.get("category"));
                monthEntry.put("amount", amount);
                
                // Include the day of month for SIPs and RDs
                if (entry.get("sipDay") != null) {
                    monthEntry.put("dayOfMonth", entry.get("sipDay"));
                }
                
                monthEntries.add(monthEntry);
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Double> totals = (Map<String, Double>) fullCalendar.get("monthlyTotals");

        result.put("month", month);
        result.put("monthName", monthName);
        result.put("entries", monthEntries);
        result.put("total", totals.get(monthName));

        return result;
    }

    public List<Map<String, Object>> getUpcomingPayments(String userId, int days) {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int nextMonth = (currentMonth % 12) + 1;

        List<Map<String, Object>> upcoming = new ArrayList<>();

        // Get current month and next month entries
        Map<String, Object> currentMonthData = getMonthCalendar(userId, currentMonth);
        Map<String, Object> nextMonthData = getMonthCalendar(userId, nextMonth);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> currentEntries = (List<Map<String, Object>>) currentMonthData.get("entries");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nextEntries = (List<Map<String, Object>>) nextMonthData.get("entries");

        currentEntries.forEach(e -> {
            e.put("dueMonth", currentMonth);
            e.put("isCurrentMonth", true);
            upcoming.add(e);
        });

        nextEntries.forEach(e -> {
            e.put("dueMonth", nextMonth);
            e.put("isCurrentMonth", false);
            upcoming.add(e);
        });

        return upcoming;
    }
}
