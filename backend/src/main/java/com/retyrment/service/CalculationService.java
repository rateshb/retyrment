package com.retyrment.service;

import com.retyrment.model.Loan;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CalculationService {

    /**
     * Calculate Future Value of a lumpsum investment
     * FV = PV × (1 + r)^n
     */
    public double calculateFutureValue(double principal, double annualRate, int years) {
        if (years <= 0) return principal;
        return principal * Math.pow(1 + annualRate / 100, years);
    }

    /**
     * Calculate Future Value of SIP (Systematic Investment Plan)
     * FV = PMT × [((1+r)^n - 1) / r] × (1+r)
     */
    public double calculateSIPFutureValue(double monthlyAmount, double annualRate, int years) {
        if (years <= 0 || monthlyAmount <= 0) return 0;
        double monthlyRate = annualRate / 100 / 12;
        int months = years * 12;
        return monthlyAmount * ((Math.pow(1 + monthlyRate, months) - 1) / monthlyRate) * (1 + monthlyRate);
    }

    /**
     * Calculate required monthly SIP to reach a target amount
     */
    public double calculateRequiredSIP(double targetAmount, double annualRate, int years) {
        if (years <= 0) return targetAmount;
        double monthlyRate = annualRate / 100 / 12;
        int months = years * 12;
        double factor = ((Math.pow(1 + monthlyRate, months) - 1) / monthlyRate) * (1 + monthlyRate);
        return targetAmount / factor;
    }

    /**
     * Calculate inflation-adjusted value
     */
    public double calculateInflatedValue(double amount, double inflationRate, int years) {
        return calculateFutureValue(amount, inflationRate, years);
    }

    /**
     * Calculate XIRR (Extended Internal Rate of Return)
     * Simplified version - for accurate XIRR, use a proper financial library
     */
    public double calculateXIRR(List<Double> cashFlows, List<Date> dates) {
        // Simplified implementation - Newton-Raphson method
        double guess = 0.1;
        for (int i = 0; i < 100; i++) {
            double f = 0, df = 0;
            Date firstDate = dates.get(0);
            for (int j = 0; j < cashFlows.size(); j++) {
                double years = (dates.get(j).getTime() - firstDate.getTime()) / (365.25 * 24 * 60 * 60 * 1000);
                f += cashFlows.get(j) / Math.pow(1 + guess, years);
                df -= years * cashFlows.get(j) / Math.pow(1 + guess, years + 1);
            }
            double newGuess = guess - f / df;
            if (Math.abs(newGuess - guess) < 0.0001) return newGuess * 100;
            guess = newGuess;
        }
        return guess * 100;
    }

    /**
     * Calculate absolute returns percentage
     */
    public double calculateAbsoluteReturns(double invested, double currentValue) {
        if (invested <= 0) return 0;
        return ((currentValue - invested) / invested) * 100;
    }

    /**
     * Calculate CAGR (Compound Annual Growth Rate)
     */
    public double calculateCAGR(double initialValue, double finalValue, double years) {
        if (years <= 0 || initialValue <= 0) return 0;
        return (Math.pow(finalValue / initialValue, 1 / years) - 1) * 100;
    }

    /**
     * Generate loan amortization schedule
     */
    public List<Map<String, Object>> calculateAmortization(Loan loan) {
        List<Map<String, Object>> schedule = new ArrayList<>();
        
        double principal = loan.getOutstandingAmount();
        double monthlyRate = loan.getInterestRate() / 100 / 12;
        double emi = loan.getEmi();
        int months = loan.getRemainingMonths();
        
        double balance = principal;
        double totalInterest = 0;
        double totalPrincipal = 0;
        
        for (int month = 1; month <= months && balance > 0; month++) {
            double interestComponent = balance * monthlyRate;
            double principalComponent = Math.min(emi - interestComponent, balance);
            balance -= principalComponent;
            
            totalInterest += interestComponent;
            totalPrincipal += principalComponent;
            
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month);
            row.put("emi", Math.round(emi));
            row.put("principal", Math.round(principalComponent));
            row.put("interest", Math.round(interestComponent));
            row.put("balance", Math.round(Math.max(0, balance)));
            row.put("totalPrincipalPaid", Math.round(totalPrincipal));
            row.put("totalInterestPaid", Math.round(totalInterest));
            
            schedule.add(row);
        }
        
        return schedule;
    }

    /**
     * Calculate EMI from loan details
     * EMI = P × r × (1+r)^n / ((1+r)^n - 1)
     */
    public double calculateEMI(double principal, double annualRate, int months) {
        double monthlyRate = annualRate / 100 / 12;
        return principal * monthlyRate * Math.pow(1 + monthlyRate, months) / 
               (Math.pow(1 + monthlyRate, months) - 1);
    }

    /**
     * Calculate PPF maturity value with variable contributions
     */
    public double calculatePPFMaturity(double currentBalance, double yearlyContribution, 
                                        double rate, int remainingYears) {
        double balance = currentBalance;
        for (int year = 1; year <= remainingYears; year++) {
            // Add contribution at beginning of year (for max interest)
            balance += yearlyContribution;
            // Apply interest
            balance *= (1 + rate / 100);
        }
        return balance;
    }

    /**
     * Calculate step-up SIP future value
     * SIP increases by stepUpPercent every year
     */
    public double calculateStepUpSIPFutureValue(double initialMonthly, double annualRate, 
                                                  double stepUpPercent, int years) {
        double total = 0;
        double currentSIP = initialMonthly;
        double monthlyRate = annualRate / 100 / 12;
        
        for (int year = 1; year <= years; year++) {
            // Calculate FV of this year's SIP at the end of total period
            int monthsRemaining = (years - year + 1) * 12;
            for (int month = 1; month <= 12; month++) {
                int monthsToGrow = monthsRemaining - month + 1;
                total += currentSIP * Math.pow(1 + monthlyRate, monthsToGrow);
            }
            // Step up SIP for next year
            currentSIP *= (1 + stepUpPercent / 100);
        }
        
        return total;
    }
}
