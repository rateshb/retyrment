// Fix user data - associate all data with rateshb@gmail.com user
var userId = "6968b02362916b53a02b53fc";

// First, delete any data that was incorrectly seeded with userId field
db.investments.deleteMany({userId: {$exists: true}});
db.expenses.deleteMany({userId: {$exists: true}});
db.loans.deleteMany({userId: {$exists: true}});
db.goals.deleteMany({userId: {$exists: true}});
db.insurance.deleteMany({userId: {$exists: true}});
db.income.deleteMany({userId: {$exists: true}});

print("Deleted incorrectly seeded data");

// Now update all existing data (without userId) to belong to this user
var result1 = db.investments.updateMany({userId: {$exists: false}}, {$set: {userId: userId}});
print("Updated investments: " + result1.modifiedCount);

var result2 = db.expenses.updateMany({userId: {$exists: false}}, {$set: {userId: userId}});
print("Updated expenses: " + result2.modifiedCount);

var result3 = db.loans.updateMany({userId: {$exists: false}}, {$set: {userId: userId}});
print("Updated loans: " + result3.modifiedCount);

var result4 = db.goals.updateMany({userId: {$exists: false}}, {$set: {userId: userId}});
print("Updated goals: " + result4.modifiedCount);

var result5 = db.insurance.updateMany({userId: {$exists: false}}, {$set: {userId: userId}});
print("Updated insurance: " + result5.modifiedCount);

var result6 = db.income.updateMany({userId: {$exists: false}}, {$set: {userId: userId}});
print("Updated income: " + result6.modifiedCount);

// Verify counts
print("\nFinal counts for user " + userId + ":");
print("Investments: " + db.investments.count({userId: userId}));
print("Expenses: " + db.expenses.count({userId: userId}));
print("Loans: " + db.loans.count({userId: userId}));
print("Goals: " + db.goals.count({userId: userId}));
print("Insurance: " + db.insurance.count({userId: userId}));
print("Income: " + db.income.count({userId: userId}));
