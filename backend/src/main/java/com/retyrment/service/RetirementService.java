package com.retyrment.service;

import com.retyrment.model.*;
import com.retyrment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RetirementService {

    private final InvestmentRepository investmentRepository;
    private final InsuranceRepository insuranceRepository;
    private final GoalRepository goalRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final LoanRepository loanRepository;
    private final RetirementScenarioRepository scenarioRepository;
    private final CalculationService calculationService;

    @Value("${app.defaults.inflation-rate}")
    private double defaultInflation;

    @Value("${app.defaults.epf-return}")
    private double defaultEpfReturn;

    @Value("${app.defaults.ppf-return}")
    private double defaultPpfReturn;

    @Value("${app.defaults.mf-equity-return}")
    private double defaultMfReturn;

    public Map<String, Object> generateRetirementMatrix(String userId, RetirementScenario scenario) {
        // Use provided scenario or get default
        if (scenario == null) {
            try {
                scenario = scenarioRepository.findByUserIdAndIsDefaultTrue(userId)
                        .orElse(createDefaultScenario(userId));
            } catch (Exception e) {
                // If there's an issue finding or creating default scenario, create a new one
                scenario = createDefaultScenario(userId);
            }
        }
        
        // Ensure scenario has required fields
        if (scenario.getCurrentAge() == null) scenario.setCurrentAge(35);
        if (scenario.getRetirementAge() == null) scenario.setRetirementAge(60);
        if (scenario.getLifeExpectancy() == null) scenario.setLifeExpectancy(85);
        if (scenario.getInflationRate() == null) scenario.setInflationRate(defaultInflation);

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> matrix = new ArrayList<>();

        int currentYear = LocalDate.now().getYear();
        int currentAge = scenario.getCurrentAge() != null ? scenario.getCurrentAge() : 35;
        int retirementAge = scenario.getRetirementAge() != null ? scenario.getRetirementAge() : 60;
        int yearsToRetirement = retirementAge - currentAge;

        // Get current balances - includes all liquid/investable assets - filter by userId
        double ppfBalance = getInvestmentBalance(userId, Investment.InvestmentType.PPF);
        double epfBalance = getInvestmentBalance(userId, Investment.InvestmentType.EPF);
        double mfBalance = getInvestmentBalance(userId, Investment.InvestmentType.MUTUAL_FUND);
        double npsBalance = getInvestmentBalance(userId, Investment.InvestmentType.NPS);
        
        // Additional liquid assets that contribute to retirement corpus
        double fdBalance = getInvestmentBalance(userId, Investment.InvestmentType.FD);
        double rdBalance = getInvestmentBalance(userId, Investment.InvestmentType.RD);
        double stockBalance = getInvestmentBalance(userId, Investment.InvestmentType.STOCK);
        double cashBalance = getInvestmentBalance(userId, Investment.InvestmentType.CASH);
        
        // Combine into categories for cleaner matrix display
        // "Other Liquid" = FD + RD + STOCK + CASH (shown separately in summary)
        double otherLiquidBalance = fdBalance + rdBalance + stockBalance + cashBalance;

        // Get monthly contributions - filter by userId
        double ppfYearly = getYearlyContribution(userId, Investment.InvestmentType.PPF);
        double epfMonthly = getMonthlyContribution(userId, Investment.InvestmentType.EPF);
        double mfSipMonthly = getMonthlySIP(userId, Investment.InvestmentType.MUTUAL_FUND);
        double npsMonthly = getMonthlyContribution(userId, Investment.InvestmentType.NPS);
        
        // RD monthly contribution (similar to SIP)
        double rdMonthly = getMonthlySIP(userId, Investment.InvestmentType.RD);
        
        // Get average return rates for FD/RD - filter by userId
        double fdReturn = getAverageReturn(userId, Investment.InvestmentType.FD, 7.0); // Default 7%
        double rdReturn = getAverageReturn(userId, Investment.InvestmentType.RD, 6.5); // Default 6.5%

        // Get goals for outflow - filter by userId
        List<Goal> goals = goalRepository.findByUserIdOrderByTargetYearAsc(userId);

        // Get insurance maturities - filter by userId
        List<Insurance> investmentPolicies = insuranceRepository.findByUserIdAndTypeIn(userId,
                Arrays.asList(Insurance.InsuranceType.ULIP, Insurance.InsuranceType.ENDOWMENT, 
                              Insurance.InsuranceType.MONEY_BACK));

        // Use simple scalar values if provided, otherwise fall back to complex period returns or defaults
        double simpleEpfReturn = scenario.getEpfReturn() != null ? scenario.getEpfReturn() : defaultEpfReturn;
        double simplePpfReturn = scenario.getPpfReturn() != null ? scenario.getPpfReturn() : defaultPpfReturn;
        double simpleMfReturn = scenario.getMfReturn() != null ? scenario.getMfReturn() : defaultMfReturn;
        double simpleInflation = scenario.getInflation() != null ? scenario.getInflation() : 
                                 (scenario.getInflationRate() != null ? scenario.getInflationRate() : defaultInflation);
        
        double sipStepUp = scenario.getSipStepup() != null ? scenario.getSipStepup() : 
                          (scenario.getSipStepUpPercent() != null ? scenario.getSipStepUpPercent() : 10);
        double lumpsumYearly = scenario.getLumpsumAmount() != null ? scenario.getLumpsumAmount() : 0;
        
        // Year from which user adjustments take effect (default: 1 = next year)
        int effectiveFromYear = scenario.getEffectiveFromYear() != null ? scenario.getEffectiveFromYear() : 1;
        
        // Income strategy and corpus return settings
        String incomeStrategy = scenario.getIncomeStrategy() != null ? scenario.getIncomeStrategy() : "SUSTAINABLE";
        double corpusReturnRate = scenario.getCorpusReturnRate() != null ? scenario.getCorpusReturnRate() : 10.0;
        double withdrawalRate = scenario.getWithdrawalRate() != null ? scenario.getWithdrawalRate() : 8.0;
        
        // Rate reduction settings (PPF/EPF/FD rates decrease over time)
        boolean enableRateReduction = scenario.getEnableRateReduction() != null ? scenario.getEnableRateReduction() : true;
        double rateReductionPercent = scenario.getRateReductionPercent() != null ? scenario.getRateReductionPercent() : 0.5;
        int rateReductionYears = scenario.getRateReductionYears() != null ? scenario.getRateReductionYears() : 5;

        double cumulativePpf = ppfBalance;
        double cumulativeEpf = epfBalance;
        double cumulativeMf = mfBalance;
        double cumulativeNps = npsBalance;
        double cumulativeOtherLiquid = otherLiquidBalance; // FD + RD + STOCK + CASH
        double currentSip = mfSipMonthly;
        double currentRdSip = rdMonthly;

        for (int year = 0; year <= yearsToRetirement; year++) {
            Map<String, Object> row = new LinkedHashMap<>();
            int calendarYear = currentYear + year;
            int age = currentAge + year;

            // User adjustments only apply from effectiveFromYear onwards
            double ppfRate, epfRate, mfRate;
            if (year < effectiveFromYear) {
                // Before effective year - use defaults from application.yml
                ppfRate = defaultPpfReturn;
                epfRate = defaultEpfReturn;
                mfRate = defaultMfReturn;
            } else {
                // From effective year onwards - use user-adjusted rates
                ppfRate = simplePpfReturn;
                epfRate = simpleEpfReturn;
                mfRate = simpleMfReturn;
            }
            
            // Apply rate reduction over time (PPF/EPF rates typically decrease)
            if (enableRateReduction && year > 0 && rateReductionYears > 0) {
                int reductionPeriods = year / rateReductionYears;
                double totalReduction = reductionPeriods * rateReductionPercent;
                // Apply reduction but ensure rate doesn't go below 4%
                ppfRate = Math.max(4.0, ppfRate - totalReduction);
                epfRate = Math.max(4.0, epfRate - totalReduction);
                // Note: MF rates are market-linked, not reduced
            }

            // Calculate year-end balances
            if (year > 0) {
                // PPF
                cumulativePpf = cumulativePpf * (1 + ppfRate / 100) + ppfYearly;
                
                // EPF
                cumulativeEpf = cumulativeEpf * (1 + epfRate / 100) + (epfMonthly * 12);
                
                // MF (with step-up SIP - only applies from effectiveFromYear onwards)
                double mfGrowth = cumulativeMf * (1 + mfRate / 100);
                double sipValue = calculationService.calculateSIPFutureValue(currentSip, mfRate, 1);
                cumulativeMf = mfGrowth + sipValue + lumpsumYearly;
                
                // Apply SIP step-up only from effectiveFromYear onwards
                if (year >= effectiveFromYear) {
                    currentSip = currentSip * (1 + sipStepUp / 100);
                }
                
                // NPS
                double npsRate = getRateForPeriod(scenario.getMfReturns(), year, 10);
                cumulativeNps = cumulativeNps * (1 + npsRate / 100) + (npsMonthly * 12);
                
                // Other Liquid Assets (FD, RD, STOCK, CASH)
                // Apply rate reduction to FD/RD rates as well
                double reductionFactor = (enableRateReduction && rateReductionYears > 0) ? ((double) year / (double) rateReductionYears) * rateReductionPercent : 0.0;
                double currentFdRate = enableRateReduction ? Math.max(4.0, fdReturn - reductionFactor) : fdReturn;
                double currentRdRate = enableRateReduction ? Math.max(4.0, rdReturn - reductionFactor) : rdReturn;
                
                // Growth: FD/RD at their rates, STOCK at MF rate, CASH at inflation (to preserve value)
                // Simplified: use weighted average of FD/RD rates for the "other liquid" bucket
                double otherLiquidRate = (currentFdRate + currentRdRate) / 2;
                double otherLiquidGrowth = cumulativeOtherLiquid * (1 + otherLiquidRate / 100);
                
                // RD contributions (like SIP)
                double rdSipValue = calculationService.calculateSIPFutureValue(currentRdSip, currentRdRate, 1);
                cumulativeOtherLiquid = otherLiquidGrowth + rdSipValue;
            }

            // Check for insurance maturities (ULIP, Endowment, Money Back)
            double insuranceMaturityInflow = 0;
            List<String> maturingPolicies = new ArrayList<>();
            for (Insurance policy : investmentPolicies) {
                if (policy.getMaturityDate() != null && 
                    policy.getMaturityDate().getYear() == calendarYear) {
                    double maturity = policy.getMaturityBenefit() != null ? 
                            policy.getMaturityBenefit() : (policy.getFundValue() != null ? policy.getFundValue() : 0);
                    insuranceMaturityInflow += maturity;
                    maturingPolicies.add(policy.getPolicyName());
                }
            }
            
            // Check for investment maturities (FD, RD, PPF)
            double investmentMaturityInflow = 0;
            List<String> maturingInvestments = new ArrayList<>();
            for (Investment inv : investmentRepository.findByUserId(userId)) {
                if (inv.getMaturityDate() != null && 
                    inv.getMaturityDate().getYear() == calendarYear) {
                    double maturityValue = calculateExpectedMaturityValue(inv);
                    investmentMaturityInflow += maturityValue;
                    maturingInvestments.add(inv.getName() + " (" + inv.getType() + ")");
                }
            }
            
            // Total inflows for this year
            double totalInflow = insuranceMaturityInflow + investmentMaturityInflow;

            // Check for goal outflows
            double goalOutflow = 0;
            List<String> goalsThisYear = new ArrayList<>();
            for (Goal goal : goals) {
                if (goal.getTargetYear() != null && goal.getTargetYear() == calendarYear 
                        && goal.getTargetAmount() != null) {
                    double inflatedAmount = calculationService.calculateInflatedValue(
                            goal.getTargetAmount(), simpleInflation, year);
                    goalOutflow += inflatedAmount;
                    goalsThisYear.add(goal.getName());
                }
            }

            // Include all liquid assets in total corpus (PPF, EPF, MF, NPS + FD, RD, STOCK, CASH + Inflows)
            double totalCorpus = cumulativePpf + cumulativeEpf + cumulativeMf + cumulativeNps + cumulativeOtherLiquid + totalInflow;
            double netCorpus = totalCorpus - goalOutflow;

            row.put("sno", year + 1);
            row.put("year", calendarYear);
            row.put("age", age);
            row.put("ppfBalance", Math.round(cumulativePpf));
            row.put("ppfRate", ppfRate);
            row.put("epfBalance", Math.round(cumulativeEpf));
            row.put("epfRate", epfRate);
            row.put("mfBalance", Math.round(cumulativeMf));
            row.put("mfRate", mfRate);
            row.put("mfSip", Math.round(currentSip));
            row.put("npsBalance", Math.round(cumulativeNps));
            row.put("otherLiquidBalance", Math.round(cumulativeOtherLiquid)); // FD + RD + STOCK + CASH
            row.put("totalCorpus", Math.round(totalCorpus));
            row.put("insuranceMaturity", Math.round(insuranceMaturityInflow));
            row.put("investmentMaturity", Math.round(investmentMaturityInflow));
            row.put("totalInflow", Math.round(totalInflow));
            row.put("maturingPolicies", maturingPolicies);
            row.put("maturingInvestments", maturingInvestments);
            row.put("goalOutflow", Math.round(goalOutflow));
            row.put("goalsThisYear", goalsThisYear);
            row.put("netCorpus", Math.round(netCorpus));

            matrix.add(row);
        }

        // Calculate retirement income
        Map<String, Object> lastRow = matrix.get(matrix.size() - 1);
        double finalCorpus = ((Number) lastRow.get("netCorpus")).doubleValue();
        int lifeExpectancy = scenario.getLifeExpectancy() != null ? scenario.getLifeExpectancy() : 85;
        int retirementYears = lifeExpectancy - retirementAge;
        
        // Use user-specified or default rates for corpus returns
        double actualCorpusReturn = corpusReturnRate / 100;  // Convert from % to decimal
        double actualWithdrawalRate = withdrawalRate / 100;
        
        // Method 1: Simple depletion (divide corpus by years)
        double monthlyRetirementIncome = finalCorpus / retirementYears / 12;
        
        // Method 2: 4% Safe Withdrawal Rule
        double monthlyIncome4Percent = (finalCorpus * 0.04) / 12;
        
        // Method 3: Sustainable income (user-configurable return and withdrawal rates)
        double yearlyIncomeFromCorpus = finalCorpus * actualWithdrawalRate;
        double monthlyIncomeFromCorpus = yearlyIncomeFromCorpus / 12;
        
        // Determine selected income based on strategy
        double selectedMonthlyIncome;
        String selectedStrategyName;
        switch (incomeStrategy) {
            case "SIMPLE_DEPLETION":
                selectedMonthlyIncome = monthlyRetirementIncome;
                selectedStrategyName = "Simple Depletion";
                break;
            case "SAFE_4_PERCENT":
                selectedMonthlyIncome = monthlyIncome4Percent;
                selectedStrategyName = "4% Safe Withdrawal";
                break;
            case "SUSTAINABLE":
            default:
                selectedMonthlyIncome = monthlyIncomeFromCorpus;
                selectedStrategyName = String.format("Sustainable (%d%% return, %d%% withdrawal)", 
                        Math.round(corpusReturnRate), Math.round(withdrawalRate));
                break;
        }
        
        // Project corpus sustainability over retirement years - based on selected strategy
        List<Map<String, Object>> retirementIncomeProjection = new ArrayList<>();
        double projectedCorpus = finalCorpus;
        
        // Calculate income based on strategy
        for (int year = 0; year <= retirementYears && year <= 30; year += 5) {
            Map<String, Object> projection = new LinkedHashMap<>();
            projection.put("year", year);
            projection.put("age", retirementAge + year);
            projection.put("corpus", Math.round(projectedCorpus));
            
            double monthlyIncome;
            switch (incomeStrategy) {
                case "SIMPLE_DEPLETION":
                    // Simple depletion: divide remaining corpus by remaining years
                    int remainingYears = retirementYears - year;
                    monthlyIncome = remainingYears > 0 ? projectedCorpus / remainingYears / 12 : 0;
                    projection.put("monthlyIncome", Math.round(monthlyIncome));
                    // Corpus depletes proportionally
                    for (int m = 0; m < 5 && (year + m) < retirementYears; m++) {
                        projectedCorpus = projectedCorpus - (monthlyIncome * 12);
                    }
                    break;
                case "SAFE_4_PERCENT":
                    // 4% withdrawal rule - fixed percentage of initial corpus
                    double annual4PercentWithdrawal = finalCorpus * 0.04;
                    monthlyIncome = annual4PercentWithdrawal / 12;
                    projection.put("monthlyIncome", Math.round(monthlyIncome));
                    // Assuming 6% growth, 4% withdrawal = 2% net growth
                    for (int m = 0; m < 5 && (year + m) < retirementYears; m++) {
                        projectedCorpus = projectedCorpus * 1.02;  // 2% net growth per year
                    }
                    break;
                case "SUSTAINABLE":
                default:
                    // Sustainable: withdrawal based on configured rate
                    monthlyIncome = projectedCorpus * actualWithdrawalRate / 12;
                    projection.put("monthlyIncome", Math.round(monthlyIncome));
                    // Corpus after this year: grows by return rate, minus withdrawal
                    for (int m = 0; m < 5 && (year + m) < retirementYears; m++) {
                        projectedCorpus = projectedCorpus * (1 + actualCorpusReturn) - (projectedCorpus * actualWithdrawalRate);
                    }
                    break;
            }
            
            retirementIncomeProjection.add(projection);
        }

        // Calculate GAP Analysis using selected income strategy
        Map<String, Object> gapAnalysis = calculateGapAnalysis(
                userId,
                finalCorpus, 
                simpleInflation, 
                yearsToRetirement, 
                retirementYears,
                goals,
                incomeStrategy,
                corpusReturnRate,
                withdrawalRate
        );

        result.put("matrix", matrix);
        
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("currentAge", currentAge);
        summary.put("retirementAge", retirementAge);
        summary.put("yearsToRetirement", yearsToRetirement);
        summary.put("finalCorpus", Math.round(finalCorpus));
        summary.put("lifeExpectancy", lifeExpectancy);
        summary.put("retirementYears", retirementYears);
        summary.put("monthlyRetirementIncome", Math.round(monthlyRetirementIncome));
        summary.put("monthlyIncome4Percent", Math.round(monthlyIncome4Percent));
        summary.put("monthlyIncomeFromCorpus", Math.round(monthlyIncomeFromCorpus));
        summary.put("yearlyIncomeFromCorpus", Math.round(yearlyIncomeFromCorpus));
        summary.put("corpusReturnRate", corpusReturnRate);
        summary.put("withdrawalRate", withdrawalRate);
        summary.put("incomeStrategy", incomeStrategy);
        summary.put("selectedMonthlyIncome", Math.round(selectedMonthlyIncome));
        summary.put("selectedStrategyName", selectedStrategyName);
        summary.put("effectiveFromYear", effectiveFromYear);
        summary.put("retirementIncomeProjection", retirementIncomeProjection);
        summary.put("scenario", scenario.getName() != null ? scenario.getName() : "Default");
        
        // Starting balances breakdown (for transparency)
        Map<String, Object> startingBalances = new LinkedHashMap<>();
        startingBalances.put("ppf", Math.round(ppfBalance));
        startingBalances.put("epf", Math.round(epfBalance));
        startingBalances.put("mutualFunds", Math.round(mfBalance));
        startingBalances.put("nps", Math.round(npsBalance));
        startingBalances.put("fd", Math.round(fdBalance));
        startingBalances.put("rd", Math.round(rdBalance));
        startingBalances.put("stocks", Math.round(stockBalance));
        startingBalances.put("cash", Math.round(cashBalance));
        startingBalances.put("otherLiquidTotal", Math.round(otherLiquidBalance));
        startingBalances.put("totalStarting", Math.round(ppfBalance + epfBalance + mfBalance + npsBalance + otherLiquidBalance));
        summary.put("startingBalances", startingBalances);
        
        // Note about excluded assets
        summary.put("excludedFromCorpus", "Gold, Real Estate, Crypto (illiquid assets)");
        
        // Calculate SIP Step-Up Optimization: Find when step-up can stop
        Map<String, Object> gapData = gapAnalysis;
        double requiredCorpus = gapData.get("requiredCorpus") != null ? 
                ((Number) gapData.get("requiredCorpus")).doubleValue() : finalCorpus;
        
        Map<String, Object> sipStepUpOptimization = calculateSipStepUpOptimization(
                mfBalance, mfSipMonthly, simpleMfReturn, sipStepUp, 
                yearsToRetirement, requiredCorpus, finalCorpus, effectiveFromYear
        );
        summary.put("sipStepUpOptimization", sipStepUpOptimization);
        
        // Add step-up info to each matrix row
        Integer optimalStopYear = sipStepUpOptimization.get("optimalStopYear") != null ?
                ((Number) sipStepUpOptimization.get("optimalStopYear")).intValue() : null;
        for (int i = 0; i < matrix.size(); i++) {
            Map<String, Object> row = matrix.get(i);
            int year = i;
            boolean isStepUpActive = (optimalStopYear == null || year < optimalStopYear) && year >= effectiveFromYear;
            row.put("sipStepUpActive", isStepUpActive);
            row.put("sipStepUpStopYear", optimalStopYear != null ? currentYear + optimalStopYear : null);
        }
        
        result.put("summary", summary);
        
        result.put("gapAnalysis", gapAnalysis);
        
        // Add maturing investments before retirement
        result.put("maturingBeforeRetirement", calculateMaturingBeforeRetirement(userId, currentAge, retirementAge));

        return result;
    }

    private RetirementScenario createDefaultScenario(String userId) {
        RetirementScenario scenario = RetirementScenario.builder()
                .userId(userId)
                .name("Default")
                .currentAge(35)
                .retirementAge(60)
                .lifeExpectancy(85)
                .inflationRate(defaultInflation)
                .sipStepUpPercent(10.0)
                .lumpsumAmount(0.0)
                .isDefault(true)
                .build();
        // Save the default scenario so it can be reused
        return scenarioRepository.save(scenario);
    }

    private double getRateForPeriod(List<RetirementScenario.PeriodReturn> periodReturns, 
                                     int year, double defaultRate) {
        if (periodReturns == null || periodReturns.isEmpty()) {
            return defaultRate;
        }
        for (RetirementScenario.PeriodReturn pr : periodReturns) {
            if (year >= pr.getFromYear() && year <= pr.getToYear()) {
                return pr.getRate();
            }
        }
        return defaultRate;
    }

    private double getInvestmentBalance(String userId, Investment.InvestmentType type) {
        return investmentRepository.findByUserIdAndType(userId, type).stream()
                .mapToDouble(i -> i.getCurrentValue() != null ? i.getCurrentValue() : 
                                  (i.getInvestedAmount() != null ? i.getInvestedAmount() : 0))
                .sum();
    }

    private double getYearlyContribution(String userId, Investment.InvestmentType type) {
        return investmentRepository.findByUserIdAndType(userId, type).stream()
                .mapToDouble(i -> i.getYearlyContribution() != null ? i.getYearlyContribution() : 0)
                .sum();
    }

    private double getMonthlyContribution(String userId, Investment.InvestmentType type) {
        return investmentRepository.findByUserIdAndType(userId, type).stream()
                .mapToDouble(i -> i.getMonthlySip() != null ? i.getMonthlySip() : 0)
                .sum();
    }

    private double getMonthlySIP(String userId, Investment.InvestmentType type) {
        return investmentRepository.findByUserIdAndType(userId, type).stream()
                .mapToDouble(i -> i.getMonthlySip() != null ? i.getMonthlySip() : 0)
                .sum();
    }

    /**
     * Get average expected return rate for a type of investment.
     * Uses interestRate for FD/RD, expectedReturn for others.
     */
    private double getAverageReturn(String userId, Investment.InvestmentType type, double defaultRate) {
        List<Investment> investments = investmentRepository.findByUserIdAndType(userId, type);
        if (investments.isEmpty()) {
            return defaultRate;
        }
        
        double totalValue = 0;
        double weightedReturn = 0;
        
        for (Investment inv : investments) {
            double value = inv.getCurrentValue() != null ? inv.getCurrentValue() : 
                          (inv.getInvestedAmount() != null ? inv.getInvestedAmount() : 0);
            double rate = inv.getInterestRate() != null ? inv.getInterestRate() : 
                         (inv.getExpectedReturn() != null ? inv.getExpectedReturn() : defaultRate);
            
            totalValue += value;
            weightedReturn += value * rate;
        }
        
        return totalValue > 0 ? weightedReturn / totalValue : defaultRate;
    }

    /**
     * Calculate optimal year to stop SIP step-up while still achieving target corpus.
     * Simulates different stop years and finds the earliest one that meets the required corpus.
     */
    private Map<String, Object> calculateSipStepUpOptimization(
            double mfBalance, double mfSipMonthly, double mfReturn, double sipStepUp,
            int yearsToRetirement, double requiredCorpus, double currentProjectedCorpus, int effectiveFromYear) {
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        // If no step-up configured or already at target, no optimization needed
        if (sipStepUp <= 0 || yearsToRetirement <= 0 || mfSipMonthly <= 0) {
            result.put("optimalStopYear", null);
            result.put("canStopEarly", false);
            result.put("reason", "No SIP step-up configured or no SIP investments");
            result.put("currentProjectedCorpus", Math.round(currentProjectedCorpus));
            result.put("requiredCorpus", Math.round(requiredCorpus));
            return result;
        }
        
        // If already under required corpus even with full step-up, can't stop early
        if (currentProjectedCorpus < requiredCorpus) {
            result.put("optimalStopYear", null);
            result.put("canStopEarly", false);
            result.put("reason", "Current projection already below required corpus - continue step-up");
            result.put("currentProjectedCorpus", Math.round(currentProjectedCorpus));
            result.put("requiredCorpus", Math.round(requiredCorpus));
            result.put("deficit", Math.round(requiredCorpus - currentProjectedCorpus));
            return result;
        }
        
        // Binary search to find optimal stop year (earliest year where we still meet target)
        int optimalStopYear = yearsToRetirement; // Default: continue until retirement
        double corpusAtOptimalStop = currentProjectedCorpus;
        
        // Calculate corpus for each possible stop year
        List<Map<String, Object>> scenarioAnalysis = new ArrayList<>();
        
        for (int stopYear = effectiveFromYear; stopYear <= yearsToRetirement; stopYear++) {
            double projectedCorpus = simulateCorpusWithStepUpStop(
                    mfBalance, mfSipMonthly, mfReturn, sipStepUp, 
                    yearsToRetirement, stopYear, effectiveFromYear
            );
            
            Map<String, Object> scenario = new LinkedHashMap<>();
            scenario.put("stopYear", stopYear);
            scenario.put("projectedCorpus", Math.round(projectedCorpus));
            scenario.put("meetsTarget", projectedCorpus >= requiredCorpus);
            scenario.put("surplus", Math.round(projectedCorpus - requiredCorpus));
            
            // Calculate final SIP at stop point
            double finalSip = mfSipMonthly;
            for (int y = effectiveFromYear; y < stopYear; y++) {
                finalSip = finalSip * (1 + sipStepUp / 100);
            }
            scenario.put("finalSipAtStop", Math.round(finalSip));
            
            scenarioAnalysis.add(scenario);
            
            // Find optimal (earliest year that still meets target)
            if (projectedCorpus >= requiredCorpus && stopYear < optimalStopYear) {
                optimalStopYear = stopYear;
                corpusAtOptimalStop = projectedCorpus;
            }
        }
        
        // Find first year that meets target
        Integer firstMeetingYear = null;
        for (Map<String, Object> scenario : scenarioAnalysis) {
            if ((Boolean) scenario.get("meetsTarget")) {
                firstMeetingYear = ((Number) scenario.get("stopYear")).intValue();
                break;
            }
        }
        
        // Calculate savings from stopping early
        double sipAtFullStepUp = mfSipMonthly;
        for (int y = effectiveFromYear; y < yearsToRetirement; y++) {
            sipAtFullStepUp = sipAtFullStepUp * (1 + sipStepUp / 100);
        }
        
        double sipAtOptimalStop = mfSipMonthly;
        for (int y = effectiveFromYear; y < optimalStopYear; y++) {
            sipAtOptimalStop = sipAtOptimalStop * (1 + sipStepUp / 100);
        }
        
        double monthlyRelief = sipAtFullStepUp - sipAtOptimalStop;
        
        result.put("optimalStopYear", firstMeetingYear);
        result.put("canStopEarly", firstMeetingYear != null && firstMeetingYear < yearsToRetirement);
        result.put("currentProjectedCorpus", Math.round(currentProjectedCorpus));
        result.put("corpusAtOptimalStop", Math.round(corpusAtOptimalStop));
        result.put("requiredCorpus", Math.round(requiredCorpus));
        result.put("sipAtStart", Math.round(mfSipMonthly));
        result.put("sipAtFullStepUp", Math.round(sipAtFullStepUp));
        result.put("sipAtOptimalStop", Math.round(sipAtOptimalStop));
        result.put("monthlyReliefFromStoppingEarly", Math.round(monthlyRelief));
        result.put("yearsOfStepUp", firstMeetingYear != null ? firstMeetingYear - effectiveFromYear : yearsToRetirement - effectiveFromYear);
        result.put("yearsWithoutStepUp", firstMeetingYear != null ? yearsToRetirement - firstMeetingYear : 0);
        result.put("scenarios", scenarioAnalysis);
        
        // Recommendation
        if (firstMeetingYear != null && firstMeetingYear < yearsToRetirement) {
            int yearsSaved = yearsToRetirement - firstMeetingYear;
            result.put("recommendation", String.format(
                    "You can stop SIP step-up after year %d (age %d) and still meet your target. " +
                    "This saves %d years of step-up, reducing monthly SIP burden by ₹%,d in final years.",
                    firstMeetingYear, firstMeetingYear + 35, yearsSaved, Math.round(monthlyRelief)
            ));
        } else {
            result.put("recommendation", "Continue SIP step-up until retirement to meet your target corpus.");
        }
        
        return result;
    }
    
    /**
     * Simulate corpus growth with SIP step-up stopping at a specific year.
     */
    private double simulateCorpusWithStepUpStop(
            double mfBalance, double mfSipMonthly, double mfReturn, double sipStepUp,
            int yearsToRetirement, int stepUpStopYear, int effectiveFromYear) {
        
        double corpus = mfBalance;
        double currentSip = mfSipMonthly;
        
        for (int year = 1; year <= yearsToRetirement; year++) {
            // Growth on existing corpus
            corpus = corpus * (1 + mfReturn / 100);
            
            // SIP contribution for this year
            double sipContribution = calculationService.calculateSIPFutureValue(currentSip, mfReturn, 1);
            corpus += sipContribution;
            
            // Apply step-up only if before stop year and after effective year
            if (year >= effectiveFromYear && year < stepUpStopYear) {
                currentSip = currentSip * (1 + sipStepUp / 100);
            }
        }
        
        return corpus;
    }
    
    /**
     * Calculate GAP analysis between projected corpus and required corpus
     * Required corpus is based on:
     * 1. Goals (if defined)
     * 2. Current monthly expenses inflated to retirement year (if no goals)
     * 3. Insurance premiums that continue after retirement (Term Life, Health Insurance)
     * 4. The selected income strategy affects how much corpus is required
     */
    private Map<String, Object> calculateGapAnalysis(
            String userId,
            double projectedCorpus,
            double inflationRate,
            int yearsToRetirement,
            int retirementYears,
            List<Goal> goals,
            String incomeStrategy,
            double corpusReturnRate,
            double withdrawalRate) {
        
        Map<String, Object> gap = new LinkedHashMap<>();
        
        int currentYear = LocalDate.now().getYear();
        int retirementYear = currentYear + yearsToRetirement;
        
        // Get all expenses and separate into time-bound and recurring
        List<Expense> allExpenses = expenseRepository.findByUserId(userId);
        
        // Calculate current monthly expenses (using new getMonthlyEquivalent method)
        double currentMonthlyExpenses = allExpenses.stream()
                .mapToDouble(Expense::getMonthlyEquivalent)
                .sum();
        
        // Separate time-bound expenses that end before retirement
        List<Expense> timeBoundExpenses = allExpenses.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsTimeBound()))
                .toList();
        
        List<Expense> recurringExpenses = allExpenses.stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsTimeBound()))
                .toList();
        
        // Calculate expenses that continue after retirement
        double expensesContinuingAfterRetirement = allExpenses.stream()
                .filter(e -> e.willContinueAfterRetirement(retirementYear))
                .mapToDouble(Expense::getMonthlyEquivalent)
                .sum();
        
        // Calculate expenses that end before retirement (investment opportunity)
        List<Map<String, Object>> endingExpenses = new ArrayList<>();
        double monthlyFreedUpByRetirement = 0;
        
        for (Expense expense : timeBoundExpenses) {
            Integer endYear = expense.calculateEndYear();
            if (endYear != null && endYear < retirementYear) {
                Map<String, Object> expenseInfo = new LinkedHashMap<>();
                expenseInfo.put("name", expense.getName());
                expenseInfo.put("category", expense.getCategory().toString());
                expenseInfo.put("monthlyAmount", Math.round(expense.getMonthlyEquivalent()));
                expenseInfo.put("yearlyAmount", Math.round(expense.getYearlyAmount()));
                expenseInfo.put("endYear", endYear);
                expenseInfo.put("dependentName", expense.getDependentName());
                expenseInfo.put("yearsRemaining", endYear - currentYear);
                
                // Calculate potential corpus if this money is invested after expense ends
                int yearsToInvest = retirementYear - endYear;
                double potentialCorpus = calculateSIPFutureValue(expense.getMonthlyEquivalent(), 12.0, yearsToInvest);
                expenseInfo.put("potentialCorpusIfInvested", Math.round(potentialCorpus));
                
                endingExpenses.add(expenseInfo);
                monthlyFreedUpByRetirement += expense.getMonthlyEquivalent();
            }
        }
        
        // Calculate insurance premiums that continue after retirement - filter by userId
        List<Insurance> allInsurance = insuranceRepository.findByUserId(userId);
        double monthlyInsurancePremiumsAfterRetirement = 0;
        List<Map<String, Object>> continuingInsurance = new ArrayList<>();
        
        for (Insurance policy : allInsurance) {
            // Check if this insurance continues after retirement
            boolean continuesAfterRetirement = shouldContinueAfterRetirement(policy);
            
            if (continuesAfterRetirement && policy.getAnnualPremium() != null) {
                double monthlyPremium = policy.getAnnualPremium() / 12;
                monthlyInsurancePremiumsAfterRetirement += monthlyPremium;
                
                Map<String, Object> policyInfo = new LinkedHashMap<>();
                policyInfo.put("name", policy.getPolicyName());
                policyInfo.put("type", policy.getType().toString());
                policyInfo.put("healthType", policy.getHealthType() != null ? policy.getHealthType().toString() : null);
                policyInfo.put("annualPremium", policy.getAnnualPremium());
                policyInfo.put("monthlyPremium", monthlyPremium);
                policyInfo.put("coverageEndAge", policy.getCoverageEndAge());
                continuingInsurance.add(policyInfo);
            }
        }
        
        // Total monthly expenses including insurance premiums that continue
        double totalCurrentMonthlyExpenses = currentMonthlyExpenses + monthlyInsurancePremiumsAfterRetirement;
        
        // Monthly expenses that will continue after retirement (excluding time-bound that end)
        double monthlyExpensesContinuingAfterRetirement = expensesContinuingAfterRetirement + monthlyInsurancePremiumsAfterRetirement;
        
        // Note: Loans are assumed to have ended by retirement ideally
        
        // Calculate required corpus based on goals or expenses
        double totalGoalAmount = goals.stream()
                .mapToDouble(g -> g.getTargetAmount() != null ? g.getTargetAmount() : 0)
                .sum();
        
        // Monthly expense at retirement = ONLY expenses that continue * (1 + inflation)^years
        // This is more accurate as time-bound expenses (school fees, etc.) will have ended
        double inflatedMonthlyExpense = monthlyExpensesContinuingAfterRetirement * 
                Math.pow(1 + inflationRate / 100, yearsToRetirement);
        
        // Yearly expense at retirement
        double yearlyExpenseAtRetirement = inflatedMonthlyExpense * 12;
        
        // Calculate required corpus based on selected income strategy
        double requiredCorpusForExpenses;
        String strategyExplanation;
        
        switch (incomeStrategy) {
            case "SIMPLE_DEPLETION":
                // Need corpus that lasts for retirement years (depletes to zero)
                // Required = yearly_expense * years (no growth during retirement)
                requiredCorpusForExpenses = 0;
                for (int year = 0; year < retirementYears; year++) {
                    double yearlyExpense = yearlyExpenseAtRetirement * Math.pow(1 + inflationRate / 100, year);
                    requiredCorpusForExpenses += yearlyExpense;
                }
                strategyExplanation = String.format("Corpus depletes over %d years", retirementYears);
                break;
                
            case "SAFE_4_PERCENT":
                // 4% rule = 25x yearly expenses
                requiredCorpusForExpenses = yearlyExpenseAtRetirement * 25;
                strategyExplanation = "25x yearly expenses (4% rule)";
                break;
                
            case "SUSTAINABLE":
            default:
                // Sustainable: corpus generates returns > withdrawal
                // Required = yearly_expense / withdrawal_rate
                // E.g., if you need ₹12L/year and withdraw 8%, need ₹1.5Cr corpus
                double actualWithdrawalRate = withdrawalRate / 100;
                if (actualWithdrawalRate > 0) {
                    requiredCorpusForExpenses = yearlyExpenseAtRetirement / actualWithdrawalRate;
                } else {
                    requiredCorpusForExpenses = yearlyExpenseAtRetirement * 25; // fallback to 4%
                }
                strategyExplanation = String.format("Yearly expense ÷ %d%% withdrawal", Math.round(withdrawalRate));
                break;
        }
        
        // Add goal amounts (already inflated in the matrix)
        double requiredCorpus = requiredCorpusForExpenses + totalGoalAmount;
        
        // Calculate the gap
        double corpusGap = requiredCorpus - projectedCorpus;
        double gapPercent = requiredCorpus > 0 ? (corpusGap / requiredCorpus) * 100 : 0;
        
        // Calculate additional monthly investment needed to close the gap
        // Using simple calculation: gap / (years * 12 months * average return factor)
        double additionalMonthlySIP = 0;
        if (corpusGap > 0 && yearsToRetirement > 0) {
            // Using formula for SIP required for target amount
            // FV = SIP * [((1+r)^n - 1) / r]
            // SIP = FV * r / ((1+r)^n - 1)
            double monthlyRate = 0.10 / 12; // Assuming 10% annual return
            int totalMonths = yearsToRetirement * 12;
            double factor = Math.pow(1 + monthlyRate, totalMonths) - 1;
            additionalMonthlySIP = corpusGap * monthlyRate / factor;
        }
        
        // Suggestions to close the gap
        List<Map<String, Object>> suggestions = new ArrayList<>();
        
        if (corpusGap > 0) {
            // Suggestion 1: Increase SIP
            Map<String, Object> sipSuggestion = new LinkedHashMap<>();
            sipSuggestion.put("icon", "💰");
            sipSuggestion.put("title", "Increase Monthly SIP");
            sipSuggestion.put("description", String.format("Increase your monthly SIP by ₹%,d to close the gap", 
                    Math.round(additionalMonthlySIP)));
            sipSuggestion.put("impact", "high");
            suggestions.add(sipSuggestion);
            
            // Suggestion 2: Reduce discretionary expenses
            double expenseReduction = currentMonthlyExpenses * 0.10; // 10% reduction
            Map<String, Object> expenseSuggestion = new LinkedHashMap<>();
            expenseSuggestion.put("icon", "✂️");
            expenseSuggestion.put("title", "Reduce Discretionary Expenses");
            expenseSuggestion.put("description", String.format("Cutting ₹%,d/month from expenses and investing it can help", 
                    Math.round(expenseReduction)));
            expenseSuggestion.put("impact", "medium");
            suggestions.add(expenseSuggestion);
            
            // Suggestion 3: Extend working years
            if (yearsToRetirement < 25) {
                Map<String, Object> workSuggestion = new LinkedHashMap<>();
                workSuggestion.put("icon", "⏰");
                workSuggestion.put("title", "Consider Delayed Retirement");
                workSuggestion.put("description", "Working 2-3 more years can significantly boost your corpus");
                workSuggestion.put("impact", "high");
                suggestions.add(workSuggestion);
            }
            
            // Suggestion 4: Higher equity allocation
            Map<String, Object> equitySuggestion = new LinkedHashMap<>();
            equitySuggestion.put("icon", "📈");
            equitySuggestion.put("title", "Review Asset Allocation");
            equitySuggestion.put("description", "Higher equity allocation early on may provide better returns");
            equitySuggestion.put("impact", "medium");
            suggestions.add(equitySuggestion);
        } else {
            // User is on track
            Map<String, Object> onTrackSuggestion = new LinkedHashMap<>();
            onTrackSuggestion.put("icon", "✅");
            onTrackSuggestion.put("title", "You're On Track!");
            onTrackSuggestion.put("description", "Your projected corpus exceeds your retirement needs. Great job!");
            onTrackSuggestion.put("impact", "positive");
            suggestions.add(onTrackSuggestion);
        }
        
        // Build expense projection table (includes insurance premiums that continue after retirement)
        List<Map<String, Object>> expenseProjection = new ArrayList<>();
        
        // Show current breakdown
        Map<String, Object> currentProj = new LinkedHashMap<>();
        currentProj.put("year", 0);
        currentProj.put("label", "Current");
        currentProj.put("monthlyExpense", Math.round(totalCurrentMonthlyExpenses));
        currentProj.put("yearlyExpense", Math.round(totalCurrentMonthlyExpenses * 12));
        currentProj.put("householdExpense", Math.round(currentMonthlyExpenses));
        currentProj.put("insurancePremium", Math.round(monthlyInsurancePremiumsAfterRetirement));
        expenseProjection.add(currentProj);
        
        // Projection at 5-year intervals
        for (int year = 5; year <= Math.min(yearsToRetirement, 15); year += 5) {
            Map<String, Object> proj = new LinkedHashMap<>();
            double inflatedTotal = totalCurrentMonthlyExpenses * Math.pow(1 + inflationRate / 100, year);
            double inflatedHousehold = currentMonthlyExpenses * Math.pow(1 + inflationRate / 100, year);
            double inflatedInsurance = monthlyInsurancePremiumsAfterRetirement * Math.pow(1 + inflationRate / 100, year);
            
            proj.put("year", year);
            proj.put("monthlyExpense", Math.round(inflatedTotal));
            proj.put("yearlyExpense", Math.round(inflatedTotal * 12));
            proj.put("householdExpense", Math.round(inflatedHousehold));
            proj.put("insurancePremium", Math.round(inflatedInsurance));
            expenseProjection.add(proj);
        }
        
        // Add retirement year
        Map<String, Object> retirementProj = new LinkedHashMap<>();
        retirementProj.put("year", yearsToRetirement);
        retirementProj.put("label", "At Retirement");
        retirementProj.put("monthlyExpense", Math.round(inflatedMonthlyExpense));
        retirementProj.put("yearlyExpense", Math.round(yearlyExpenseAtRetirement));
        double inflatedHouseholdAtRetirement = currentMonthlyExpenses * Math.pow(1 + inflationRate / 100, yearsToRetirement);
        double inflatedInsuranceAtRetirement = monthlyInsurancePremiumsAfterRetirement * Math.pow(1 + inflationRate / 100, yearsToRetirement);
        retirementProj.put("householdExpense", Math.round(inflatedHouseholdAtRetirement));
        retirementProj.put("insurancePremium", Math.round(inflatedInsuranceAtRetirement));
        expenseProjection.add(retirementProj);
        
        // Calculate total monthly income from income sources - filter by userId
        double totalMonthlyIncome = incomeRepository.findByUserId(userId).stream()
                .mapToDouble(i -> i.getMonthlyAmount() != null ? i.getMonthlyAmount() : 0)
                .sum();
        
        // Calculate monthly EMIs from active loans - filter by userId
        double totalMonthlyEMI = loanRepository.findByUserId(userId).stream()
                .filter(l -> l.getOutstandingAmount() != null && l.getOutstandingAmount() > 0)
                .mapToDouble(l -> l.getEmi() != null ? l.getEmi() : 0)
                .sum();
        
        // Calculate current monthly SIP/investments - filter by userId
        double totalMonthlySIP = investmentRepository.findByUserId(userId).stream()
                .mapToDouble(i -> i.getMonthlySip() != null ? i.getMonthlySip() : 0)
                .sum();
        
        // Net monthly savings = Income - Expenses - EMIs - SIPs
        double netMonthlySavings = totalMonthlyIncome - totalCurrentMonthlyExpenses - totalMonthlyEMI;
        double availableMonthlySavings = netMonthlySavings - totalMonthlySIP; // After existing SIPs
        
        gap.put("monthlyIncome", Math.round(totalMonthlyIncome));
        gap.put("currentMonthlyExpenses", Math.round(currentMonthlyExpenses));
        gap.put("monthlyInsurancePremiums", Math.round(monthlyInsurancePremiumsAfterRetirement));
        gap.put("totalCurrentMonthlyExpenses", Math.round(totalCurrentMonthlyExpenses));
        gap.put("monthlyEMI", Math.round(totalMonthlyEMI));
        gap.put("monthlySIP", Math.round(totalMonthlySIP));
        gap.put("netMonthlySavings", Math.round(netMonthlySavings));
        gap.put("availableMonthlySavings", Math.round(availableMonthlySavings));
        gap.put("inflatedMonthlyExpenses", Math.round(inflatedMonthlyExpense));
        gap.put("yearlyExpenseAtRetirement", Math.round(yearlyExpenseAtRetirement));
        gap.put("requiredCorpus", Math.round(requiredCorpus));
        gap.put("requiredCorpusForExpenses", Math.round(requiredCorpusForExpenses));
        gap.put("projectedCorpus", Math.round(projectedCorpus));
        gap.put("corpusGap", Math.round(corpusGap));
        gap.put("gapPercent", Math.round(gapPercent * 10) / 10.0);
        gap.put("isOnTrack", corpusGap <= 0);
        gap.put("additionalSIPRequired", Math.round(additionalMonthlySIP));
        gap.put("totalGoalAmount", Math.round(totalGoalAmount));
        gap.put("continuingInsurance", continuingInsurance);
        gap.put("incomeStrategy", incomeStrategy);
        gap.put("strategyExplanation", strategyExplanation);
        gap.put("corpusReturnRate", corpusReturnRate);
        gap.put("withdrawalRate", withdrawalRate);
        gap.put("suggestions", suggestions);
        gap.put("expenseProjection", expenseProjection);
        
        // Time-bound expense analysis
        gap.put("timeBoundExpenseCount", timeBoundExpenses.size());
        gap.put("recurringExpenseCount", recurringExpenses.size());
        gap.put("monthlyExpensesContinuingAfterRetirement", Math.round(monthlyExpensesContinuingAfterRetirement));
        gap.put("monthlyFreedUpByRetirement", Math.round(monthlyFreedUpByRetirement));
        gap.put("yearlyFreedUpByRetirement", Math.round(monthlyFreedUpByRetirement * 12));
        gap.put("endingExpensesBeforeRetirement", endingExpenses);
        
        // Calculate total potential corpus from freed-up expenses
        double totalPotentialFromFreedUp = endingExpenses.stream()
                .mapToDouble(e -> ((Number) e.get("potentialCorpusIfInvested")).doubleValue())
                .sum();
        gap.put("potentialCorpusFromFreedUpExpenses", Math.round(totalPotentialFromFreedUp));
        
        return gap;
    }
    
    /**
     * Determine if an insurance policy's premium continues after retirement.
     * 
     * Rules:
     * - TERM_LIFE: Continues if explicitly marked or until premiumEndAge/coverageEndAge
     * - HEALTH (PERSONAL, FAMILY_FLOATER): Continues after retirement
     * - HEALTH (GROUP): Does NOT continue (employer-provided, ends at retirement)
     * - ULIP, ENDOWMENT, MONEY_BACK: Does NOT continue (has maturity)
     * - VEHICLE: Does NOT continue (not a life-long expense)
     * - If continuesAfterRetirement is explicitly set, use that value
     */
    private boolean shouldContinueAfterRetirement(Insurance policy) {
        // If explicitly set, use that value
        if (policy.getContinuesAfterRetirement() != null) {
            return policy.getContinuesAfterRetirement();
        }
        
        Insurance.InsuranceType type = policy.getType();
        if (type == null) {
            return false;
        }
        
        switch (type) {
            case TERM_LIFE:
                // Term life insurance typically continues until coverage end age (e.g., 65-85)
                // Assume it continues unless maturity date is set and passed
                return true;
                
            case HEALTH:
                // Health insurance: GROUP ends at retirement, PERSONAL/FAMILY_FLOATER continues
                Insurance.HealthInsuranceType healthType = policy.getHealthType();
                if (healthType == Insurance.HealthInsuranceType.GROUP) {
                    return false; // Employer-provided, ends at retirement
                }
                // PERSONAL and FAMILY_FLOATER continue after retirement
                return true;
                
            case ULIP:
            case ENDOWMENT:
            case MONEY_BACK:
                // Investment-linked policies have maturity dates, don't continue
                return false;
                
            case VEHICLE:
            case OTHER:
            default:
                return false;
        }
    }

    /**
     * Calculate investments and insurance policies maturing before retirement.
     * These become available for reinvestment and should be tracked separately.
     */
    public Map<String, Object> calculateMaturingBeforeRetirement(String userId, int currentAge, int retirementAge) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> maturingInvestments = new ArrayList<>();
        List<Map<String, Object>> maturingInsurance = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        int yearsToRetirement = retirementAge - currentAge;
        LocalDate retirementDate = today.plusYears(yearsToRetirement);
        
        double totalMaturingAmount = 0;
        
        // Check investments with maturity dates (FD, RD, PPF)
        List<Investment> investments = investmentRepository.findByUserId(userId);
        for (Investment inv : investments) {
            if (inv.getMaturityDate() != null && 
                inv.getMaturityDate().isAfter(today) && 
                inv.getMaturityDate().isBefore(retirementDate)) {
                
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", inv.getId());
                item.put("name", inv.getName());
                item.put("type", inv.getType() != null ? inv.getType().name() : "OTHER");
                item.put("maturityDate", inv.getMaturityDate());
                item.put("yearsToMaturity", java.time.temporal.ChronoUnit.YEARS.between(today, inv.getMaturityDate()));
                
                // Calculate expected maturity value
                double maturityValue = calculateExpectedMaturityValue(inv);
                item.put("expectedMaturityValue", Math.round(maturityValue));
                item.put("currentValue", inv.getCurrentValue() != null ? Math.round(inv.getCurrentValue()) : 0);
                
                maturingInvestments.add(item);
                totalMaturingAmount += maturityValue;
            }
        }
        
        // Check insurance policies with maturity (ULIP, Endowment, Money Back)
        List<Insurance> insurances = insuranceRepository.findByUserId(userId);
        for (Insurance ins : insurances) {
            Insurance.InsuranceType type = ins.getType();
            boolean hasMaturity = type == Insurance.InsuranceType.ULIP || 
                                  type == Insurance.InsuranceType.ENDOWMENT || 
                                  type == Insurance.InsuranceType.MONEY_BACK;
            
            if (hasMaturity && ins.getMaturityDate() != null && 
                ins.getMaturityDate().isAfter(today) && 
                ins.getMaturityDate().isBefore(retirementDate)) {
                
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", ins.getId());
                item.put("name", ins.getPolicyName());
                item.put("type", type.name());
                item.put("maturityDate", ins.getMaturityDate());
                item.put("yearsToMaturity", java.time.temporal.ChronoUnit.YEARS.between(today, ins.getMaturityDate()));
                
                // Expected maturity value (sum assured + bonuses for traditional, fund value for ULIP)
                double maturityValue = ins.getSumAssured() != null ? ins.getSumAssured() : 0;
                if (type == Insurance.InsuranceType.ULIP && ins.getFundValue() != null) {
                    // For ULIP, project fund value growth
                    long yearsToMaturity = java.time.temporal.ChronoUnit.YEARS.between(today, ins.getMaturityDate());
                    double expectedReturn = 8.0; // Assume 8% for ULIP
                    maturityValue = calculationService.calculateFutureValue(ins.getFundValue(), expectedReturn, (int) yearsToMaturity);
                }
                item.put("expectedMaturityValue", Math.round(maturityValue));
                item.put("currentFundValue", ins.getFundValue() != null ? Math.round(ins.getFundValue()) : 0);
                
                maturingInsurance.add(item);
                totalMaturingAmount += maturityValue;
            }
        }
        
        // Sort by maturity date
        maturingInvestments.sort((a, b) -> ((LocalDate) a.get("maturityDate")).compareTo((LocalDate) b.get("maturityDate")));
        maturingInsurance.sort((a, b) -> ((LocalDate) a.get("maturityDate")).compareTo((LocalDate) b.get("maturityDate")));
        
        result.put("maturingInvestments", maturingInvestments);
        result.put("maturingInsurance", maturingInsurance);
        result.put("totalMaturingBeforeRetirement", Math.round(totalMaturingAmount));
        result.put("investmentCount", maturingInvestments.size());
        result.put("insuranceCount", maturingInsurance.size());
        result.put("retirementDate", retirementDate);
        
        return result;
    }
    
    /**
     * Calculate SIP future value for internal use
     */
    private double calculateSIPFutureValue(double monthlySIP, double annualRate, int years) {
        if (years <= 0 || monthlySIP <= 0) return 0;
        double monthlyRate = annualRate / 100 / 12;
        int months = years * 12;
        return monthlySIP * ((Math.pow(1 + monthlyRate, months) - 1) / monthlyRate) * (1 + monthlyRate);
    }
    
    /**
     * Calculate expected maturity value for an investment based on its type.
     */
    private double calculateExpectedMaturityValue(Investment inv) {
        LocalDate today = LocalDate.now();
        
        if (inv.getMaturityDate() == null) {
            return inv.getCurrentValue() != null ? inv.getCurrentValue() : 0;
        }
        
        long yearsToMaturity = java.time.temporal.ChronoUnit.YEARS.between(today, inv.getMaturityDate());
        if (yearsToMaturity <= 0) {
            return inv.getCurrentValue() != null ? inv.getCurrentValue() : 0;
        }
        
        double currentValue = inv.getCurrentValue() != null ? inv.getCurrentValue() : 
                             (inv.getInvestedAmount() != null ? inv.getInvestedAmount() : 0);
        
        Investment.InvestmentType type = inv.getType();
        
        if (type == Investment.InvestmentType.FD) {
            // FD: compound at interest rate
            double rate = inv.getInterestRate() != null ? inv.getInterestRate() : 7.0;
            return calculationService.calculateFutureValue(currentValue, rate, (int) yearsToMaturity);
            
        } else if (type == Investment.InvestmentType.RD) {
            // RD: current value + future SIP contributions
            double rate = inv.getInterestRate() != null ? inv.getInterestRate() : 6.5;
            double currentGrowth = calculationService.calculateFutureValue(currentValue, rate, (int) yearsToMaturity);
            double sipGrowth = 0;
            if (inv.getMonthlySip() != null && inv.getMonthlySip() > 0) {
                sipGrowth = calculationService.calculateSIPFutureValue(inv.getMonthlySip(), rate, (int) yearsToMaturity);
            }
            return currentGrowth + sipGrowth;
            
        } else if (type == Investment.InvestmentType.PPF) {
            // PPF: current value + yearly contributions
            double rate = inv.getExpectedReturn() != null ? inv.getExpectedReturn() : defaultPpfReturn;
            double currentGrowth = calculationService.calculateFutureValue(currentValue, rate, (int) yearsToMaturity);
            double yearlyGrowth = 0;
            if (inv.getYearlyContribution() != null && inv.getYearlyContribution() > 0) {
                // Convert yearly to monthly for SIP calculation, then multiply
                double monthlyEquivalent = inv.getYearlyContribution() / 12;
                yearlyGrowth = calculationService.calculateSIPFutureValue(monthlyEquivalent, rate, (int) yearsToMaturity);
            }
            return currentGrowth + yearlyGrowth;
        }
        
        // Default: assume current value grows at expected return
        double rate = inv.getExpectedReturn() != null ? inv.getExpectedReturn() : 7.0;
        return calculationService.calculateFutureValue(currentValue, rate, (int) yearsToMaturity);
    }

    /**
     * Generate withdrawal strategy recommendations for retirement.
     * Provides optimal order of fund withdrawal to maximize tax efficiency and corpus longevity.
     */
    public Map<String, Object> generateWithdrawalStrategy(String userId, int currentAge, int retirementAge, int lifeExpectancy) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        int yearsToRetirement = retirementAge - currentAge;
        int retirementYears = lifeExpectancy - retirementAge;
        
        // Get all investments
        List<Investment> investments = investmentRepository.findByUserId(userId);
        
        // Categorize assets by withdrawal phase
        Map<String, Object> phase1 = new LinkedHashMap<>(); // Taxable & Liquid (first)
        Map<String, Object> phase2 = new LinkedHashMap<>(); // Tax-Deferred (second)
        Map<String, Object> phase3 = new LinkedHashMap<>(); // Tax-Free & Long-Term (last)
        
        List<Map<String, Object>> phase1Assets = new ArrayList<>();
        List<Map<String, Object>> phase2Assets = new ArrayList<>();
        List<Map<String, Object>> phase3Assets = new ArrayList<>();
        
        double phase1Total = 0, phase2Total = 0, phase3Total = 0;
        
        for (Investment inv : investments) {
            double futureValue = calculateProjectedValueAtRetirement(inv, yearsToRetirement);
            if (futureValue <= 0) continue;
            
            Map<String, Object> asset = new LinkedHashMap<>();
            asset.put("id", inv.getId());
            asset.put("name", inv.getName());
            asset.put("type", inv.getType() != null ? inv.getType().name() : "OTHER");
            asset.put("currentValue", Math.round(inv.getCurrentValue() != null ? inv.getCurrentValue() : 0));
            asset.put("projectedValueAtRetirement", Math.round(futureValue));
            
            Investment.InvestmentType type = inv.getType();
            
            if (type == null) {
                phase2Assets.add(asset);
                asset.put("reason", "Default category for unspecified type");
                asset.put("taxTreatment", "Varies");
                phase2Total += futureValue;
            } else {
                switch (type) {
                    case CASH:
                        phase1Assets.add(asset);
                        asset.put("reason", "Already taxed, no growth benefit from deferral");
                        asset.put("taxTreatment", "Interest taxed as income");
                        asset.put("withdrawalTip", "Use for immediate expenses in early retirement");
                        phase1Total += futureValue;
                        break;
                        
                    case FD:
                    case RD:
                        phase1Assets.add(asset);
                        asset.put("reason", "Low returns, interest taxed annually");
                        asset.put("taxTreatment", "Interest taxed as per income slab");
                        asset.put("withdrawalTip", "Withdraw as they mature, don't renew");
                        phase1Total += futureValue;
                        break;
                        
                    case STOCK:
                        phase1Assets.add(asset);
                        asset.put("reason", "Taxable gains, harvest losses strategically");
                        asset.put("taxTreatment", "LTCG above ₹1L taxed at 10%");
                        asset.put("withdrawalTip", "Harvest ₹1L LTCG tax-free each year");
                        phase1Total += futureValue;
                        break;
                        
                    case EPF:
                        phase2Assets.add(asset);
                        asset.put("reason", "Tax-free after 5 years, withdraw gradually");
                        asset.put("taxTreatment", "Tax-free if 5+ years of service");
                        asset.put("withdrawalTip", "Consider pension option for regular income");
                        phase2Total += futureValue;
                        break;
                        
                    case NPS:
                        phase2Assets.add(asset);
                        asset.put("reason", "40% mandatory annuity, 60% tax-free");
                        asset.put("taxTreatment", "60% tax-free, annuity income taxable");
                        asset.put("withdrawalTip", "Choose annuity with return of purchase price");
                        asset.put("mandatoryAnnuityPercent", 40);
                        phase2Total += futureValue;
                        break;
                        
                    case MUTUAL_FUND:
                        phase2Assets.add(asset);
                        asset.put("reason", "Tax-efficient if held long-term");
                        asset.put("taxTreatment", "LTCG after 1 year, indexed for debt");
                        asset.put("withdrawalTip", "SWP for tax-efficient regular income");
                        phase2Total += futureValue;
                        break;
                        
                    case PPF:
                        phase3Assets.add(asset);
                        asset.put("reason", "Completely tax-free, continue to grow");
                        asset.put("taxTreatment", "100% tax-free (EEE status)");
                        asset.put("withdrawalTip", "Extend in 5-year blocks, withdraw last");
                        phase3Total += futureValue;
                        break;
                        
                    case GOLD:
                        phase3Assets.add(asset);
                        asset.put("reason", "Inflation hedge, emergency reserve");
                        asset.put("taxTreatment", "SGB maturity tax-free, physical gold LTCG after 3 years");
                        asset.put("withdrawalTip", "Sell only in emergencies or for legacy");
                        phase3Total += futureValue;
                        break;
                        
                    case REAL_ESTATE:
                        phase3Assets.add(asset);
                        asset.put("reason", "Legacy asset, consider rental income");
                        asset.put("taxTreatment", "LTCG after 2 years with indexation");
                        asset.put("withdrawalTip", "Rental income or reverse mortgage option");
                        phase3Total += futureValue;
                        break;
                        
                    case CRYPTO:
                        phase1Assets.add(asset);
                        asset.put("reason", "High volatility, use during favorable markets");
                        asset.put("taxTreatment", "30% flat tax on gains");
                        asset.put("withdrawalTip", "Sell during bull markets, use for discretionary");
                        phase1Total += futureValue;
                        break;
                        
                    default:
                        phase2Assets.add(asset);
                        asset.put("reason", "Standard tax treatment");
                        asset.put("taxTreatment", "Varies by holding period");
                        phase2Total += futureValue;
                        break;
                }
            }
        }
        
        // Build phase summaries
        phase1.put("name", "Taxable & Liquid Assets");
        phase1.put("description", "Use first - already taxed, let other assets grow");
        phase1.put("suggestedAgeRange", retirementAge + " - " + Math.min(retirementAge + 5, lifeExpectancy));
        phase1.put("total", Math.round(phase1Total));
        phase1.put("assets", phase1Assets);
        phase1.put("priority", 1);
        phase1.put("color", "emerald");
        
        phase2.put("name", "Tax-Deferred Accounts");
        phase2.put("description", "Mandatory withdrawals, plan around them");
        phase2.put("suggestedAgeRange", Math.min(retirementAge + 5, lifeExpectancy) + " - " + Math.min(retirementAge + 15, lifeExpectancy));
        phase2.put("total", Math.round(phase2Total));
        phase2.put("assets", phase2Assets);
        phase2.put("priority", 2);
        phase2.put("color", "blue");
        
        phase3.put("name", "Tax-Free & Long-Term");
        phase3.put("description", "Keep growing tax-free, use as backup");
        phase3.put("suggestedAgeRange", Math.min(retirementAge + 15, lifeExpectancy) + " - " + lifeExpectancy + "+");
        phase3.put("total", Math.round(phase3Total));
        phase3.put("assets", phase3Assets);
        phase3.put("priority", 3);
        phase3.put("color", "purple");
        
        // Calculate withdrawal schedule
        double totalCorpus = phase1Total + phase2Total + phase3Total;
        
        // Get monthly expenses from expenses
        List<Expense> expenses = expenseRepository.findByUserId(userId);
        double monthlyExpenses = expenses.stream()
                .filter(e -> e.getAmount() != null)
                .mapToDouble(e -> {
                    double amount = e.getAmount();
                    Expense.ExpenseFrequency freq = e.getFrequency();
                    if (freq == null) freq = Expense.ExpenseFrequency.MONTHLY;
                    switch (freq) {
                        case MONTHLY: return amount;
                        case QUARTERLY: return amount / 3;
                        case HALF_YEARLY: return amount / 6;
                        case YEARLY: return amount / 12;
                        case ONE_TIME: return 0; // One-time expenses not included in monthly
                        default: return amount;
                    }
                })
                .sum();
        
        // Project expenses to retirement
        double inflationRate = 0.06;
        double monthlyExpenseAtRetirement = monthlyExpenses * Math.pow(1 + inflationRate, yearsToRetirement);
        double yearlyExpenseAtRetirement = monthlyExpenseAtRetirement * 12;
        
        // Calculate years each phase can support
        int phase1Years = yearlyExpenseAtRetirement > 0 ? (int) Math.floor(phase1Total / yearlyExpenseAtRetirement) : 0;
        int phase2Years = yearlyExpenseAtRetirement > 0 ? (int) Math.floor(phase2Total / yearlyExpenseAtRetirement) : 0;
        int phase3Years = yearlyExpenseAtRetirement > 0 ? (int) Math.floor(phase3Total / yearlyExpenseAtRetirement) : 0;
        int totalYearsCovered = phase1Years + phase2Years + phase3Years;
        
        phase1.put("yearsCovered", phase1Years);
        phase2.put("yearsCovered", phase2Years);
        phase3.put("yearsCovered", phase3Years);
        
        // Calculate safe withdrawal rate metrics
        double safeWithdrawalRate = 0.04; // 4% rule
        double safeMonthlyWithdrawal = totalCorpus * safeWithdrawalRate / 12;
        double sustainableRate = 0.06; // Assuming 8% return, 6% withdrawal
        double sustainableMonthlyWithdrawal = totalCorpus * sustainableRate / 12;
        
        // Withdrawal schedule by year
        List<Map<String, Object>> withdrawalSchedule = new ArrayList<>();
        double remainingCorpus = totalCorpus;
        double currentPhase1 = phase1Total;
        double currentPhase2 = phase2Total;
        double currentPhase3 = phase3Total;
        
        for (int year = 0; year < retirementYears && year < 30; year++) {
            Map<String, Object> yearData = new LinkedHashMap<>();
            int age = retirementAge + year;
            double yearlyExpense = yearlyExpenseAtRetirement * Math.pow(1 + inflationRate, year);
            
            yearData.put("year", year + 1);
            yearData.put("age", age);
            yearData.put("yearlyExpense", Math.round(yearlyExpense));
            yearData.put("corpusAtStart", Math.round(remainingCorpus));
            
            // Determine which phase to withdraw from
            String withdrawFrom;
            if (currentPhase1 > 0) {
                withdrawFrom = "Phase 1";
                double withdrawal = Math.min(yearlyExpense, currentPhase1);
                currentPhase1 -= withdrawal;
                remainingCorpus -= withdrawal;
                // Other phases continue to grow
                currentPhase2 *= 1.08;
                currentPhase3 *= 1.07;
            } else if (currentPhase2 > 0) {
                withdrawFrom = "Phase 2";
                double withdrawal = Math.min(yearlyExpense, currentPhase2);
                currentPhase2 -= withdrawal;
                remainingCorpus -= withdrawal;
                currentPhase3 *= 1.07;
            } else {
                withdrawFrom = "Phase 3";
                double withdrawal = Math.min(yearlyExpense, currentPhase3);
                currentPhase3 -= withdrawal;
                remainingCorpus -= withdrawal;
            }
            
            yearData.put("withdrawFrom", withdrawFrom);
            yearData.put("corpusAtEnd", Math.round(Math.max(0, currentPhase1 + currentPhase2 + currentPhase3)));
            
            // Check if corpus depleted
            if (remainingCorpus <= 0) {
                yearData.put("warning", "Corpus depleted");
                withdrawalSchedule.add(yearData);
                break;
            }
            
            withdrawalSchedule.add(yearData);
        }
        
        // Tax optimization tips
        List<Map<String, Object>> taxTips = new ArrayList<>();
        
        Map<String, Object> tip1 = new LinkedHashMap<>();
        tip1.put("title", "Stay under ₹7L taxable income");
        tip1.put("description", "Senior citizens (60+) get higher basic exemption. Plan withdrawals to stay under limit.");
        tip1.put("savingsEstimate", "Up to ₹31,200/year in tax savings");
        taxTips.add(tip1);
        
        Map<String, Object> tip2 = new LinkedHashMap<>();
        tip2.put("title", "Harvest LTCG up to ₹1L/year");
        tip2.put("description", "Sell and rebuy equity investments to reset cost basis and use tax-free LTCG limit.");
        tip2.put("savingsEstimate", "₹10,000/year in tax savings");
        taxTips.add(tip2);
        
        Map<String, Object> tip3 = new LinkedHashMap<>();
        tip3.put("title", "Use SCSS & PMVVY");
        tip3.put("description", "Senior Citizen Savings Scheme and Pradhan Mantri Vaya Vandana Yojana offer 8%+ returns.");
        tip3.put("savingsEstimate", "1-2% higher returns than FD");
        taxTips.add(tip3);
        
        Map<String, Object> tip4 = new LinkedHashMap<>();
        tip4.put("title", "Health insurance premium deduction");
        tip4.put("description", "Deduction up to ₹50,000 for senior citizens under Section 80D.");
        tip4.put("savingsEstimate", "Up to ₹15,600/year in tax savings");
        taxTips.add(tip4);
        
        // Build result
        result.put("phases", List.of(phase1, phase2, phase3));
        result.put("summary", Map.of(
                "totalCorpusAtRetirement", Math.round(totalCorpus),
                "monthlyExpenseAtRetirement", Math.round(monthlyExpenseAtRetirement),
                "yearlyExpenseAtRetirement", Math.round(yearlyExpenseAtRetirement),
                "totalYearsCovered", totalYearsCovered,
                "retirementYears", retirementYears,
                "safeMonthlyWithdrawal", Math.round(safeMonthlyWithdrawal),
                "sustainableMonthlyWithdrawal", Math.round(sustainableMonthlyWithdrawal),
                "isSustainable", safeMonthlyWithdrawal >= monthlyExpenseAtRetirement,
                "shortfallOrSurplus", Math.round(safeMonthlyWithdrawal - monthlyExpenseAtRetirement)
        ));
        result.put("withdrawalSchedule", withdrawalSchedule);
        result.put("taxOptimizationTips", taxTips);
        
        // Important notes
        List<String> importantNotes = List.of(
                "Keep 1-2 years expenses in liquid form for emergencies",
                "Increase withdrawal by 5-6% annually to maintain lifestyle against inflation",
                "Avoid selling equity in down markets - use other assets temporarily",
                "Review and rebalance portfolio annually",
                "Consult a tax advisor as tax laws change frequently"
        );
        result.put("importantNotes", importantNotes);
        
        return result;
    }
    
    /**
     * Calculate projected value of investment at retirement.
     */
    private double calculateProjectedValueAtRetirement(Investment inv, int yearsToRetirement) {
        double currentValue = inv.getCurrentValue() != null ? inv.getCurrentValue() : 0;
        if (currentValue <= 0 && inv.getInvestedAmount() != null) {
            currentValue = inv.getInvestedAmount();
        }
        if (currentValue <= 0) return 0;
        
        double expectedReturn = inv.getExpectedReturn() != null ? inv.getExpectedReturn() : getDefaultReturnForType(inv.getType());
        double monthlySip = inv.getMonthlySip() != null ? inv.getMonthlySip() : 0;
        
        // Lump sum growth
        double futureValue = calculationService.calculateFutureValue(currentValue, expectedReturn, yearsToRetirement);
        
        // SIP growth
        if (monthlySip > 0) {
            futureValue += calculationService.calculateSIPFutureValue(monthlySip, expectedReturn, yearsToRetirement);
        }
        
        return futureValue;
    }
    
    /**
     * Get default expected return for investment type.
     */
    private double getDefaultReturnForType(Investment.InvestmentType type) {
        if (type == null) return 7.0;
        switch (type) {
            case STOCK:
                return 12.0;
            case MUTUAL_FUND:
                return 10.0;
            case FD:
                return 6.5;
            case RD:
                return 6.0;
            case PPF:
                return defaultPpfReturn;
            case EPF:
                return defaultEpfReturn;
            case NPS:
                return 10.0;
            case GOLD:
                return 8.0;
            case REAL_ESTATE:
                return 7.0;
            case CASH:
                return 3.5;
            case CRYPTO:
                return 15.0; // High volatility, high potential return
            default:
                return 7.0;
        }
    }
}
