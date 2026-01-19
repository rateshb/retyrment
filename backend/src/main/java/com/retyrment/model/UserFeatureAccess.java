package com.retyrment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Per-user feature access control.
 * Controls which pages, tabs, and features are visible to each user.
 */
@Document(collection = "user_feature_access")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFeatureAccess {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    // Page visibility flags
    @Builder.Default
    private Boolean incomePage = true;  // Always visible

    @Builder.Default
    private Boolean investmentPage = true;  // Visible by default

    @Builder.Default
    private Boolean loanPage = true;  // Always visible

    @Builder.Default
    private Boolean insurancePage = true;  // Visible by default

    @Builder.Default
    private Boolean expensePage = true;  // Always visible

    @Builder.Default
    private Boolean goalsPage = true;  // Visible by default

    @Builder.Default
    private Boolean familyPage = true;  // Family members page - visible by default

    @Builder.Default
    private Boolean calendarPage = false;  // Restricted by default

    @Builder.Default
    private Boolean retirementPage = true;  // Visible by default

    @Builder.Default
    private Boolean insuranceRecommendationsPage = true;  // Insurance Advisor page - visible by default

    @Builder.Default
    private Boolean reportsPage = false;  // Restricted by default

    @Builder.Default
    private Boolean simulationPage = false;  // Restricted by default - controls page visibility

    @Builder.Default
    private Boolean canRunSimulation = false;  // Restricted by default - controls ability to run simulations

    @Builder.Default
    private Boolean adminPanel = false;  // Admin only

    @Builder.Default
    private Boolean preferencesPage = false;  // Restricted by default

    @Builder.Default
    private Boolean settingsPage = true;  // Always visible

    @Builder.Default
    private Boolean accountPage = true;  // Always visible

    // Investment type restrictions (allowed types)
    private Set<String> allowedInvestmentTypes;

    // Insurance type restrictions (blocked types)
    private Set<String> blockedInsuranceTypes;

    // Defensive getters/setters to prevent EI_EXPOSE_REP
    public Set<String> getAllowedInvestmentTypes() {
        return allowedInvestmentTypes == null ? null : Collections.unmodifiableSet(new HashSet<>(allowedInvestmentTypes));
    }

    public void setAllowedInvestmentTypes(Set<String> allowedInvestmentTypes) {
        this.allowedInvestmentTypes = allowedInvestmentTypes == null ? null : new HashSet<>(allowedInvestmentTypes);
    }

    public Set<String> getBlockedInsuranceTypes() {
        return blockedInsuranceTypes == null ? null : Collections.unmodifiableSet(new HashSet<>(blockedInsuranceTypes));
    }

    public void setBlockedInsuranceTypes(Set<String> blockedInsuranceTypes) {
        this.blockedInsuranceTypes = blockedInsuranceTypes == null ? null : new HashSet<>(blockedInsuranceTypes);
    }

    // Retirement page tab restrictions
    @Builder.Default
    private Boolean retirementStrategyPlannerTab = false;  // Restricted by default

    @Builder.Default
    private Boolean retirementWithdrawalStrategyTab = false;  // Restricted by default

    // Report type restrictions
    @Builder.Default
    private Boolean canExportPdf = false;  // Restricted by default

    @Builder.Default
    private Boolean canExportExcel = false;  // Restricted by default

    @Builder.Default
    private Boolean canExportJson = false;  // Restricted by default

    @Builder.Default
    private Boolean canImportData = false;  // Restricted by default

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
