# Test Data Seed Script for Retyrment
# User: rateshb@gmail.com, Age: 35, Retirement: 65

$API_BASE = "http://localhost:8080/api"

# You need to get the auth token first - login via browser and copy from localStorage
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Retyrment Test Data Seeder" -ForegroundColor Cyan  
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Please login to the app in browser first, then:" -ForegroundColor Yellow
Write-Host "1. Open browser DevTools (F12)" -ForegroundColor Yellow
Write-Host "2. Go to Application > Local Storage > localhost:3000" -ForegroundColor Yellow
Write-Host "3. Copy the value of 'retyrment_token'" -ForegroundColor Yellow
Write-Host ""

$token = Read-Host "Paste your auth token"

if ([string]::IsNullOrWhiteSpace($token)) {
    Write-Host "Token is required!" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $token"
}

function Invoke-Api {
    param($Method, $Endpoint, $Body)
    
    $uri = "$API_BASE$Endpoint"
    try {
        if ($Body) {
            $response = Invoke-RestMethod -Uri $uri -Method $Method -Headers $headers -Body ($Body | ConvertTo-Json -Depth 10)
        } else {
            $response = Invoke-RestMethod -Uri $uri -Method $Method -Headers $headers
        }
        return $response
    } catch {
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

Write-Host ""
Write-Host "Starting data seed..." -ForegroundColor Green
Write-Host ""

# ============================================
# 1. INVESTMENTS
# ============================================
Write-Host "1. Adding Investments..." -ForegroundColor Cyan

$investments = @(
    # PPF Account
    @{
        type = "PPF"
        name = "SBI PPF Account"
        description = "Public Provident Fund - 15 year lock-in"
        investedAmount = 1500000
        currentValue = 1800000
        expectedReturn = 7.1
        startDate = "2015-04-01"
        maturityDate = "2030-04-01"
    },
    # EPF
    @{
        type = "EPF"
        name = "EPFO Account"
        description = "Employee Provident Fund"
        investedAmount = 2000000
        currentValue = 2500000
        expectedReturn = 8.15
        monthlySip = 15000
    },
    # Mutual Funds - Equity
    @{
        type = "MUTUAL_FUND"
        name = "Axis Bluechip Fund"
        description = "Large Cap Equity Fund"
        investedAmount = 500000
        currentValue = 750000
        expectedReturn = 12
        monthlySip = 10000
    },
    @{
        type = "MUTUAL_FUND"
        name = "Mirae Asset Emerging Bluechip"
        description = "Large & Mid Cap Fund"
        investedAmount = 300000
        currentValue = 480000
        expectedReturn = 14
        monthlySip = 5000
    },
    @{
        type = "MUTUAL_FUND"
        name = "Parag Parikh Flexi Cap"
        description = "Flexi Cap Fund"
        investedAmount = 200000
        currentValue = 280000
        expectedReturn = 13
        monthlySip = 5000
    },
    # FD
    @{
        type = "FD"
        name = "HDFC Bank FD"
        description = "Fixed Deposit - 3 year"
        investedAmount = 500000
        currentValue = 550000
        expectedReturn = 7
        startDate = "2023-01-15"
        maturityDate = "2026-01-15"
    },
    # RD
    @{
        type = "RD"
        name = "ICICI RD"
        description = "Recurring Deposit"
        investedAmount = 120000
        currentValue = 135000
        expectedReturn = 6.5
        monthlySip = 10000
        maturityDate = "2025-12-01"
    },
    # Stocks
    @{
        type = "STOCK"
        name = "Direct Equity Portfolio"
        description = "Reliance, TCS, HDFC Bank, Infosys"
        investedAmount = 400000
        currentValue = 520000
        expectedReturn = 15
    },
    # NPS
    @{
        type = "NPS"
        name = "NPS Tier 1"
        description = "National Pension System"
        investedAmount = 300000
        currentValue = 380000
        expectedReturn = 10
        monthlySip = 5000
    },
    # Gold
    @{
        type = "GOLD"
        name = "Sovereign Gold Bonds"
        description = "SGB 2019 Series"
        investedAmount = 200000
        currentValue = 320000
        expectedReturn = 8
        maturityDate = "2027-03-15"
    },
    # Real Estate
    @{
        type = "REAL_ESTATE"
        name = "Plot in Pune"
        description = "Residential plot - Hinjewadi"
        investedAmount = 2500000
        currentValue = 4000000
        expectedReturn = 8
    },
    # Cash
    @{
        type = "CASH"
        name = "Emergency Fund"
        description = "Savings Account + Liquid Fund"
        investedAmount = 300000
        currentValue = 300000
        expectedReturn = 4
    }
)

foreach ($inv in $investments) {
    Write-Host "  Adding: $($inv.name)..." -NoNewline
    $result = Invoke-Api -Method "POST" -Endpoint "/investments" -Body $inv
    if ($result) {
        Write-Host " Done" -ForegroundColor Green
    }
}

# ============================================
# 2. EXPENSES
# ============================================
Write-Host ""
Write-Host "2. Adding Expenses..." -ForegroundColor Cyan

$expenses = @(
    # Regular Monthly Expenses
    @{
        category = "RENT"
        name = "House Rent"
        amount = 25000
        frequency = "MONTHLY"
        isFixed = $true
    },
    @{
        category = "UTILITIES"
        name = "Electricity + Gas + Water"
        amount = 4000
        frequency = "MONTHLY"
        isFixed = $true
    },
    @{
        category = "GROCERIES"
        name = "Monthly Groceries"
        amount = 12000
        frequency = "MONTHLY"
        isFixed = $false
    },
    @{
        category = "TRANSPORT"
        name = "Fuel + Maintenance"
        amount = 8000
        frequency = "MONTHLY"
        isFixed = $false
    },
    @{
        category = "HEALTHCARE"
        name = "Medical Expenses"
        amount = 3000
        frequency = "MONTHLY"
        isFixed = $false
    },
    @{
        category = "ENTERTAINMENT"
        name = "OTT + Outings"
        amount = 5000
        frequency = "MONTHLY"
        isFixed = $false
    },
    @{
        category = "SUBSCRIPTIONS"
        name = "Internet + Mobile + Subscriptions"
        amount = 2500
        frequency = "MONTHLY"
        isFixed = $true
    },
    # Time-bound Education Expenses
    @{
        category = "SCHOOL_FEE"
        name = "Child 1 - School Fee"
        amount = 45000
        frequency = "QUARTERLY"
        isFixed = $true
        isTimeBound = $true
        dependentName = "Aarav"
        dependentDob = "2016-05-15"
        endAge = 18
        annualIncreasePercent = 8
    },
    @{
        category = "SCHOOL_FEE"
        name = "Child 2 - School Fee"
        amount = 35000
        frequency = "QUARTERLY"
        isFixed = $true
        isTimeBound = $true
        dependentName = "Ananya"
        dependentDob = "2019-08-20"
        endAge = 18
        annualIncreasePercent = 8
    },
    @{
        category = "TUITION"
        name = "Tuition Classes"
        amount = 5000
        frequency = "MONTHLY"
        isFixed = $true
        isTimeBound = $true
        dependentName = "Aarav"
        dependentDob = "2016-05-15"
        endAge = 18
    },
    # Yearly expenses
    @{
        category = "TRAVEL"
        name = "Annual Vacation"
        amount = 100000
        frequency = "YEARLY"
        isFixed = $false
    },
    @{
        category = "SHOPPING"
        name = "Festival Shopping"
        amount = 50000
        frequency = "YEARLY"
        isFixed = $false
    }
)

foreach ($exp in $expenses) {
    Write-Host "  Adding: $($exp.name)..." -NoNewline
    $result = Invoke-Api -Method "POST" -Endpoint "/expenses" -Body $exp
    if ($result) {
        Write-Host " Done" -ForegroundColor Green
    }
}

# ============================================
# 3. LOANS
# ============================================
Write-Host ""
Write-Host "3. Adding Loans..." -ForegroundColor Cyan

$loans = @(
    @{
        type = "HOME"
        name = "HDFC Home Loan"
        description = "Home loan for flat in Mumbai"
        originalAmount = 5000000
        outstandingAmount = 3500000
        interestRate = 8.5
        emi = 45000
        startDate = "2020-06-01"
        endDate = "2035-06-01"
        tenure = 180
    },
    @{
        type = "VEHICLE"
        name = "ICICI Car Loan"
        description = "Car loan for Honda City"
        originalAmount = 800000
        outstandingAmount = 350000
        interestRate = 9.0
        emi = 17000
        startDate = "2022-03-15"
        endDate = "2027-03-15"
        tenure = 60
    }
)

foreach ($loan in $loans) {
    Write-Host "  Adding: $($loan.name)..." -NoNewline
    $result = Invoke-Api -Method "POST" -Endpoint "/loans" -Body $loan
    if ($result) {
        Write-Host " Done" -ForegroundColor Green
    }
}

# ============================================
# 4. GOALS
# ============================================
Write-Host ""
Write-Host "4. Adding Goals..." -ForegroundColor Cyan

$currentYear = (Get-Date).Year

$goals = @(
    @{
        name = "Child 1 Higher Education"
        description = "Engineering/MBA for Aarav"
        targetAmount = 2500000
        targetYear = $currentYear + 12
        currentSavings = 200000
        priority = "HIGH"
    },
    @{
        name = "Child 2 Higher Education"
        description = "Education fund for Ananya"
        targetAmount = 3000000
        targetYear = $currentYear + 15
        currentSavings = 100000
        priority = "HIGH"
    },
    @{
        name = "Child 1 Marriage"
        description = "Marriage expenses for Aarav"
        targetAmount = 2000000
        targetYear = $currentYear + 20
        currentSavings = 0
        priority = "MEDIUM"
    },
    @{
        name = "Dream Home"
        description = "Upgrade to bigger house"
        targetAmount = 10000000
        targetYear = $currentYear + 10
        currentSavings = 500000
        priority = "MEDIUM"
    },
    @{
        name = "World Tour"
        description = "International vacation with family"
        targetAmount = 800000
        targetYear = $currentYear + 5
        currentSavings = 150000
        priority = "LOW"
    },
    @{
        name = "New Car"
        description = "Upgrade to SUV"
        targetAmount = 1500000
        targetYear = $currentYear + 3
        currentSavings = 300000
        priority = "LOW"
    }
)

foreach ($goal in $goals) {
    Write-Host "  Adding: $($goal.name)..." -NoNewline
    $result = Invoke-Api -Method "POST" -Endpoint "/goals" -Body $goal
    if ($result) {
        Write-Host " Done" -ForegroundColor Green
    }
}

# ============================================
# 5. INSURANCE
# ============================================
Write-Host ""
Write-Host "5. Adding Insurance Policies..." -ForegroundColor Cyan

$insurances = @(
    # Term Life
    @{
        type = "TERM_LIFE"
        company = "HDFC Life"
        policyName = "Click 2 Protect"
        policyNumber = "TL123456789"
        sumAssured = 10000000
        annualPremium = 15000
        premiumFrequency = "YEARLY"
        startDate = "2020-01-15"
        maturityDate = "2055-01-15"
        renewalMonth = 1
    },
    # Health Insurance - Personal
    @{
        type = "HEALTH"
        healthType = "FAMILY_FLOATER"
        company = "Star Health"
        policyName = "Family Health Optima"
        policyNumber = "HI987654321"
        sumAssured = 1000000
        annualPremium = 25000
        premiumFrequency = "YEARLY"
        startDate = "2021-04-01"
        renewalMonth = 4
        coversPostRetirement = $true
    },
    # Health - Group (employer)
    @{
        type = "HEALTH"
        healthType = "GROUP"
        company = "ICICI Lombard"
        policyName = "Company Group Insurance"
        policyNumber = "GRP456789"
        sumAssured = 500000
        annualPremium = 0
        premiumFrequency = "YEARLY"
        coversPostRetirement = $false
    },
    # ULIP
    @{
        type = "ULIP"
        company = "ICICI Prudential"
        policyName = "Signature"
        policyNumber = "ULIP123456"
        sumAssured = 2500000
        annualPremium = 100000
        premiumFrequency = "YEARLY"
        startDate = "2019-07-01"
        maturityDate = "2034-07-01"
        currentFundValue = 650000
        expectedMaturityValue = 2000000
        renewalMonth = 7
    },
    # Endowment
    @{
        type = "ENDOWMENT"
        company = "LIC"
        policyName = "Jeevan Anand"
        policyNumber = "END789456"
        sumAssured = 1000000
        annualPremium = 50000
        premiumFrequency = "YEARLY"
        startDate = "2015-09-01"
        maturityDate = "2035-09-01"
        currentFundValue = 550000
        expectedMaturityValue = 1500000
        renewalMonth = 9
    },
    # Money Back
    @{
        type = "MONEY_BACK"
        company = "LIC"
        policyName = "Money Back Policy"
        policyNumber = "MB321654"
        sumAssured = 500000
        annualPremium = 30000
        premiumFrequency = "YEARLY"
        startDate = "2018-03-15"
        maturityDate = "2038-03-15"
        currentFundValue = 200000
        expectedMaturityValue = 800000
        renewalMonth = 3
    },
    # Vehicle
    @{
        type = "VEHICLE"
        company = "Bajaj Allianz"
        policyName = "Car Comprehensive"
        policyNumber = "VH159753"
        sumAssured = 800000
        annualPremium = 12000
        premiumFrequency = "YEARLY"
        startDate = "2024-01-01"
        renewalMonth = 1
    }
)

foreach ($ins in $insurances) {
    Write-Host "  Adding: $($ins.policyName)..." -NoNewline
    $result = Invoke-Api -Method "POST" -Endpoint "/insurance" -Body $ins
    if ($result) {
        Write-Host " Done" -ForegroundColor Green
    }
}

# ============================================
# 6. INCOME
# ============================================
Write-Host ""
Write-Host "6. Adding Income Sources..." -ForegroundColor Cyan

$incomes = @(
    @{
        type = "SALARY"
        name = "Primary Job - IT Company"
        description = "Software Engineer salary"
        grossAmount = 150000
        netAmount = 125000
        frequency = "MONTHLY"
        isActive = $true
    },
    @{
        type = "RENTAL"
        name = "Rental Income - Flat"
        description = "2BHK flat in Pune on rent"
        grossAmount = 18000
        netAmount = 18000
        frequency = "MONTHLY"
        isActive = $true
    },
    @{
        type = "DIVIDEND"
        name = "Stock Dividends"
        description = "Annual dividends from direct equity"
        grossAmount = 25000
        netAmount = 25000
        frequency = "YEARLY"
        isActive = $true
    },
    @{
        type = "FREELANCE"
        name = "Freelance Consulting"
        description = "Part-time consulting work"
        grossAmount = 30000
        netAmount = 30000
        frequency = "MONTHLY"
        isActive = $true
    }
)

foreach ($inc in $incomes) {
    Write-Host "  Adding: $($inc.name)..." -NoNewline
    $result = Invoke-Api -Method "POST" -Endpoint "/income" -Body $inc
    if ($result) {
        Write-Host " Done" -ForegroundColor Green
    }
}

# ============================================
# SUMMARY
# ============================================
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  Test Data Seeding Complete!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "Data added:" -ForegroundColor Cyan
Write-Host "  - $($investments.Count) Investments (PPF, EPF, MF, FD, RD, Stocks, NPS, Gold, Real Estate, Cash)"
Write-Host "  - $($expenses.Count) Expenses (Regular + Time-bound school fees)"
Write-Host "  - $($loans.Count) Loans (Home + Car)"
Write-Host "  - $($goals.Count) Goals (Education, Marriage, Home, Travel, Car)"
Write-Host "  - $($insurances.Count) Insurance Policies (Term, Health, ULIP, Endowment, Money Back)"
Write-Host "  - $($incomes.Count) Income Sources (Salary, Rental, Dividend, Freelance)"
Write-Host ""
Write-Host "Now refresh the Retyrment app to see all the test data!" -ForegroundColor Yellow
