// Comprehensive test data for rateshb@gmail.com (Age 35, Retirement Age 65)
var userId = "6968b02362916b53a02b53fc";
var now = new Date();

// Clear existing data for this user first
db.investments.deleteMany({userId: userId});
db.expenses.deleteMany({userId: userId});
db.loans.deleteMany({userId: userId});
db.goals.deleteMany({userId: userId});
db.insurance.deleteMany({userId: userId});
db.income.deleteMany({userId: userId});
db.settings.deleteMany({userId: userId});

print("Cleared existing data for user");

// ============ INVESTMENTS ============
// Enum: MUTUAL_FUND, STOCK, FD, RD, PPF, EPF, NPS, REAL_ESTATE, GOLD, CRYPTO, CASH, OTHER
db.investments.insertMany([
    {
        userId: userId,
        type: "EPF",
        name: "Employee Provident Fund",
        investedAmount: 1200000,
        currentValue: 1500000,
        monthlySip: 15000,
        sipDay: 1,
        purchaseDate: new Date("2015-01-01"),
        evaluationDate: now,
        expectedReturn: 8.15,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Investment"
    },
    {
        userId: userId,
        type: "PPF",
        name: "Public Provident Fund",
        investedAmount: 500000,
        currentValue: 650000,
        monthlySip: 12500,
        sipDay: 5,
        purchaseDate: new Date("2018-04-01"),
        evaluationDate: now,
        expectedReturn: 7.1,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Investment"
    },
    {
        userId: userId,
        type: "MUTUAL_FUND",
        name: "HDFC Flexi Cap Fund",
        investedAmount: 800000,
        currentValue: 1100000,
        monthlySip: 20000,
        sipDay: 10,
        purchaseDate: new Date("2019-01-01"),
        evaluationDate: now,
        expectedReturn: 12,
        sipStepUpPercent: 10,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Investment"
    },
    {
        userId: userId,
        type: "MUTUAL_FUND",
        name: "Axis Bluechip Fund",
        investedAmount: 400000,
        currentValue: 520000,
        monthlySip: 10000,
        sipDay: 15,
        purchaseDate: new Date("2020-06-01"),
        evaluationDate: now,
        expectedReturn: 11,
        sipStepUpPercent: 10,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Investment"
    },
    {
        userId: userId,
        type: "MUTUAL_FUND",
        name: "Parag Parikh Flexi Cap",
        investedAmount: 300000,
        currentValue: 380000,
        monthlySip: 15000,
        sipDay: 5,
        purchaseDate: new Date("2021-01-01"),
        evaluationDate: now,
        expectedReturn: 12,
        sipStepUpPercent: 10,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Investment"
    },
    {
        userId: userId,
        type: "STOCK",
        name: "Direct Equity Portfolio",
        investedAmount: 500000,
        currentValue: 700000,
        monthlySip: 0,
        purchaseDate: new Date("2018-01-01"),
        evaluationDate: now,
        expectedReturn: 14,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Investment"
    },
    {
        userId: userId,
        type: "NPS",
        name: "National Pension Scheme",
        investedAmount: 300000,
        currentValue: 400000,
        monthlySip: 5000,
        sipDay: 1,
        purchaseDate: new Date("2020-01-01"),
        evaluationDate: now,
        expectedReturn: 10,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Investment"
    },
    {
        userId: userId,
        type: "GOLD",
        name: "Sovereign Gold Bonds",
        investedAmount: 200000,
        currentValue: 280000,
        monthlySip: 0,
        purchaseDate: new Date("2019-06-01"),
        evaluationDate: now,
        expectedReturn: 8,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Investment"
    },
    {
        userId: userId,
        type: "FD",
        name: "HDFC Bank FD",
        investedAmount: 500000,
        currentValue: 550000,
        monthlySip: 0,
        purchaseDate: new Date("2024-01-01"),
        evaluationDate: now,
        expectedReturn: 7,
        maturityDate: new Date("2027-01-01"),
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Investment"
    },
    {
        userId: userId,
        type: "REAL_ESTATE",
        name: "Rental Property - Bangalore",
        investedAmount: 5000000,
        currentValue: 7500000,
        monthlySip: 0,
        purchaseDate: new Date("2018-01-01"),
        evaluationDate: now,
        expectedReturn: 8,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Investment"
    }
]);

