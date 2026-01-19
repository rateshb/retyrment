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
db.family_members.deleteMany({ userId: userId });
print("Done.\n");

// ============================================
// 1. INVESTMENTS (with Emergency Fund Tagging)
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
    // FD - Regular (part of retirement corpus)
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
        isEmergencyFund: false,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // FD - EMERGENCY FUND (excluded from retirement corpus)
    {
        userId: userId,
        type: "FD",
        name: "Emergency FD - SBI",
        description: "Emergency fund - 6 months expenses",
        investedAmount: 300000,
        currentValue: 315000,
        expectedReturn: 6.5,
        startDate: new Date("2024-06-01"),
        maturityDate: new Date("2025-06-01"),
        isEmergencyFund: true,  // TAGGED AS EMERGENCY FUND
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // RD - Regular
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
        isEmergencyFund: false,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // RD - EMERGENCY FUND
    {
        userId: userId,
        type: "RD",
        name: "Emergency RD - HDFC",
        description: "Building emergency fund",
        investedAmount: 60000,
        currentValue: 62000,
        expectedReturn: 6.5,
        monthlySip: 10000,
        maturityDate: new Date("2025-09-01"),
        isEmergencyFund: true,  // TAGGED AS EMERGENCY FUND
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
        name: "Savings Account",
        description: "Liquid cash in savings",
        investedAmount: 100000,
        currentValue: 100000,
        expectedReturn: 4,
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

db.investments.insertMany(investments);
print("  Added " + investments.length + " investments (including 2 tagged as Emergency Fund)");

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
// 4. GOALS (with Recurring Goals)
// ============================================
print("4. Adding Goals (including Recurring Goals)...");

const goals = [
    // One-time goals
    {
        userId: userId,
        name: "Child 1 Higher Education",
        description: "Engineering/MBA for Aarav",
        targetAmount: 2500000,
        targetYear: currentYear + 12,
        currentSavings: 200000,
        priority: "HIGH",
        isRecurring: false,
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
        isRecurring: false,
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
        isRecurring: false,
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
        isRecurring: false,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    
    // RECURRING GOALS - Annual Vacation
    {
        userId: userId,
        name: "Annual Family Vacation",
        description: "Yearly family vacation - domestic/international",
        targetAmount: 200000,
        targetYear: currentYear + 1,
        currentSavings: 50000,
        priority: "MEDIUM",
        isRecurring: true,
        recurrenceInterval: 1,  // Every year
        recurrenceEndYear: currentYear + 25,  // Until retirement
        adjustForInflation: true,
        customInflationRate: 8.0,  // Travel inflation higher than general
        createdAt: new Date(),
        updatedAt: new Date()
    },
    
    // RECURRING GOALS - Car Replacement
    {
        userId: userId,
        name: "Car Replacement",
        description: "Replace car every 7 years",
        targetAmount: 1500000,
        targetYear: currentYear + 7,
        currentSavings: 0,
        priority: "LOW",
        isRecurring: true,
        recurrenceInterval: 7,  // Every 7 years
        recurrenceEndYear: currentYear + 28,  // 4 cars total
        adjustForInflation: true,
        customInflationRate: 5.0,  // Vehicle price inflation
        createdAt: new Date(),
        updatedAt: new Date()
    },
    
    // RECURRING GOALS - Home Renovation
    {
        userId: userId,
        name: "Home Renovation",
        description: "Major home renovation every 10 years",
        targetAmount: 500000,
        targetYear: currentYear + 5,
        currentSavings: 100000,
        priority: "LOW",
        isRecurring: true,
        recurrenceInterval: 10,  // Every 10 years
        recurrenceEndYear: currentYear + 25,
        adjustForInflation: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    
    // RECURRING GOALS - Parents' Anniversary Celebration
    {
        userId: userId,
        name: "Parents Anniversary Celebration",
        description: "Special celebration every 5 years",
        targetAmount: 100000,
        targetYear: currentYear + 2,
        currentSavings: 20000,
        priority: "LOW",
        isRecurring: true,
        recurrenceInterval: 5,  // Every 5 years
        recurrenceEndYear: currentYear + 20,
        adjustForInflation: true,
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

db.goals.insertMany(goals);
print("  Added " + goals.length + " goals (including " + goals.filter(g => g.isRecurring).length + " recurring goals)");

// ============================================
// 5. INSURANCE (with Money Back Payouts)
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
    // Health - Super Top-Up
    {
        userId: userId,
        type: "HEALTH",
        healthType: "SUPER_TOP_UP",
        company: "ICICI Lombard",
        policyName: "Super Top-Up Health",
        policyNumber: "STU456789",
        sumAssured: 2500000,
        annualPremium: 8000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2022-04-01"),
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
    // Money Back - WITH MULTIPLE PAYOUTS
    {
        userId: userId,
        type: "MONEY_BACK",
        company: "LIC",
        policyName: "Jeevan Tarun Money Back",
        policyNumber: "MB321654",
        sumAssured: 1000000,
        annualPremium: 45000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2020-03-15"),
        maturityDate: new Date("2040-03-15"),
        currentFundValue: 200000,
        expectedMaturityValue: 1500000,
        renewalMonth: 3,
        // MONEY BACK PAYOUTS - Multiple scheduled payouts
        moneyBackPayouts: [
            {
                policyYear: 5,
                percentage: 15.0,  // 15% of sum assured at year 5
                includesBonus: false,
                description: "Survival Benefit 1"
            },
            {
                policyYear: 10,
                percentage: 20.0,  // 20% of sum assured at year 10
                includesBonus: false,
                description: "Survival Benefit 2"
            },
            {
                policyYear: 15,
                percentage: 25.0,  // 25% of sum assured at year 15
                includesBonus: false,
                description: "Survival Benefit 3"
            },
            {
                policyYear: 20,
                percentage: 40.0,  // 40% of sum assured + bonus at maturity
                includesBonus: true,
                description: "Maturity Benefit"
            }
        ],
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Another Money Back policy
    {
        userId: userId,
        type: "MONEY_BACK",
        company: "SBI Life",
        policyName: "Smart Money Back Gold",
        policyNumber: "MB654987",
        sumAssured: 500000,
        annualPremium: 25000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2022-01-01"),
        maturityDate: new Date("2037-01-01"),
        currentFundValue: 75000,
        expectedMaturityValue: 700000,
        renewalMonth: 1,
        moneyBackPayouts: [
            {
                policyYear: 5,
                percentage: 20.0,
                includesBonus: false,
                description: "First Survival Benefit"
            },
            {
                policyYear: 10,
                percentage: 30.0,
                includesBonus: false,
                description: "Second Survival Benefit"
            },
            {
                policyYear: 15,
                percentage: 50.0,
                includesBonus: true,
                description: "Maturity + Bonus"
            }
        ],
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
    },
    // Personal Accident
    {
        userId: userId,
        type: "OTHER",
        company: "HDFC ERGO",
        policyName: "Personal Accident Cover",
        policyNumber: "PA789123",
        sumAssured: 5000000,
        annualPremium: 3000,
        premiumFrequency: "YEARLY",
        startDate: new Date("2023-06-01"),
        renewalMonth: 6,
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

db.insurance.insertMany(insurances);
print("  Added " + insurances.length + " insurance policies (including Money Back with payouts)");

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
// 7. FAMILY MEMBERS (NEW FEATURE)
// ============================================
print("7. Adding Family Members...");

const familyMembers = [
    // Self
    {
        userId: userId,
        name: "Ratesh Bansal",
        relationship: "SELF",
        dateOfBirth: new Date("1989-03-15"),
        gender: "MALE",
        isEarning: true,
        monthlyIncome: 150000,
        annualIncome: 1800000,
        hasOwnInsurance: true,
        hasPreExistingConditions: false,
        isSmoker: false,
        isAlcoholic: false,
        isDependent: false,
        existingHealthCover: 1000000,
        existingLifeCover: 10000000,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Spouse
    {
        userId: userId,
        name: "Priya Bansal",
        relationship: "SPOUSE",
        dateOfBirth: new Date("1992-07-22"),
        gender: "FEMALE",
        isEarning: true,
        monthlyIncome: 80000,
        annualIncome: 960000,
        hasOwnInsurance: true,
        hasPreExistingConditions: false,
        isSmoker: false,
        isAlcoholic: false,
        isDependent: false,
        existingHealthCover: 500000,
        existingLifeCover: 5000000,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Child 1
    {
        userId: userId,
        name: "Aarav Bansal",
        relationship: "CHILD",
        dateOfBirth: new Date("2016-05-15"),
        gender: "MALE",
        isEarning: false,
        hasOwnInsurance: false,
        hasPreExistingConditions: false,
        currentEducation: "PRIMARY",
        expectedEducationEndAge: 22,
        isDependent: true,
        dependencyEndAge: 25,
        existingHealthCover: 0,
        existingLifeCover: 0,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Child 2
    {
        userId: userId,
        name: "Ananya Bansal",
        relationship: "CHILD",
        dateOfBirth: new Date("2019-08-20"),
        gender: "FEMALE",
        isEarning: false,
        hasOwnInsurance: false,
        hasPreExistingConditions: false,
        currentEducation: "PRE_SCHOOL",
        expectedEducationEndAge: 22,
        isDependent: true,
        dependencyEndAge: 25,
        existingHealthCover: 0,
        existingLifeCover: 0,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Father
    {
        userId: userId,
        name: "Rajesh Bansal",
        relationship: "PARENT",
        dateOfBirth: new Date("1958-11-10"),
        gender: "MALE",
        isEarning: false,
        hasOwnInsurance: true,
        hasPreExistingConditions: true,
        preExistingConditions: "Diabetes, Hypertension",
        isSmoker: false,
        isAlcoholic: false,
        isDependent: true,
        livesWithUser: true,
        hasSeparateHealthPolicy: true,
        existingHealthCover: 300000,
        existingLifeCover: 0,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    // Mother
    {
        userId: userId,
        name: "Sunita Bansal",
        relationship: "PARENT",
        dateOfBirth: new Date("1962-04-25"),
        gender: "FEMALE",
        isEarning: false,
        hasOwnInsurance: true,
        hasPreExistingConditions: false,
        isSmoker: false,
        isAlcoholic: false,
        isDependent: true,
        livesWithUser: true,
        hasSeparateHealthPolicy: true,
        existingHealthCover: 300000,
        existingLifeCover: 0,
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

db.family_members.insertMany(familyMembers);
print("  Added " + familyMembers.length + " family members");

// ============================================
// SUMMARY
// ============================================
print("\n============================================");
print("  Test Data Seeding Complete!");
print("============================================\n");
print("Data added for user: " + userId);
print("  - " + investments.length + " Investments");
print("    * Including 2 Emergency Fund FD/RDs (excluded from retirement corpus)");
print("  - " + expenses.length + " Expenses (including time-bound school fees)");
print("  - " + loans.length + " Loans");
print("  - " + goals.length + " Goals");
print("    * Including " + goals.filter(g => g.isRecurring).length + " Recurring Goals (vacation, car, renovation)");
print("  - " + insurances.length + " Insurance Policies");
print("    * Including Money Back policies with multi-year payouts");
print("  - " + incomes.length + " Income Sources");
print("  - " + familyMembers.length + " Family Members (for insurance recommendations)");
print("\n=== NEW FEATURES TESTED ===");
print("  1. Emergency Fund Tagging: FD + RD tagged as emergency funds");
print("  2. Recurring Goals: Annual vacation, car replacement every 7 years, etc.");
print("  3. Money Back Payouts: Multiple scheduled survival benefits");
print("  4. Family Members: Self, spouse, 2 children, 2 parents");
print("\nRefresh the app to see the data!");
