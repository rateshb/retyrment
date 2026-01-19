// Delete the bad record and create a new one
db.userFeatureAccess.deleteOne({_id: ObjectId("696dc8e53cdd33eb3000efe2")});
print("Deleted bad record");

// Create correct record
db.userFeatureAccess.insertOne({
    userId: "696cb54540c4f92bda4cd711",
    incomePage: true,
    investmentPage: true,
    loanPage: true,
    insurancePage: true,
    expensePage: true,
    goalsPage: true,
    familyPage: true,
    calendarPage: true,
    retirementPage: true,
    insuranceRecommendationsPage: true,
    reportsPage: true,
    simulationPage: true,
    adminPanel: true,
    preferencesPage: true,
    settingsPage: true,
    accountPage: true,
    allowedInvestmentTypes: ["MUTUAL_FUND", "PPF", "EPF", "FD", "RD", "REAL_ESTATE", "STOCK", "NPS", "GOLD", "CRYPTO", "CASH"],
    blockedInsuranceTypes: [],
    retirementStrategyPlannerTab: true,
    canExportPdf: true,
    canExportExcel: true,
    canExportJson: true,
    canImportData: true,
    createdAt: new Date(),
    updatedAt: new Date()
});
print("Created correct record");

// Verify
var access = db.userFeatureAccess.findOne({userId: "696cb54540c4f92bda4cd711"});
print("userId: " + access.userId);
print("adminPanel: " + access.adminPanel);