print("Inserted 10 investments");

// ============ INCOME ============
// Fields: source, monthlyAmount, annualIncrement, startDate, isActive
db.income.insertMany([
    {
        userId: userId,
        source: "Primary Salary - IT Company",
        monthlyAmount: 200000,
        annualIncrement: 8,
        startDate: new Date("2020-01-01"),
        isActive: true,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Income"
    },
    {
        userId: userId,
        source: "Rental Income - Bangalore Property",
        monthlyAmount: 25000,
        annualIncrement: 5,
        startDate: new Date("2019-01-01"),
        isActive: true,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Income"
    },
    {
        userId: userId,
        source: "Stock Dividends (Yearly/12)",
        monthlyAmount: 4167,
        annualIncrement: 10,
        startDate: new Date("2020-01-01"),
        isActive: true,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Income"
    },
    {
        userId: userId,
        source: "Annual Bonus (Yearly/12)",
        monthlyAmount: 33333,
        annualIncrement: 5,
        startDate: new Date("2020-01-01"),
        isActive: true,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Income"
    }
]);

print("Inserted 4 income sources");

// ============ EXPENSES ============
// Categories: RENT, UTILITIES, GROCERIES, TRANSPORT, ENTERTAINMENT, HEALTHCARE, SHOPPING, 
//             DINING, TRAVEL, SUBSCRIPTIONS, SCHOOL_FEE, COLLEGE_FEE, TUITION, COACHING, 
//             BOOKS_SUPPLIES, HOSTEL, CHILDCARE, DAYCARE, ELDERLY_CARE, MAINTENANCE, 
//             SOCIETY_CHARGES, INSURANCE_PREMIUM, OTHER
// Frequency: MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY, ONE_TIME
db.expenses.insertMany([
    {
        userId: userId,
        name: "House Rent",
        category: "RENT",
        amount: 40000,
        frequency: "MONTHLY",
        isFixed: true,
        isTimeBound: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Expense"
    },
    {
        userId: userId,
        name: "Groceries & Food",
        category: "GROCERIES",
        amount: 15000,
        frequency: "MONTHLY",
        isFixed: false,
        isTimeBound: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Expense"
    },
    {
        userId: userId,
        name: "Utilities (Electricity, Water, Gas)",
        category: "UTILITIES",
        amount: 5000,
        frequency: "MONTHLY",
        isFixed: true,
        isTimeBound: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Expense"
    },
    {
        userId: userId,
        name: "Transportation & Fuel",
        category: "TRANSPORT",
        amount: 8000,
        frequency: "MONTHLY",
        isFixed: false,
        isTimeBound: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Expense"
    },
    {
        userId: userId,
        name: "Child 1 School Fee",
        category: "SCHOOL_FEE",
        amount: 15000,
        frequency: "MONTHLY",
        isFixed: true,
        isTimeBound: true,
        startDate: new Date("2024-04-01"),
        endDate: new Date("2036-03-31"),
        dependentName: "Child 1",
        dependentCurrentAge: 8,
        endAge: 18,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Expense"
    },
    {
        userId: userId,
        name: "Child 2 School Fee",
        category: "SCHOOL_FEE",
        amount: 12000,
        frequency: "MONTHLY",
        isFixed: true,
        isTimeBound: true,
        startDate: new Date("2026-04-01"),
        endDate: new Date("2040-03-31"),
        dependentName: "Child 2",
        dependentCurrentAge: 4,
        endAge: 18,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Expense"
    },
    {
        userId: userId,
        name: "Health Insurance Premium",
        category: "INSURANCE_PREMIUM",
        amount: 60000,
        frequency: "YEARLY",
        isFixed: true,
        isTimeBound: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Expense"
    },
    {
        userId: userId,
        name: "Entertainment & Leisure",
        category: "ENTERTAINMENT",
        amount: 10000,
        frequency: "MONTHLY",
        isFixed: false,
        isTimeBound: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Expense"
    },
    {
        userId: userId,
        name: "Medical Expenses",
        category: "HEALTHCARE",
        amount: 5000,
        frequency: "MONTHLY",
        isFixed: false,
        isTimeBound: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Expense"
    },
    {
        userId: userId,
        name: "Parents Support",
        category: "ELDERLY_CARE",
        amount: 20000,
        frequency: "MONTHLY",
        isFixed: true,
        isTimeBound: true,
        startDate: new Date("2020-01-01"),
        endDate: new Date("2040-12-31"),
        dependentName: "Parents",
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Expense"
    },
    {
        userId: userId,
        name: "Annual Vacation",
        category: "TRAVEL",
        amount: 150000,
        frequency: "YEARLY",
        isFixed: false,
        isTimeBound: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Expense"
    },
    {
        userId: userId,
        name: "Shopping & Clothing",
        category: "SHOPPING",
        amount: 8000,
        frequency: "MONTHLY",
        isFixed: false,
        isTimeBound: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Expense"
    }
]);

