package com.retyrment.service;

import com.retyrment.model.Investment;
import com.retyrment.model.Insurance;
import com.retyrment.model.User;
import com.retyrment.model.UserFeatureAccess;
import com.retyrment.repository.UserFeatureAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.HashSet;

/**
 * Service to manage and check user feature access.
 * Provides default access based on user role and allows per-user overrides.
 */
@Service
@RequiredArgsConstructor
public class FeatureAccessService {

    private final UserFeatureAccessRepository featureAccessRepository;

    /**
     * Get feature access for a user, creating defaults if not exists.
     */
    public UserFeatureAccess getUserFeatureAccess(User user) {
        return featureAccessRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultFeatureAccess(user));
    }

    /**
     * Create default feature access based on user role.
     */
    private UserFeatureAccess createDefaultFeatureAccess(User user) {
        UserFeatureAccess.UserFeatureAccessBuilder builder = UserFeatureAccess.builder()
                .userId(user.getId())
                // Pages - defaults from requirements
                .incomePage(true)  // All users
                .investmentPage(true)  // Visible by default
                .loanPage(true)  // All users
                .insurancePage(true)  // Visible by default
                .expensePage(true)  // All users
                .goalsPage(true)  // Visible by default
                .familyPage(true)  // Family page - visible by default
                .calendarPage(false)  // Restricted by default
                .retirementPage(true)  // Visible by default
                .insuranceRecommendationsPage(true)  // Insurance Advisor - visible by default
                .reportsPage(false)  // Restricted by default
                .simulationPage(true)  // Restricted by default
                .adminPanel(user.isAdmin())  // Admin only
                .preferencesPage(false)  // Restricted by default
                .settingsPage(true)  // All users
                .accountPage(true)  // All users
                // Investment types - default allowed
                .allowedInvestmentTypes(new HashSet<>(Arrays.asList(
                        "MUTUAL_FUND", "PPF", "EPF", "FD", "RD", "REAL_ESTATE", 
                    "STOCK", "NPS", "GOLD", "CRYPTO", "CASH"
                )))
                // Insurance types - default blocked
                // Note: Frontend uses categories (PENSION, LIFE_SAVINGS) but backend uses types
                // PENSION category -> ANNUITY type
                // LIFE_SAVINGS category -> ENDOWMENT, MONEY_BACK, ULIP types
                .blockedInsuranceTypes(new HashSet<>(Arrays.asList()))
                // Retirement tabs
                .retirementStrategyPlannerTab(false)  // Restricted by default
                .retirementWithdrawalStrategyTab(false)  // Restricted by default
                // Reports
                .canExportPdf(false)  // Restricted by default
                .canExportExcel(false)  // Restricted by default
                .canExportJson(false)  // Restricted by default
                .canImportData(false);  // Restricted by default

        // PRO users get some additional access by default
        if (user.isPro()) {
            builder.reportsPage(true)
                    .canExportPdf(false)
                    .canExportExcel(false)
                    .canExportJson(false)
                    .canImportData(false)
                    .simulationPage(true)
                    .canRunSimulation(false)
                    .retirementStrategyPlannerTab(false)
                    .retirementWithdrawalStrategyTab(false)
                    .calendarPage(false)
                    .preferencesPage(false);
        }

        // ADMIN specific overrides - Admins have access to everything by default
        if (user.isAdmin()) {
            builder.reportsPage(true)
                    .canExportPdf(true)
                    .canExportExcel(true)
                    .canExportJson(true)
                    .canImportData(true)
                    .simulationPage(true)  // Admin should have simulation access
                    .canRunSimulation(true)  // Admin can run simulations
                    .retirementStrategyPlannerTab(true)
                    .retirementWithdrawalStrategyTab(true)
                    .calendarPage(true)
                    .preferencesPage(true);
            // Admins have access to all investment types by default
            builder.allowedInvestmentTypes(new HashSet<>(Arrays.asList(
                    "MUTUAL_FUND", "PPF", "EPF", "FD", "RD", "REAL_ESTATE", 
                    "STOCK", "NPS", "GOLD", "CRYPTO", "CASH"
            )));
            // Admins have no blocked insurance types
            builder.blockedInsuranceTypes(new HashSet<>());
        }

        UserFeatureAccess access = builder
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return featureAccessRepository.save(access);
    }

    /**
     * Check if user can access a specific page.
     */
    public boolean canAccessPage(User user, String pageName) {
        UserFeatureAccess access = getUserFeatureAccess(user);
        
        return switch (pageName.toLowerCase()) {
            case "dashboard", "index" -> true;  // Dashboard always accessible for all users
            case "income" -> access.getIncomePage();
            case "investment" -> access.getInvestmentPage();
            case "loan" -> access.getLoanPage();
            case "insurance" -> access.getInsurancePage();
            case "expense" -> access.getExpensePage();
            case "goals" -> access.getGoalsPage();
            case "calendar" -> access.getCalendarPage();
            case "retirement" -> access.getRetirementPage();
            case "reports" -> access.getReportsPage();
            case "simulation" -> access.getSimulationPage();
            case "admin" -> access.getAdminPanel() && user.isAdmin();
            case "preferences" -> access.getPreferencesPage();
            case "settings" -> access.getSettingsPage();
            case "account" -> access.getAccountPage();
            default -> false;
        };
    }

    /**
     * Check if user can access a specific investment type.
     */
    public boolean canAccessInvestmentType(User user, Investment.InvestmentType type) {
        UserFeatureAccess access = getUserFeatureAccess(user);
        return access.getAllowedInvestmentTypes().contains(type.name());
    }

    /**
     * Check if user can access a specific insurance type.
     */
    public boolean canAccessInsuranceType(User user, Insurance.InsuranceType type) {
        UserFeatureAccess access = getUserFeatureAccess(user);
        return !access.getBlockedInsuranceTypes().contains(type.name());
    }

    /**
     * Check if user can access retirement strategy planner tab.
     */
    public boolean canAccessRetirementStrategyPlanner(User user) {
        UserFeatureAccess access = getUserFeatureAccess(user);
        return access.getRetirementStrategyPlannerTab();
    }

    /**
     * Check if user can export PDF.
     */
    public boolean canExportPdf(User user) {
        UserFeatureAccess access = getUserFeatureAccess(user);
        return access.getCanExportPdf();
    }

    /**
     * Check if user can export Excel.
     */
    public boolean canExportExcel(User user) {
        UserFeatureAccess access = getUserFeatureAccess(user);
        return access.getCanExportExcel();
    }

    /**
     * Check if user can export JSON.
     */
    public boolean canExportJson(User user) {
        UserFeatureAccess access = getUserFeatureAccess(user);
        return access.getCanExportJson();
    }

    /**
     * Check if user can import data.
     */
    public boolean canImportData(User user) {
        UserFeatureAccess access = getUserFeatureAccess(user);
        return access.getCanImportData();
    }

    /**
     * Get all feature access as a map for frontend.
     */
    public Map<String, Object> getFeatureAccessMap(User user) {
        UserFeatureAccess access = getUserFeatureAccess(user);
        Map<String, Object> features = new LinkedHashMap<>();

        // Page access
        features.put("incomePage", access.getIncomePage());
        features.put("investmentPage", access.getInvestmentPage());
        features.put("loanPage", access.getLoanPage());
        features.put("insurancePage", access.getInsurancePage());
        features.put("expensePage", access.getExpensePage());
        features.put("goalsPage", access.getGoalsPage());
        features.put("familyPage", access.getFamilyPage());
        features.put("calendarPage", access.getCalendarPage());
        features.put("retirementPage", access.getRetirementPage());
        features.put("insuranceRecommendationsPage", access.getInsuranceRecommendationsPage());
        features.put("reportsPage", access.getReportsPage());
        features.put("simulationPage", access.getSimulationPage());
        features.put("canRunSimulation", access.getCanRunSimulation());
        features.put("adminPanel", access.getAdminPanel() && user.isAdmin());
        features.put("preferencesPage", access.getPreferencesPage());
        features.put("settingsPage", access.getSettingsPage());
        features.put("accountPage", access.getAccountPage());

        // Investment types
        features.put("allowedInvestmentTypes", new ArrayList<>(access.getAllowedInvestmentTypes()));
        features.put("blockedInsuranceTypes", new ArrayList<>(access.getBlockedInsuranceTypes()));

        // Retirement tabs
        features.put("retirementStrategyPlannerTab", access.getRetirementStrategyPlannerTab());
        features.put("retirementWithdrawalStrategyTab", access.getRetirementWithdrawalStrategyTab());

        // Reports
        features.put("canExportPdf", access.getCanExportPdf());
        features.put("canExportExcel", access.getCanExportExcel());
        features.put("canExportJson", access.getCanExportJson());
        features.put("canImportData", access.getCanImportData());

        return features;
    }

    /**
     * Update feature access for a user (admin only).
     * Note: adminPanel is NOT updated here - it's determined by user role.
     */
    public UserFeatureAccess updateFeatureAccess(String userId, UserFeatureAccess updatedAccess) {
        UserFeatureAccess existing = featureAccessRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserFeatureAccess newAccess = new UserFeatureAccess();
                    newAccess.setUserId(userId);
                    // Set adminPanel to true by default for new records - will be filtered by role in getFeatureAccessMap
                    newAccess.setAdminPanel(true);
                    return newAccess;
                });

        // Update all fields EXCEPT adminPanel (which is role-based, not editable)
        if (updatedAccess.getIncomePage() != null) existing.setIncomePage(updatedAccess.getIncomePage());
        if (updatedAccess.getInvestmentPage() != null) existing.setInvestmentPage(updatedAccess.getInvestmentPage());
        if (updatedAccess.getLoanPage() != null) existing.setLoanPage(updatedAccess.getLoanPage());
        if (updatedAccess.getInsurancePage() != null) existing.setInsurancePage(updatedAccess.getInsurancePage());
        if (updatedAccess.getExpensePage() != null) existing.setExpensePage(updatedAccess.getExpensePage());
        if (updatedAccess.getGoalsPage() != null) existing.setGoalsPage(updatedAccess.getGoalsPage());
        if (updatedAccess.getCalendarPage() != null) existing.setCalendarPage(updatedAccess.getCalendarPage());
        if (updatedAccess.getRetirementPage() != null) existing.setRetirementPage(updatedAccess.getRetirementPage());
        if (updatedAccess.getReportsPage() != null) existing.setReportsPage(updatedAccess.getReportsPage());
        if (updatedAccess.getSimulationPage() != null) existing.setSimulationPage(updatedAccess.getSimulationPage());
        if (updatedAccess.getCanRunSimulation() != null) existing.setCanRunSimulation(updatedAccess.getCanRunSimulation());
        // NOTE: adminPanel is intentionally NOT updated here - it's controlled by user.isAdmin()
        // The frontend filters adminPanel visibility based on role in getFeatureAccessMap()
        if (updatedAccess.getPreferencesPage() != null) existing.setPreferencesPage(updatedAccess.getPreferencesPage());
        if (updatedAccess.getSettingsPage() != null) existing.setSettingsPage(updatedAccess.getSettingsPage());
        if (updatedAccess.getAccountPage() != null) existing.setAccountPage(updatedAccess.getAccountPage());

        if (updatedAccess.getAllowedInvestmentTypes() != null) {
            // Defensive copy to prevent EI_EXPOSE_REP
            existing.setAllowedInvestmentTypes(new HashSet<>(updatedAccess.getAllowedInvestmentTypes()));
        }
        if (updatedAccess.getBlockedInsuranceTypes() != null) {
            // Defensive copy to prevent EI_EXPOSE_REP
            existing.setBlockedInsuranceTypes(new HashSet<>(updatedAccess.getBlockedInsuranceTypes()));
        }

        if (updatedAccess.getRetirementStrategyPlannerTab() != null) {
            existing.setRetirementStrategyPlannerTab(updatedAccess.getRetirementStrategyPlannerTab());
        }
        if (updatedAccess.getRetirementWithdrawalStrategyTab() != null) {
            existing.setRetirementWithdrawalStrategyTab(updatedAccess.getRetirementWithdrawalStrategyTab());
        }

        if (updatedAccess.getCanExportPdf() != null) existing.setCanExportPdf(updatedAccess.getCanExportPdf());
        if (updatedAccess.getCanExportExcel() != null) existing.setCanExportExcel(updatedAccess.getCanExportExcel());
        if (updatedAccess.getCanExportJson() != null) existing.setCanExportJson(updatedAccess.getCanExportJson());
        if (updatedAccess.getCanImportData() != null) existing.setCanImportData(updatedAccess.getCanImportData());

        existing.setUpdatedAt(LocalDateTime.now());
        return featureAccessRepository.save(existing);
    }
}
