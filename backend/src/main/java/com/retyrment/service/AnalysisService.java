package com.retyrment.service;

import com.retyrment.model.*;
import com.retyrment.model.Investment.InvestmentType;
import com.retyrment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final IncomeRepository incomeRepository;
    private final InvestmentRepository investmentRepository;
    private final LoanRepository loanRepository;
    private final InsuranceRepository insuranceRepository;
    private final ExpenseRepository expenseRepository;
    private final GoalRepository goalRepository;
    private final CalculationService calculationService;

    @Value("${app.defaults.inflation-rate}")
    private double defaultInflation;

    @Value("${app.defaults.mf-equity-return}")
    private double defaultMFReturn;

    public Map<String, Object> calculateNetWorth(String userId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Calculate total assets - filter by userId
        double totalInvestments = investmentRepository.findByUserId(userId).stream()
                .mapToDouble(i -> i.getCurrentValue() != null ? i.getCurrentValue() : 
                                  (i.getInvestedAmount() != null ? i.getInvestedAmount() : 0))
                .sum();

        // Add insurance fund values (ULIP, Endowment) - filter by userId
        double insuranceFundValue = insuranceRepository.findByUserId(userId).stream()
                .filter(i -> i.getFundValue() != null)
                .mapToDouble(Insurance::getFundValue)
                .sum();

        double totalAssets = totalInvestments + insuranceFundValue;

        // Calculate total liabilities - filter by userId
        double totalLiabilities = loanRepository.findByUserId(userId).stream()
                .mapToDouble(l -> l.getOutstandingAmount() != null ? l.getOutstandingAmount() : 0)
                .sum();

        double netWorth = totalAssets - totalLiabilities;

        result.put("totalAssets", Math.round(totalAssets));
        result.put("totalInvestments", Math.round(totalInvestments));
        result.put("insuranceFundValue", Math.round(insuranceFundValue));
        result.put("totalLiabilities", Math.round(totalLiabilities));
        result.put("netWorth", Math.round(netWorth));

        // Breakdown by investment type - filter by userId
        Map<String, Double> assetBreakdown = new LinkedHashMap<>();
        investmentRepository.findByUserId(userId).forEach(inv -> {
            String type = inv.getType() != null ? inv.getType().name() : "OTHER";
            double value = inv.getCurrentValue() != null ? inv.getCurrentValue() : 
                          (inv.getInvestedAmount() != null ? inv.getInvestedAmount() : 0);
            assetBreakdown.merge(type, value, Double::sum);
        });
        result.put("assetBreakdown", assetBreakdown);
        
     // Breakdown by investment type - filter by userId
        Map<String, Double> sellableAssets = new LinkedHashMap<>();
        investmentRepository.findByUserIdAndType(userId, InvestmentType.REAL_ESTATE)
        .stream()
        .filter(inv -> inv.getMonthlyRentalIncome() == null 
                || (
                		!"SELF_OCCUPIED".equals(inv.getRealEstateType())
                		&& !"RENTAL".equals(inv.getRealEstateType())
        		
        		))
        .forEach(inv -> {
        	String type = inv.getType() != null ? inv.getType().name() : "OTHER";
            double value = inv.getCurrentValue() != null ? inv.getCurrentValue() : 
                          (inv.getInvestedAmount() != null ? inv.getInvestedAmount() : 0);
            sellableAssets.merge(type, value, Double::sum);
        });
        result.put("sellableAssets", sellableAssets);

        return result;
    }

    public Map<String, Object> calculateProjections(String userId, int years) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> yearlyProjection = new ArrayList<>();

        List<Investment> investments = investmentRepository.findByUserId(userId);
        int currentYear = LocalDate.now().getYear();

        for (int year = 0; year <= years; year++) {
            Map<String, Object> yearData = new LinkedHashMap<>();
            yearData.put("year", currentYear + year);
            
            double totalValue = 0;

            // Project each investment
            for (Investment inv : investments) {
                double currentValue = inv.getCurrentValue() != null ? inv.getCurrentValue() : 
                                     (inv.getInvestedAmount() != null ? inv.getInvestedAmount() : 0);
                double returnRate = inv.getExpectedReturn() != null ? inv.getExpectedReturn() : defaultMFReturn;
                
                // Lumpsum growth
                double projected = calculationService.calculateFutureValue(currentValue, returnRate, year);
                
                // SIP growth
                if (inv.getMonthlySip() != null && inv.getMonthlySip() > 0) {
                    projected += calculationService.calculateSIPFutureValue(inv.getMonthlySip(), returnRate, year);
                }
                
                totalValue += projected;
            }

            yearData.put("projectedValue", Math.round(totalValue));
            yearlyProjection.add(yearData);
        }

        result.put("projections", yearlyProjection);
        result.put("finalValue", yearlyProjection.isEmpty() ? 0 : 
                   yearlyProjection.get(yearlyProjection.size() - 1).get("projectedValue"));

        return result;
    }

    public Map<String, Object> analyzeGoals(String userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Goal> goals = goalRepository.findByUserIdOrderByTargetYearAsc(userId);
        
        int currentYear = LocalDate.now().getYear();

        // First pass: calculate total goals value for proportional allocation
        double totalGoalsValue = goals.stream()
                .mapToDouble(g -> g.getTargetAmount() != null ? g.getTargetAmount() : 0)
                .sum();

        List<Map<String, Object>> goalAnalysis = new ArrayList<>();
        double totalInflatedValue = 0;

        for (Goal goal : goals) {
            Map<String, Object> analysis = new LinkedHashMap<>();
            int yearsToGoal = goal.getTargetYear() - currentYear;
            
            double targetAmount = goal.getTargetAmount() != null ? goal.getTargetAmount() : 0;
            double inflatedAmount = calculationService.calculateInflatedValue(targetAmount, defaultInflation, yearsToGoal);
            
            // Calculate projected corpus at goal year
            Map<String, Object> projection = calculateProjections(userId, yearsToGoal);
            double projectedCorpus = ((Number) projection.get("finalValue")).doubleValue();
            
            // Simple proportional allocation based on total goals
            double allocation = totalGoalsValue > 0 ? 
                    (targetAmount / totalGoalsValue) * projectedCorpus : projectedCorpus;
            
            double fundingPercent = inflatedAmount > 0 ? 
                    Math.min(100, (allocation / inflatedAmount) * 100) : 0;
            
            String status = fundingPercent >= 100 ? "FUNDED" : 
                           fundingPercent >= 50 ? "PARTIAL" : "UNFUNDED";

            analysis.put("id", goal.getId());
            analysis.put("name", goal.getName());
            analysis.put("icon", goal.getIcon());
            analysis.put("targetYear", goal.getTargetYear());
            analysis.put("yearsAway", yearsToGoal);
            analysis.put("targetAmount", Math.round(targetAmount));
            analysis.put("inflatedAmount", Math.round(inflatedAmount));
            analysis.put("fundingPercent", Math.round(fundingPercent));
            analysis.put("status", status);
            analysis.put("gap", Math.round(Math.max(0, inflatedAmount - allocation)));

            goalAnalysis.add(analysis);
            totalInflatedValue += inflatedAmount;
        }

        result.put("goals", goalAnalysis);
        result.put("totalGoalsValue", Math.round(totalGoalsValue));
        result.put("totalInflatedValue", Math.round(totalInflatedValue));

        return result;
    }

    public Map<String, Object> generateRecommendations(String userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> recommendations = new ArrayList<>();

        // Get current data - filter by userId
        List<Investment> investments = investmentRepository.findByUserId(userId);
        List<Insurance> insurances = insuranceRepository.findByUserId(userId);
        List<Expense> expenses = expenseRepository.findByUserId(userId);
        List<Income> incomes = incomeRepository.findByUserIdAndIsActiveTrue(userId);

        double totalMonthlyIncome = incomes.stream()
                .mapToDouble(i -> i.getMonthlyAmount() != null ? i.getMonthlyAmount() : 0)
                .sum();

        double totalMonthlyExpenses = expenses.stream()
                .mapToDouble(e -> e.getMonthlyAmount() != null ? e.getMonthlyAmount() : 0)
                .sum();

        double cashBalance = investments.stream()
                .filter(i -> i.getType() == Investment.InvestmentType.CASH)
                .mapToDouble(i -> i.getCurrentValue() != null ? i.getCurrentValue() : 0)
                .sum();

        double totalMonthlySIP = investments.stream()
                .filter(i -> i.getMonthlySip() != null)
                .mapToDouble(Investment::getMonthlySip)
                .sum();

        // Check emergency fund (6 months expenses)
        double requiredEmergencyFund = totalMonthlyExpenses * 6;
        if (cashBalance < requiredEmergencyFund) {
            recommendations.add(createRecommendation("danger", "🆘", "Build Emergency Fund",
                    String.format("Maintain at least 6 months of expenses (₹%.0f) in liquid savings. Current: ₹%.0f",
                            requiredEmergencyFund, cashBalance)));
        }

        // Check health insurance
        boolean hasHealthInsurance = insurances.stream()
                .anyMatch(i -> i.getType() == Insurance.InsuranceType.HEALTH);
        if (!hasHealthInsurance) {
            recommendations.add(createRecommendation("danger", "🏥", "Get Health Insurance",
                    "Medical emergencies can deplete savings quickly. Get a family floater with adequate coverage."));
        }

        // Check term insurance
        boolean hasTermInsurance = insurances.stream()
                .anyMatch(i -> i.getType() == Insurance.InsuranceType.TERM_LIFE);
        if (!hasTermInsurance && totalMonthlyIncome > 0) {
            recommendations.add(createRecommendation("warning", "🛡️", "Consider Term Insurance",
                    "Term life insurance provides high coverage at low cost. Aim for 10-15x annual income."));
        }

        // Check savings rate
        double savingsRate = totalMonthlyIncome > 0 ? 
                ((totalMonthlyIncome - totalMonthlyExpenses) / totalMonthlyIncome) * 100 : 0;
        if (savingsRate < 20 && totalMonthlyIncome > 0) {
            recommendations.add(createRecommendation("warning", "💰", "Increase Savings Rate",
                    String.format("Your savings rate is %.0f%%. Aim for at least 20-30%% of income.", savingsRate)));
        }

        // Check SIP investments
        if (totalMonthlySIP == 0) {
            recommendations.add(createRecommendation("tip", "📈", "Start SIP Investments",
                    "Begin systematic investments in mutual funds. Even ₹5,000/month grows significantly over time."));
        }

        if (recommendations.isEmpty()) {
            recommendations.add(createRecommendation("success", "✅", "Well Planned!",
                    "Your financial foundation looks solid. Keep monitoring and adjusting as life changes."));
        }

        result.put("recommendations", recommendations);
        result.put("savingsRate", Math.round(savingsRate));
        result.put("monthlyIncome", Math.round(totalMonthlyIncome));
        result.put("monthlyExpenses", Math.round(totalMonthlyExpenses));
        result.put("monthlySavings", Math.round(totalMonthlyIncome - totalMonthlyExpenses));

        return result;
    }

    private Map<String, Object> createRecommendation(String type, String icon, String title, String description) {
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("type", type);
        rec.put("icon", icon);
        rec.put("title", title);
        rec.put("description", description);
        return rec;
    }

    public Map<String, Object> runMonteCarloSimulation(String userId, int simulations, int years) {
        Map<String, Object> result = new LinkedHashMap<>();
        java.security.SecureRandom random = new java.security.SecureRandom();

        List<Investment> investments = investmentRepository.findByUserId(userId);
        double currentValue = investments.stream()
                .mapToDouble(i -> i.getCurrentValue() != null ? i.getCurrentValue() : 0)
                .sum();
        double monthlySIP = investments.stream()
                .filter(i -> i.getMonthlySip() != null)
                .mapToDouble(Investment::getMonthlySip)
                .sum();

        double meanReturn = defaultMFReturn;
        double stdDev = 8.0; // Typical equity volatility

        List<Double> finalValues = new ArrayList<>();

        for (int sim = 0; sim < simulations; sim++) {
            double value = currentValue;
            for (int year = 0; year < years; year++) {
                // Random return with normal distribution
                double yearReturn = meanReturn + random.nextGaussian() * stdDev;
                value = value * (1 + yearReturn / 100);
                value += monthlySIP * 12 * (1 + yearReturn / 200); // Simplified SIP addition
            }
            finalValues.add(value);
        }

        Collections.sort(finalValues);

        // Create percentiles object for frontend compatibility
        Map<String, Object> percentiles = new LinkedHashMap<>();
        percentiles.put("p10", Math.round(finalValues.get((int)(simulations * 0.10))));
        percentiles.put("p25", Math.round(finalValues.get((int)(simulations * 0.25))));
        percentiles.put("p50", Math.round(finalValues.get((int)(simulations * 0.50))));
        percentiles.put("p75", Math.round(finalValues.get((int)(simulations * 0.75))));
        percentiles.put("p90", Math.round(finalValues.get((int)(simulations * 0.90))));

        // Calculate success rate (percentage of simulations that beat current value * 2)
        double targetValue = currentValue * 2; // Target: double the current value
        long successfulSimulations = finalValues.stream()
                .mapToLong(v -> v >= targetValue ? 1 : 0)
                .sum();
        double successRate = (successfulSimulations * 100.0) / simulations;

        result.put("simulations", simulations);
        result.put("years", years);
        result.put("percentiles", percentiles);  // ✅ Nested structure
        result.put("average", Math.round(finalValues.stream().mapToDouble(d -> d).average().orElse(0)));
        result.put("successRate", Math.round(successRate * 10) / 10.0);  // ✅ Add success rate

        return result;
    }

    public Map<String, Object> getFullSummary(String userId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("netWorth", calculateNetWorth(userId));
        summary.put("projections", calculateProjections(userId, 10));
        summary.put("goals", analyzeGoals(userId));
        summary.put("recommendations", generateRecommendations(userId));
        return summary;
    }
}