print("Inserted 12 expenses");

// ============ LOANS ============
// Type: HOME, VEHICLE, PERSONAL, EDUCATION, CREDIT_CARD, OTHER
// Fields: originalAmount, outstandingAmount, emi, interestRate, tenureMonths, remainingMonths
db.loans.insertMany([
    {
        userId: userId,
        name: "Home Loan - Bangalore Property",
        type: "HOME",
        originalAmount: 4000000,
        outstandingAmount: 2500000,
        interestRate: 8.5,
        emi: 45000,
        tenureMonths: 240,
        remainingMonths: 72,
        startDate: new Date("2018-01-01"),
        endDate: new Date("2032-01-01"),
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Loan"
    },
    {
        userId: userId,
        name: "Car Loan - Honda City",
        type: "VEHICLE",
        originalAmount: 800000,
        outstandingAmount: 300000,
        interestRate: 9,
        emi: 18000,
        tenureMonths: 60,
        remainingMonths: 18,
        startDate: new Date("2022-06-01"),
        endDate: new Date("2027-06-01"),
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Loan"
    }
]);

print("Inserted 2 loans");

// ============ GOALS ============
db.goals.insertMany([
    {
        userId: userId,
        name: "Child 1 Higher Education",
        targetAmount: 3000000,
        currentAmount: 500000,
        targetYear: 2038,
        priority: "HIGH",
        category: "EDUCATION",
        isRecurring: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Goal"
    },
    {
        userId: userId,
        name: "Child 2 Higher Education",
        targetAmount: 3500000,
        currentAmount: 200000,
        targetYear: 2042,
        priority: "HIGH",
        category: "EDUCATION",
        isRecurring: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Goal"
    },
    {
        userId: userId,
        name: "Child 1 Wedding",
        targetAmount: 2500000,
        currentAmount: 100000,
        targetYear: 2045,
        priority: "MEDIUM",
        category: "WEDDING",
        isRecurring: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Goal"
    },
    {
        userId: userId,
        name: "Child 2 Wedding",
        targetAmount: 3000000,
        currentAmount: 50000,
        targetYear: 2050,
        priority: "MEDIUM",
        category: "WEDDING",
        isRecurring: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Goal"
    },
    {
        userId: userId,
        name: "Dream Home Upgrade",
        targetAmount: 10000000,
        currentAmount: 1000000,
        targetYear: 2035,
        priority: "LOW",
        category: "REAL_ESTATE",
        isRecurring: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Goal"
    },
    {
        userId: userId,
        name: "Retirement Corpus",
        targetAmount: 50000000,
        currentAmount: 5000000,
        targetYear: 2056,
        priority: "HIGH",
        category: "RETIREMENT",
        isRecurring: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Goal"
    },
    {
        userId: userId,
        name: "Emergency Fund",
        targetAmount: 1200000,
        currentAmount: 800000,
        targetYear: 2027,
        priority: "HIGH",
        category: "EMERGENCY",
        isRecurring: false,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Goal"
    }
]);

print("Inserted 7 goals");

