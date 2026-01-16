// Update feature access for users

// Enable Strategy Planner for rateshb@gmail.com
db.user_feature_access.updateOne(
    {userId: '6968b02362916b53a02b53fc'}, 
    {$set: {retirementStrategyPlannerTab: true}}
);
print('Enabled Strategy Planner for rateshb@gmail.com');

// Enable Admin Panel for bansalitadvisory@gmail.com (admin user)
db.user_feature_access.updateOne(
    {userId: '6967e1a0e8b4b628ca3ca25b'}, 
    {$set: {adminPanel: true}}
);
print('Enabled Admin Panel for bansalitadvisory@gmail.com');

// Verify changes
print('\nUpdated feature access:');
db.user_feature_access.find({}, {userId:1, retirementStrategyPlannerTab:1, adminPanel:1}).forEach(function(doc) {
    printjson(doc);
});
