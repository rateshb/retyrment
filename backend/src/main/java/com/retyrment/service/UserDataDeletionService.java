package com.retyrment.service;

import com.retyrment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataDeletionService {

    private final IncomeRepository incomeRepository;
    private final InvestmentRepository investmentRepository;
    private final LoanRepository loanRepository;
    private final InsuranceRepository insuranceRepository;
    private final ExpenseRepository expenseRepository;
    private final GoalRepository goalRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final SettingsRepository settingsRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserStrategyRepository userStrategyRepository;
    private final RetirementScenarioRepository retirementScenarioRepository;
    private final CalendarEntryRepository calendarEntryRepository;

    /**
     * Get a summary of all user data (count of records in each category)
     */
    public Map<String, Object> getUserDataSummary(String userId) {
        log.info("Getting data summary for user: {}", userId);
        
        Map<String, Object> summary = new HashMap<>();
        
        try {
            // Count records in each category with null safety
            var incomeList = incomeRepository.findByUserId(userId);
            summary.put("income", incomeList != null ? incomeList.size() : 0);
            
            var investmentList = investmentRepository.findByUserId(userId);
            summary.put("investments", investmentList != null ? investmentList.size() : 0);
            
            var loanList = loanRepository.findByUserId(userId);
            summary.put("loans", loanList != null ? loanList.size() : 0);
            
            var insuranceList = insuranceRepository.findByUserId(userId);
            summary.put("insurance", insuranceList != null ? insuranceList.size() : 0);
            
            var expenseList = expenseRepository.findByUserId(userId);
            summary.put("expenses", expenseList != null ? expenseList.size() : 0);
            
            var goalList = goalRepository.findByUserId(userId);
            summary.put("goals", goalList != null ? goalList.size() : 0);
            
            var familyList = familyMemberRepository.findByUserId(userId);
            summary.put("familyMembers", familyList != null ? familyList.size() : 0);
            
            summary.put("preferences", userPreferenceRepository.findByUserId(userId).isPresent() ? 1 : 0);
            summary.put("settings", settingsRepository.findByUserId(userId).isPresent() ? 1 : 0);
            summary.put("strategies", userStrategyRepository.findByUserId(userId).isPresent() ? 1 : 0);
            
            var scenarioList = retirementScenarioRepository.findByUserId(userId);
            summary.put("scenarios", scenarioList != null ? scenarioList.size() : 0);
            
            var calendarList = calendarEntryRepository.findByUserId(userId);
            summary.put("calendarEntries", calendarList != null ? calendarList.size() : 0);
            
            // Calculate total
            int total = summary.values().stream()
                    .mapToInt(v -> v instanceof Integer ? (Integer) v : 0)
                    .sum();
            summary.put("total", total);
            
            log.info("Data summary for user {}: {} total records", userId, total);
            
        } catch (Exception e) {
            log.error("Error calculating data summary for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
        
        return summary;
    }

    /**
     * Delete ALL user financial data
     * Keeps: User account, email, role, subscription status, feature access
     * Deletes: All financial data (income, investments, loans, insurance, expenses, goals, family, preferences, settings)
     */
    @Transactional
    public Map<String, Object> deleteAllUserData(String userId) {
        log.warn("DELETING ALL DATA for user: {}", userId);
        
        Map<String, Object> deletionSummary = new HashMap<>();
        
        try {
            // Delete Income
            int incomeDeleted = incomeRepository.findByUserId(userId).size();
            incomeRepository.deleteByUserId(userId);
            deletionSummary.put("income", incomeDeleted);
            log.info("Deleted {} income records for user {}", incomeDeleted, userId);
            
            // Delete Investments
            int investmentsDeleted = investmentRepository.findByUserId(userId).size();
            investmentRepository.deleteByUserId(userId);
            deletionSummary.put("investments", investmentsDeleted);
            log.info("Deleted {} investment records for user {}", investmentsDeleted, userId);
            
            // Delete Loans
            int loansDeleted = loanRepository.findByUserId(userId).size();
            loanRepository.deleteByUserId(userId);
            deletionSummary.put("loans", loansDeleted);
            log.info("Deleted {} loan records for user {}", loansDeleted, userId);
            
            // Delete Insurance
            int insuranceDeleted = insuranceRepository.findByUserId(userId).size();
            insuranceRepository.deleteByUserId(userId);
            deletionSummary.put("insurance", insuranceDeleted);
            log.info("Deleted {} insurance records for user {}", insuranceDeleted, userId);
            
            // Delete Expenses
            int expensesDeleted = expenseRepository.findByUserId(userId).size();
            expenseRepository.deleteByUserId(userId);
            deletionSummary.put("expenses", expensesDeleted);
            log.info("Deleted {} expense records for user {}", expensesDeleted, userId);
            
            // Delete Goals
            int goalsDeleted = goalRepository.findByUserId(userId).size();
            goalRepository.deleteByUserId(userId);
            deletionSummary.put("goals", goalsDeleted);
            log.info("Deleted {} goal records for user {}", goalsDeleted, userId);
            
            // Delete Family Members
            int familyDeleted = familyMemberRepository.findByUserId(userId).size();
            familyMemberRepository.findByUserId(userId).forEach(member -> 
                familyMemberRepository.deleteById(member.getId())
            );
            deletionSummary.put("familyMembers", familyDeleted);
            log.info("Deleted {} family member records for user {}", familyDeleted, userId);
            
            // Delete User Preferences
            userPreferenceRepository.findByUserId(userId).ifPresent(pref -> {
                userPreferenceRepository.delete(pref);
                deletionSummary.put("preferences", 1);
                log.info("Deleted user preferences for user {}", userId);
            });
            if (!deletionSummary.containsKey("preferences")) {
                deletionSummary.put("preferences", 0);
            }
            
            // Delete Settings
            settingsRepository.findByUserId(userId).ifPresent(settings -> {
                settingsRepository.delete(settings);
                deletionSummary.put("settings", 1);
                log.info("Deleted settings for user {}", userId);
            });
            if (!deletionSummary.containsKey("settings")) {
                deletionSummary.put("settings", 0);
            }
            
            // Delete User Settings (new user settings model)
            userSettingsRepository.findByUserId(userId).ifPresent(userSettings -> {
                userSettingsRepository.delete(userSettings);
                log.info("Deleted user settings for user {}", userId);
            });
            
            // Delete User Strategy
            userStrategyRepository.findByUserId(userId).ifPresent(strategy -> {
                userStrategyRepository.delete(strategy);
                deletionSummary.put("strategies", 1);
                log.info("Deleted strategy for user {}", userId);
            });
            if (!deletionSummary.containsKey("strategies")) {
                deletionSummary.put("strategies", 0);
            }
            
            // Delete Retirement Scenarios
            int scenariosDeleted = retirementScenarioRepository.findByUserId(userId).size();
            retirementScenarioRepository.deleteByUserId(userId);
            deletionSummary.put("scenarios", scenariosDeleted);
            log.info("Deleted {} retirement scenario records for user {}", scenariosDeleted, userId);
            
            // Delete Calendar Entries (derived data)
            int calendarDeleted = calendarEntryRepository.findByUserId(userId).size();
            calendarEntryRepository.deleteByUserId(userId);
            deletionSummary.put("calendarEntries", calendarDeleted);
            log.info("Deleted {} calendar entry records for user {}", calendarDeleted, userId);
            
            // Calculate total deleted
            int totalDeleted = deletionSummary.values().stream()
                    .mapToInt(v -> (Integer) v)
                    .sum();
            deletionSummary.put("total", totalDeleted);
            deletionSummary.put("success", true);
            deletionSummary.put("message", "All user data deleted successfully. Your account remains active.");
            
            log.warn("Successfully deleted {} total records for user {}", totalDeleted, userId);
            
        } catch (Exception e) {
            log.error("Error deleting user data for user {}: {}", userId, e.getMessage(), e);
            deletionSummary.put("success", false);
            deletionSummary.put("error", "Failed to delete user data: " + e.getMessage());
        }
        
        return deletionSummary;
    }
}