// ============ INSURANCE ============
// Type: TERM_LIFE, HEALTH, ULIP, ENDOWMENT, MONEY_BACK, ANNUITY, VEHICLE, OTHER
// PremiumFrequency: MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY, SINGLE
// Fields: policyName, company, annualPremium, premiumAmount
db.insurance.insertMany([
    {
        userId: userId,
        policyName: "HDFC Life Term Plan",
        type: "TERM_LIFE",
        sumAssured: 20000000,
        annualPremium: 25000,
        premiumAmount: 25000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2020-01-01"),
        maturityDate: new Date("2055-01-01"),
        company: "HDFC Life",
        policyNumber: "HDFC123456",
        policyTerm: 35,
        coverageEndAge: 70,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Insurance"
    },
    {
        userId: userId,
        policyName: "ICICI Pru Term Plan",
        type: "TERM_LIFE",
        sumAssured: 10000000,
        annualPremium: 15000,
        premiumAmount: 15000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2021-01-01"),
        maturityDate: new Date("2056-01-01"),
        company: "ICICI Prudential",
        policyNumber: "ICICI789012",
        policyTerm: 35,
        coverageEndAge: 71,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Insurance"
    },
    {
        userId: userId,
        policyName: "Star Health Insurance",
        type: "HEALTH",
        healthType: "FAMILY_FLOATER",
        sumAssured: 1000000,
        annualPremium: 35000,
        premiumAmount: 35000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2019-01-01"),
        company: "Star Health",
        policyNumber: "STAR345678",
        continuesAfterRetirement: true,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Insurance"
    },
    {
        userId: userId,
        policyName: "Super Top-Up Health",
        type: "HEALTH",
        healthType: "FAMILY_FLOATER",
        sumAssured: 2500000,
        annualPremium: 12000,
        premiumAmount: 12000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2022-01-01"),
        company: "HDFC Ergo",
        policyNumber: "HDFC901234",
        continuesAfterRetirement: true,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Insurance"
    },
    {
        userId: userId,
        policyName: "LIC Jeevan Anand",
        type: "ENDOWMENT",
        sumAssured: 500000,
        annualPremium: 40000,
        premiumAmount: 40000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2015-01-01"),
        maturityDate: new Date("2035-01-01"),
        company: "LIC",
        policyNumber: "LIC567890",
        policyTerm: 20,
        maturityBenefit: 1000000,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Insurance"
    },
    {
        userId: userId,
        policyName: "Car Insurance - Honda City",
        type: "VEHICLE",
        sumAssured: 800000,
        annualPremium: 15000,
        premiumAmount: 15000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2024-01-01"),
        maturityDate: new Date("2025-01-01"),
        company: "ICICI Lombard",
        policyNumber: "ICICI234567",
        policyTerm: 1,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Insurance"
    },
    {
        userId: userId,
        policyName: "Personal Accident Cover",
        type: "OTHER",
        sumAssured: 5000000,
        annualPremium: 5000,
        premiumAmount: 5000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2023-01-01"),
        company: "Bajaj Allianz",
        policyNumber: "BAJAJ678901",
        continuesAfterRetirement: true,
        createdAt: now,
        updatedAt: now,
        _class: "com.retyrment.model.Insurance"
    }
]);

print("Inserted 7 insurance policies");

// ============ SETTINGS ============
db.settings.insertOne({
    userId: userId,
    currentAge: 35,
    retirementAge: 65,
    lifeExpectancy: 85,
    inflationRate: 6,
    preRetirementReturn: 12,
    postRetirementReturn: 8,
    createdAt: now,
    updatedAt: now,
    _class: "com.retyrment.model.Settings"
});

print("Inserted settings");

// Final summary
print("\n========== DATA SEEDING COMPLETE ==========");
print("User: rateshb@gmail.com (ID: " + userId + ")");
print("Current Age: 35, Retirement Age: 65");
print("");
print("Data Summary:");
print("  Investments: " + db.investments.count({userId: userId}));
print("  Income: " + db.income.count({userId: userId}));
print("  Expenses: " + db.expenses.count({userId: userId}));
print("  Loans: " + db.loans.count({userId: userId}));
print("  Goals: " + db.goals.count({userId: userId}));
print("  Insurance: " + db.insurance.count({userId: userId}));
print("  Settings: " + db.settings.count({userId: userId}));
print("============================================");
