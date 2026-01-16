// MongoDB Test Data Seed Script
// Run with: mongosh retyrment_local scripts/seed-test-data.js
// Or: mongosh "mongodb://localhost:27017/retyrment_local" scripts/seed-test-data.js

const userId = "rateshb@gmail.com";
const currentYear = new Date().getFullYear();

print("============================================");
print("  Retyrment Test Data Seeder");
print("  User: " + userId);
print("============================================\n");

// Clear existing data for this user
print("Clearing existing data for user...");
db.investments.deleteMany({ userId: userId });
db.expenses.deleteMany({ userId: userId });
db.loans.deleteMany({ userId: userId });
db.goals.deleteMany({ userId: userId });
db.insurance.deleteMany({ userId: userId });
db.incomes.deleteMany({ userId: userId });
print("Done.\n");

// ============================================
// 1. INVESTMENTS
// ============================================
print("1. Adding Investments...");

const investments = [
    // PPF Account
    {
        userId: userId,
        type: "PPF",
        name: "SBI PPF Account",
        description: "Public Provident Fund - 15 year lock-in",
        investedAmount: 1500000,
        currentValue: 1800000,
        expectedReturn: 7.1,
        startDate: new Date("2015-04-01"),
        maturityDate: new Date("2030-04-01"),
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // EPF
    {
        userId: userId,
        type: "EPF",
        name: "EPFO Account",
        description: "Employee Provident Fund",
        investedAmount: 2000000,
        currentValue: 2500000,
        expectedReturn: 8.15,
        monthlySip: 15000,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Mutual Funds - Equity
    {
        userId: userId,
        type: "MUTUAL_FUND",
        name: "Axis Bluechip Fund",
        description: "Large Cap Equity Fund",
        investedAmount: 500000,
        currentValue: 750000,
        expectedReturn: 12,
        monthlySip: 10000,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        type: "MUTUAL_FUND",
        name: "Mirae Asset Emerging Bluechip",
        description: "Large & Mid Cap Fund",
        investedAmount: 300000,
        currentValue: 480000,
        expectedReturn: 14,
        monthlySip: 5000,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        type: "MUTUAL_FUND",
        name: "Parag Parikh Flexi Cap",
        description: "Flexi Cap Fund",
        investedAmount: 200000,
        currentValue: 280000,
        expectedReturn: 13,
        monthlySip: 5000,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // FD
    {
        userId: userId,
        type: "FD",
        name: "HDFC Bank FD",
        description: "Fixed Deposit - 3 year",
        investedAmount: 500000,
        currentValue: 550000,
        expectedReturn: 7,
        startDate: new Date("2023-01-15"),
        maturityDate: new Date("2026-01-15"),
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // RD
    {
        userId: userId,
        type: "RD",
        name: "ICICI RD",
        description: "Recurring Deposit",
        investedAmount: 120000,
        currentValue: 135000,
        expectedReturn: 6.5,
        monthlySip: 10000,
        maturityDate: new Date("2025-12-01"),
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Stocks
    {
        userId: userId,
        type: "STOCK",
        name: "Direct Equity Portfolio",
        description: "Reliance, TCS, HDFC Bank, Infosys",
        investedAmount: 400000,
        currentValue: 520000,
        expectedReturn: 15,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // NPS
    {
        userId: userId,
        type: "NPS",
        name: "NPS Tier 1",
        description: "National Pension System",
        investedAmount: 300000,
        currentValue: 380000,
        expectedReturn: 10,
        monthlySip: 5000,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Gold
    {
        userId: userId,
        type: "GOLD",
        name: "Sovereign Gold Bonds",
        description: "SGB 2019 Series",
        investedAmount: 200000,
        currentValue: 320000,
        expectedReturn: 8,
        maturityDate: new Date("2027-03-15"),
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Real Estate
    {
        userId: userId,
        type: "REAL_ESTATE",
        name: "Plot in Pune",
        description: "Residential plot - Hinjewadi",
        investedAmount: 2500000,
        currentValue: 4000000,
        expectedReturn: 8,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Cash
    {
        userId: userId,
        type: "CASH",
        name: "Emergency Fund",
        description: "Savings Account + Liquid Fund",
        investedAmount: 300000,
        currentValue: 300000,
        expectedReturn: 4,
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

db.investments.insertMany(investments);
print("  Added " + investments.length + " investments");

// ============================================
// 2. EXPENSES
// ============================================
print("2. Adding Expenses...");

const expenses = [
    // Regular Monthly Expenses
    {
        userId: userId,
        category: "RENT",
        name: "House Rent",
        amount: 25000,
        frequency: "MONTHLY",
        isFixed: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        category: "UTILITIES",
        name: "Electricity + Gas + Water",
        amount: 4000,
        frequency: "MONTHLY",
        isFixed: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        category: "GROCERIES",
        name: "Monthly Groceries",
        amount: 12000,
        frequency: "MONTHLY",
        isFixed: false,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        category: "TRANSPORT",
        name: "Fuel + Maintenance",
        amount: 8000,
        frequency: "MONTHLY",
        isFixed: false,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        category: "HEALTHCARE",
        name: "Medical Expenses",
        amount: 3000,
        frequency: "MONTHLY",
        isFixed: false,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        category: "ENTERTAINMENT",
        name: "OTT + Outings",
        amount: 5000,
        frequency: "MONTHLY",
        isFixed: false,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        category: "SUBSCRIPTIONS",
        name: "Internet + Mobile + Subscriptions",
        amount: 2500,
        frequency: "MONTHLY",
        isFixed: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Time-bound Education Expenses - Child 1
    {
        userId: userId,
        category: "SCHOOL_FEE",
        name: "Child 1 - School Fee",
        amount: 45000,
        frequency: "QUARTERLY",
        isFixed: true,
        isTimeBound: true,
        dependentName: "Aarav",
        dependentDob: new Date("2016-05-15"),
        endAge: 18,
        annualIncreasePercent: 8,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Time-bound Education Expenses - Child 2
    {
        userId: userId,
        category: "SCHOOL_FEE",
        name: "Child 2 - School Fee",
        amount: 35000,
        frequency: "QUARTERLY",
        isFixed: true,
        isTimeBound: true,
        dependentName: "Ananya",
        dependentDob: new Date("2019-08-20"),
        endAge: 18,
        annualIncreasePercent: 8,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        category: "TUITION",
        name: "Tuition Classes - Aarav",
        amount: 5000,
        frequency: "MONTHLY",
        isFixed: true,
        isTimeBound: true,
        dependentName: "Aarav",
        dependentDob: new Date("2016-05-15"),
        endAge: 18,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Yearly expenses
    {
        userId: userId,
        category: "TRAVEL",
        name: "Annual Vacation",
        amount: 100000,
        frequency: "YEARLY",
        isFixed: false,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        category: "SHOPPING",
        name: "Festival Shopping",
        amount: 50000,
        frequency: "YEARLY",
        isFixed: false,
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

db.expenses.insertMany(expenses);
print("  Added " + expenses.length + " expenses");

// ============================================
// 3. LOANS
// ============================================
print("3. Adding Loans...");

const loans = [
    {
        userId: userId,
        type: "HOME",
        name: "HDFC Home Loan",
        description: "Home loan for flat in Mumbai",
        originalAmount: 5000000,
        outstandingAmount: 3500000,
        interestRate: 8.5,
        emi: 45000,
        startDate: new Date("2020-06-01"),
        endDate: new Date("2035-06-01"),
        tenure: 180,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        type: "VEHICLE",
        name: "ICICI Car Loan",
        description: "Car loan for Honda City",
        originalAmount: 800000,
        outstandingAmount: 350000,
        interestRate: 9.0,
        emi: 17000,
        startDate: new Date("2022-03-15"),
        endDate: new Date("2027-03-15"),
        tenure: 60,
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

db.loans.insertMany(loans);
print("  Added " + loans.length + " loans");

// ============================================
// 4. GOALS
// ============================================
print("4. Adding Goals...");

const goals = [
    {
        userId: userId,
        name: "Child 1 Higher Education",
        description: "Engineering/MBA for Aarav",
        targetAmount: 2500000,
        targetYear: currentYear + 12,
        currentSavings: 200000,
        priority: "HIGH",
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        name: "Child 2 Higher Education",
        description: "Education fund for Ananya",
        targetAmount: 3000000,
        targetYear: currentYear + 15,
        currentSavings: 100000,
        priority: "HIGH",
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        name: "Child 1 Marriage",
        description: "Marriage expenses for Aarav",
        targetAmount: 2000000,
        targetYear: currentYear + 20,
        currentSavings: 0,
        priority: "MEDIUM",
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        name: "Dream Home",
        description: "Upgrade to bigger house",
        targetAmount: 10000000,
        targetYear: currentYear + 10,
        currentSavings: 500000,
        priority: "MEDIUM",
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        name: "World Tour",
        description: "International vacation with family",
        targetAmount: 800000,
        targetYear: currentYear + 5,
        currentSavings: 150000,
        priority: "LOW",
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        name: "New Car",
        description: "Upgrade to SUV",
        targetAmount: 1500000,
        targetYear: currentYear + 3,
        currentSavings: 300000,
        priority: "LOW",
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

db.goals.insertMany(goals);
print("  Added " + goals.length + " goals");

// ============================================
// 5. INSURANCE
// ============================================
print("5. Adding Insurance Policies...");

const insurances = [
    // Term Life
    {
        userId: userId,
        type: "TERM_LIFE",
        company: "HDFC Life",
        policyName: "Click 2 Protect",
        policyNumber: "TL123456789",
        sumAssured: 10000000,
        annualPremium: 15000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2020-01-15"),
        maturityDate: new Date("2055-01-15"),
        renewalMonth: 1,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Health Insurance - Family Floater
    {
        userId: userId,
        type: "HEALTH",
        healthType: "FAMILY_FLOATER",
        company: "Star Health",
        policyName: "Family Health Optima",
        policyNumber: "HI987654321",
        sumAssured: 1000000,
        annualPremium: 25000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2021-04-01"),
        renewalMonth: 4,
        coversPostRetirement: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Health - Group (employer)
    {
        userId: userId,
        type: "HEALTH",
        healthType: "GROUP",
        company: "ICICI Lombard",
        policyName: "Company Group Insurance",
        policyNumber: "GRP456789",
        sumAssured: 500000,
        annualPremium: 0,
        premiumFrequency: "YEARLY",
        coversPostRetirement: false,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // ULIP
    {
        userId: userId,
        type: "ULIP",
        company: "ICICI Prudential",
        policyName: "Signature",
        policyNumber: "ULIP123456",
        sumAssured: 2500000,
        annualPremium: 100000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2019-07-01"),
        maturityDate: new Date("2034-07-01"),
        currentFundValue: 650000,
        expectedMaturityValue: 2000000,
        renewalMonth: 7,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Endowment
    {
        userId: userId,
        type: "ENDOWMENT",
        company: "LIC",
        policyName: "Jeevan Anand",
        policyNumber: "END789456",
        sumAssured: 1000000,
        annualPremium: 50000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2015-09-01"),
        maturityDate: new Date("2035-09-01"),
        currentFundValue: 550000,
        expectedMaturityValue: 1500000,
        renewalMonth: 9,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Money Back
    {
        userId: userId,
        type: "MONEY_BACK",
        company: "LIC",
        policyName: "Money Back Policy",
        policyNumber: "MB321654",
        sumAssured: 500000,
        annualPremium: 30000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2018-03-15"),
        maturityDate: new Date("2038-03-15"),
        currentFundValue: 200000,
        expectedMaturityValue: 800000,
        renewalMonth: 3,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Vehicle
    {
        userId: userId,
        type: "VEHICLE",
        company: "Bajaj Allianz",
        policyName: "Car Comprehensive",
        policyNumber: "VH159753",
        sumAssured: 800000,
        annualPremium: 12000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2024-01-01"),
        renewalMonth: 1,
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

db.insurance.insertMany(insurances);
print("  Added " + insurances.length + " insurance policies");

// ============================================
// 6. INCOME
// ============================================
print("6. Adding Income Sources...");

const incomes = [
    {
        userId: userId,
        type: "SALARY",
        name: "Primary Job - IT Company",
        description: "Software Engineer salary",
        grossAmount: 150000,
        netAmount: 125000,
        frequency: "MONTHLY",
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        type: "RENTAL",
        name: "Rental Income - Flat",
        description: "2BHK flat in Pune on rent",
        grossAmount: 18000,
        netAmount: 18000,
        frequency: "MONTHLY",
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        type: "DIVIDEND",
        name: "Stock Dividends",
        description: "Annual dividends from direct equity",
        grossAmount: 25000,
        netAmount: 25000,
        frequency: "YEARLY",
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        userId: userId,
        type: "FREELANCE",
        name: "Freelance Consulting",
        description: "Part-time consulting work",
        grossAmount: 30000,
        netAmount: 30000,
        frequency: "MONTHLY",
        isActive: true,
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

db.incomes.insertMany(incomes);
print("  Added " + incomes.length + " income sources");

// ============================================
// SUMMARY
// ============================================
print("\n============================================");
print("  Test Data Seeding Complete!");
print("============================================\n");
print("Data added for user: " + userId);
print("  - " + investments.length + " Investments");
print("  - " + expenses.length + " Expenses (including time-bound school fees)");
print("  - " + loans.length + " Loans");
print("  - " + goals.length + " Goals");
print("  - " + insurances.length + " Insurance Policies");
print("  - " + incomes.length + " Income Sources");
print("\nRefresh the app to see the data!");
