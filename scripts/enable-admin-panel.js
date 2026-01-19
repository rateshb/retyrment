// Script to enable Admin Panel for a specific user
var user = db.users.findOne({email: 'bansalitadvisory@gmail.com'});
if (user) {
    print('Found user: ' + user.email + ' (ID: ' + user._id.toString() + ')');
    
    // Check if feature access exists
    var existingAccess = db.userFeatureAccess.findOne({userId: user._id.toString()});
    
    if (existingAccess) {
        // Update existing record
        var result = db.userFeatureAccess.updateOne(
            {userId: user._id.toString()}, 
            {$set: {adminPanel: true, updatedAt: new Date()}}
        );
        print('Updated existing record. Matched: ' + result.matchedCount + ', Modified: ' + result.modifiedCount);
    } else {
        // Create new record with adminPanel enabled
        var newAccess = {
            userId: user._id.toString(),
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
            allowedInvestmentTypes: ['MUTUAL_FUND', 'PPF', 'EPF', 'FD', 'RD', 'REAL_ESTATE', 'STOCK', 'NPS', 'GOLD', 'CRYPTO', 'CASH'],
            blockedInsuranceTypes: [],
            retirementStrategyPlannerTab: true,
            canExportPdf: true,
            canExportExcel: true,
            canExportJson: true,
            canImportData: true,
            createdAt: new Date(),
            updatedAt: new Date()
        };
        db.userFeatureAccess.insertOne(newAccess);
        print('Created new feature access record with adminPanel enabled');
    }
    
    // Verify the update
    var access = db.userFeatureAccess.findOne({userId: user._id.toString()});
    if (access) {
        print('Current adminPanel value: ' + access.adminPanel);
    }
} else {
    print('User not found');
}
