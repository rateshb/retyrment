# Retyrment - Financial Planning Application

## Project Overview

A comprehensive personal finance planning application to help individual users and families track their investments, liabilities, income, and plan for future financial goals.

---

## Document Status

| Field | Value |
|-------|-------|
| **Status** | Final Draft - Phase 1 |
| **Last Updated** | 2026-01-14 |
| **Author** | Retyrment Team |

---

## 1. Target Users

| User Type | Included | Notes |
|-----------|----------|-------|
| Individual investors managing personal finances | ✅ Yes | Primary target |
| Families planning for long-term goals | ✅ Yes | Education, marriage, retirement |
| Users with multiple investment platforms | ❌ Phase 2 | Cross-platform aggregation later |

**Scope:** Personal use only (not for financial advisors/planners)

---

## 2. Technology Stack

### 2.1 Finalized Stack

| Layer | Technology |
|-------|------------|
| **Frontend** | HTML5, CSS3 (Tailwind CSS), Vanilla JavaScript |
| **Charts** | Chart.js |
| **Backend** | Java 17+ with Spring Boot |
| **Database** | MongoDB |
| **Build Tool** | Maven or Gradle |

### 2.2 Environment Configuration

**Profiles:**

| Profile | Usage | MongoDB | Port |
|---------|-------|---------|------|
| `local` | Local development | localhost:27017 | 8080 |
| `dev` | Dev server | dev-mongo-server:27017 | 8080 |
| `prod` | Production | prod-mongo-cluster:27017 | 8080 |

**Configuration Files:**

| File | Purpose |
|------|---------|
| `application.yml` | Common settings (shared across all profiles) |
| `application-local.yml` | Local MongoDB, debug logging |
| `application-dev.yml` | Dev server settings, info logging |
| `application-prod.yml` | Production settings, warn logging, optimizations |

**Run Scripts:**

| Script | Command | Description |
|--------|---------|-------------|
| `run-local.bat/sh` | `mvn spring-boot:run -Dspring.profiles.active=local` | Run locally |
| `run-dev.bat/sh` | `java -jar app.jar --spring.profiles.active=dev` | Run on dev server |
| `run-prod.bat/sh` | `java -jar app.jar --spring.profiles.active=prod` | Run in production |

**Environment Variables (for sensitive data):**

| Variable | Description |
|----------|-------------|
| `MONGO_URI` | MongoDB connection string |
| `MONGO_DATABASE` | Database name |
| `JWT_SECRET` | Secret for JWT tokens (Phase 2) |

---

### 2.3 Browser Support

| Browser | Supported |
|---------|-----------|
| Chrome (latest) | ✅ Yes |
| Firefox (latest) | ✅ Yes |
| Safari | ❌ No |
| Edge | ❌ No |
| Mobile browsers | ❌ No |

---

## 3. Core Features - Phase 1

### 3.1 Income Tracking (NEW)

| Field | Description |
|-------|-------------|
| Source Name | e.g., "Salary - TCS", "Freelance" |
| Monthly Amount | Current monthly income (₹) |
| Annual Increment (%) | Default: Inflation + 1% |
| Start Date | When this income started |

**Auto-Calculated:**
- Yearly income projection
- Income growth over time
- Total lifetime earnings projection

---

### 3.2 Assets & Investments

| Category | Fields | Auto-Calculated |
|----------|--------|-----------------|
| **Mutual Funds** | Name, Invested Amount, Current Value, Purchase Date, Monthly SIP | Gain/Loss, % Return, XIRR |
| **Stocks/Equity** | Name, Invested Amount, Current Value, Purchase Date | Gain/Loss, % Return |
| **Fixed Deposits** | Bank, Principal, Interest Rate, Start Date, Maturity Date | Maturity Value, Days Remaining |
| **Recurring Deposits** | Bank, Monthly Amount, Tenure, Rate, Start Date | Maturity Value |
| **PPF** | Balance, Yearly Contribution, Account Start Year | Projected Balance, Maturity Year |
| **EPF** | Balance, Monthly Contribution (Employee + Employer) | Projected Balance |
| **NPS** | Balance, Monthly Contribution, Asset Allocation | Projected Balance |
| **Real Estate** | Description, Purchase Price, Purchase Date, Current Value | Appreciation %, Annualized Return |
| **Gold/Others** | Description, Invested, Current Value, Purchase Date | Gain/Loss, % Return |
| **Savings/Cash** | Account Name, Bank, Balance | - |

**Features:**
- ✅ Multiple entries per category
- ✅ Track purchase dates for all investments
- ✅ User-configurable expected returns per asset
- ❌ External API integration (Phase 2)
- ❌ Foreign currency support (Phase 2)

---

### 3.3 Liabilities & Loans

| Category | Fields | Auto-Calculated |
|----------|--------|-----------------|
| **Home Loan** | Bank, Original Amount, Outstanding, Interest Rate, EMI, Tenure, Start Date | Total Interest, Payoff Date, Amortization Schedule |
| **Vehicle Loan** | Description, Outstanding, Interest Rate, EMI, Tenure | Payoff Date, Amortization |
| **Personal Loan** | Description, Outstanding, Interest Rate, EMI, Tenure | Payoff Date, Amortization |
| **Education Loan** | Description, Outstanding, Interest Rate, EMI, Moratorium Period | Payoff Date |
| **Credit Card Debt** | Card Name, Outstanding, Interest Rate | Monthly Interest |

**Amortization Features:**
- Principal vs Interest breakdown per EMI
- Remaining balance after each payment
- Total interest paid over loan lifetime
- Prepayment impact calculator

---

### 3.4 Insurance Policies

#### Policy Types Supported

| Type | Description | Key Features |
|------|-------------|--------------|
| **TERM_LIFE** | Pure protection | No maturity benefit, continues after retirement option |
| **HEALTH** | Medical coverage | Group/Personal/Family Floater subtypes |
| **ULIP** | Unit-linked investment | Fund value tracking, market-linked returns |
| **ENDOWMENT** | Traditional savings | Guaranteed returns + bonus |
| **MONEY_BACK** | Periodic payouts | Money back at specified intervals |
| **ANNUITY** | Pension plans | Pay for N years, receive monthly from N+1 |
| **VEHICLE** | Auto insurance | Annual renewal |
| **OTHER** | Miscellaneous | Custom policies |

#### Basic Fields

| Field | Description |
|-------|-------------|
| Policy Type | One of the types above |
| Company & Policy Name | e.g., "LIC Jeevan Anand" |
| Policy Number | For reference |
| Sum Assured / Cover | Coverage amount (₹) |
| Policy Start Date | When policy was taken |
| Policy Term | Duration in years |

#### Premium Payment Details

| Field | Description |
|-------|-------------|
| Premium Frequency | YEARLY, HALF_YEARLY, QUARTERLY, MONTHLY, SINGLE |
| Premium Amount | Per-installment amount (₹) |
| Annual Premium | Auto-calculated total yearly premium |
| Premium Payment Years | For limited-pay policies (e.g., pay for 12 years) |
| Premium End Age | Age until which premium is payable |
| Continues After Retirement | For Term/Health - does premium continue? |

#### Premium Schedule (varies by frequency)

| Frequency | Schedule Fields |
|-----------|-----------------|
| YEARLY | Renewal Month (Jan-Dec) |
| HALF_YEARLY | Payment Months (e.g., "Jan & Jul") |
| QUARTERLY | First Payment Month (e.g., "Jan" for Jan/Apr/Jul/Oct) |
| MONTHLY | Payment Day of Month (1-28) |
| SINGLE | One-time payment |

#### Health Insurance Subtypes

| Subtype | Description | Post-Retirement |
|---------|-------------|-----------------|
| GROUP | Employer-provided | ❌ Ends at retirement |
| PERSONAL | Individual policy | ✅ Continues |
| FAMILY_FLOATER | Family coverage | ✅ Continues |

#### Investment-Linked Policies (ULIP/Endowment/Money-Back)

| Field | Description |
|-------|-------------|
| Fund Value | Current fund value (₹) |
| Guaranteed Returns (%) | For traditional policies |
| Bonus Accrued | Accumulated bonus (₹) |
| Maturity Benefit | Expected maturity amount (₹) |
| Maturity Date | When policy matures |

#### Money-Back Specific Fields

| Field | Description |
|-------|-------------|
| Money Back Years | Years when payouts occur (e.g., "5,10,15,20") |
| Money Back Percent | % of sum assured received each time |
| Money Back Amount | Fixed amount if not percentage-based |

#### Annuity/Pension Policies

| Field | Description |
|-------|-------------|
| Is Annuity Policy | Flag for pension-type policies |
| Annuity Start Year | Years from start when payments begin |
| Monthly Annuity Amount | Monthly amount received (₹) |
| Annuity Growth Rate | Annual increase in annuity (%) |

**Example Annuity Policy:**
- Pay premium for 15 years
- From year 16, receive ₹25,000/month
- Annuity increases 3% annually

---

### 3.5 Monthly Expenses

| Field | Description |
|-------|-------------|
| Category | Rent, Utilities, Groceries, Transport, Entertainment, Education, Healthcare, EMI, Insurance, Others |
| Description | Specific description |
| Monthly Amount | Current monthly expense (₹) |
| Is Fixed/Variable | For budgeting |

**Auto-Calculated:**
- Total monthly expenses
- Yearly expense projection
- Expense breakdown by category (pie chart)
- Savings rate (Income - Expenses)

---

### 3.6 Investment & Outflow Calendar (NEW)

A month-wise calendar view showing all recurring financial commitments throughout the year.

**Calendar Layout:**

| S.No | Description/Month | JAN | FEB | MAR | APR | MAY | JUN | JUL | AUG | SEP | OCT | NOV | DEC |
|------|-------------------|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|
| 1 | MF SIP | ₹45K | ₹45K | ₹45K | ₹45K | ₹45K | ₹45K | ₹45K | ₹45K | ₹45K | ₹45K | ₹45K | ₹45K |
| 2 | Health Insurance | - | - | - | - | ₹25K | - | - | - | - | - | - | - |
| 3 | Term Plan Renewal | - | - | - | - | - | - | ₹38K | - | - | - | - | - |
| 4 | LIC Premium | ₹16K | - | - | - | - | - | - | ₹36K | - | ₹30K | - | - |
| 5 | Car Insurance | - | - | - | - | - | - | - | - | - | - | - | ₹20K |
| | **GRAND TOTAL** | ₹61K | ₹45K | ₹45K | ₹45K | ₹70K | ₹45K | ₹83K | ₹81K | ₹45K | ₹75K | ₹45K | ₹65K |

**Data Sources (Auto-populated from):**

| Calendar Item | Source |
|---------------|--------|
| MF SIP | Mutual Fund entries with SIP amount |
| RD Installments | Recurring Deposit entries |
| PPF Contribution | PPF entry (yearly - user picks month) |
| Insurance Premiums | Insurance policies with renewal month |
| Loan EMIs | All active loans |
| School/Education Fees | Expense entries marked as periodic |
| Precious Metal SIP | Gold/Other investments with monthly contribution |

**Features:**

| Feature | Description |
|---------|-------------|
| **Auto-populate** | Calendar auto-fills from SIPs, EMIs, Insurance policies |
| **Manual entries** | Add one-time or periodic payments manually |
| **Payment Due Month** | Each entry has a "due month" field |
| **Frequency** | Monthly / Quarterly / Half-Yearly / Yearly |
| **Reminder** | Flag items due in current/next month |
| **Yearly Total** | Sum of all 12 months per item |
| **Monthly Grand Total** | Sum of all items per month |

**Calendar Entry Fields:**

| Field | Description |
|-------|-------------|
| Description | Name of the payment |
| Category | SIP / Insurance / EMI / Education / Investment / Other |
| Amount | Payment amount (₹) |
| Frequency | Monthly / Quarterly / Half-Yearly / Yearly |
| Due Months | Which months this is due (e.g., [1,4,7,10] for quarterly) |
| Auto-linked | Is this linked to another entry (MF, Insurance, Loan)? |
| Reminder Days | Days before due date to show reminder |

**Views:**

| View | Description |
|------|-------------|
| **Calendar View** | 12-month grid (as shown above) |
| **List View** | Chronological list of upcoming payments |
| **Monthly View** | Detailed breakdown for selected month |
| **Category View** | Grouped by SIP/Insurance/EMI/etc. |

**API Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/calendar` | Get full year calendar |
| GET | `/api/calendar/month/{month}` | Get specific month details |
| GET | `/api/calendar/upcoming` | Get next 30 days payments |
| POST | `/api/calendar` | Add manual calendar entry |
| PUT | `/api/calendar/{id}` | Update calendar entry |
| DELETE | `/api/calendar/{id}` | Delete calendar entry |

---

### 3.7 Retirement Roadmap Matrix (NEW)

A comprehensive year-by-year projection matrix showing how all investments grow towards retirement, with variable return assumptions and goal tracking.

**Input Parameters (Configurable):**

| Parameter | Description | Example |
|-----------|-------------|---------|
| **Retirement Year** | Target retirement year | 2045 |
| **Current Age** | User's current age | 35 |
| **Retirement Age** | Planned retirement age | 60 |

**Return Assumptions (Variable by Period):**

| Investment | 1-5 Yrs | 6-10 Yrs | 11-15 Yrs | 16-21 Yrs |
|------------|---------|----------|-----------|-----------|
| EPF Returns | 8.50% | 7.50% | 6.50% | 5.50% |
| PPF Returns | 7.10% | 6.10% | 5.10% | 4.10% |
| MF Returns | 12% | 11% | 10% | 9% |
| Debt Funds | 7% | 6.5% | 6% | 5.5% |

*Note: Returns decrease over time to be conservative as retirement approaches*

**SIP/Investment Growth Parameters:**

| Parameter | Description | Example |
|-----------|-------------|---------|
| Current SIP Amount | Monthly SIP today | ₹20,000 |
| Annual SIP Step-Up (%) | Yearly increase in SIP | 10% |
| Additional SIP | Extra SIP amount | ₹10,000 |
| Lumpsum Top-Up (Yearly) | Annual lumpsum investment | ₹5,00,000 |
| Existing MF Balance | Current MF corpus | ₹41,00,000 |

**Matrix Columns:**

| Column | Description |
|--------|-------------|
| **S.No** | Serial number |
| **Year** | Calendar year (2024, 2025, ...) |
| **Age** | User's age that year |
| **Investment (Yearly)** | Total amount invested that year |
| **PPF Balance** | PPF corpus with reducing returns |
| **EPF Balance** | EPF corpus with reducing returns |
| **Monthly EPF Contribution** | Employee + Employer contribution |
| **MF SIP Balance** | SIP corpus growth |
| **MF Lumpsum Balance** | Lumpsum investment growth |
| **Total MF Balance** | Combined MF corpus |
| **FD/Debt Balance** | Fixed income investments |
| **NPS Balance** | NPS corpus growth |
| **Insurance Maturities** | Policies maturing that year |
| **Guaranteed Income** | From maturing policies/annuities |
| **Goal Outflows** | Money needed for goals that year |
| **Net Corpus** | Total - Goal Outflows |
| **Monthly Retirement Income** | Corpus / Remaining years / 12 |

**Insurance Maturity Tracking:**

| Policy | Maturity Year | Expected Payout |
|--------|---------------|-----------------|
| LIC Jeevan Anand (Self) - 30 Yrs | 2040 | ₹28,400 (yearly) |
| LIC Jeevan Anand (Spouse) - 20 Yrs | 2035 | ₹X |
| ULIP Maturity | 2038 | ₹Y |
| Endowment Policy | 2042 | ₹Z |

**Goal Outflow Timeline:**

| Year | Goal | Amount (Inflated) |
|------|------|-------------------|
| 2027 | Child Education - Class 11 | ₹5,00,000 |
| 2029 | Child Higher Education | ₹25,00,000 |
| 2035 | Child Marriage | ₹40,00,000 |
| 2045 | Retirement Corpus | ₹2,00,00,000 |

**Sample Matrix Output:**

| S.No | Year | Age | PPF Balance | EPF Balance | MF Balance | Insurance Maturity | Goal Outflow | Net Corpus |
|------|------|-----|-------------|-------------|------------|-------------------|--------------|------------|
| 1 | 2024 | 35 | ₹7,00,000 | ₹9,43,200 | ₹51,60,000 | - | - | ₹68,03,200 |
| 2 | 2025 | 36 | ₹7,49,700 | ₹10,21,234 | ₹58,75,200 | - | - | ₹76,46,134 |
| ... | ... | ... | ... | ... | ... | ... | ... | ... |
| 10 | 2033 | 45 | ₹12,50,000 | ₹25,00,000 | ₹1,50,00,000 | LIC ₹28,400 | Education ₹25L | ₹1,62,78,400 |
| ... | ... | ... | ... | ... | ... | ... | ... | ... |
| 21 | 2045 | 60 | ₹18,00,000 | ₹45,00,000 | ₹3,50,00,000 | - | Retirement | ₹4,13,00,000 |

**Features:**

| Feature | Description |
|---------|-------------|
| **Customizable Horizon** | Set any retirement year/age |
| **Variable Returns** | Different rates for different periods |
| **Step-Up SIP** | Annual % increase in SIP |
| **Insurance Maturity Integration** | Auto-add maturities from policies |
| **Goal Integration** | Auto-add goal outflows from goals |
| **What-If Scenarios** | Change assumptions, see impact |
| **Export to Excel** | Download matrix as spreadsheet |
| **PDF Report** | Generate retirement plan document |

**Calculations:**

| Calculation | Formula |
|-------------|---------|
| PPF Year N | Balance(N-1) × (1 + Rate) + Yearly Contribution |
| EPF Year N | Balance(N-1) × (1 + Rate) + (Monthly × 12) |
| MF SIP Year N | Previous + SIP FV for 12 months |
| Step-Up SIP | SIP(N) = SIP(N-1) × (1 + StepUp%) |
| Net Corpus | Sum(All Balances) + Maturities - Goal Outflows |
| Monthly Income | (Final Corpus) / (Life Expectancy - Retirement Age) / 12 |

**Chart Visualizations:**

| Chart | Type | Purpose |
|-------|------|---------|
| Corpus Growth | Stacked Area | Show contribution of each investment |
| Year-wise Breakdown | Stacked Bar | PPF vs EPF vs MF vs Others |
| Goal vs Corpus | Line Chart | Track if on target |
| Income Projection | Line Chart | Post-retirement income stream |

**API Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/retirement/matrix` | Get full retirement matrix |
| POST | `/api/retirement/calculate` | Calculate with custom assumptions |
| GET | `/api/retirement/scenarios` | Get saved scenarios |
| POST | `/api/retirement/scenarios` | Save a scenario |
| GET | `/api/retirement/export/excel` | Export matrix to Excel |
| GET | `/api/retirement/export/pdf` | Generate PDF report |

**Saved Scenarios:**
Users can save multiple "what-if" scenarios:
- Conservative (lower returns)
- Moderate (expected returns)
- Aggressive (higher returns)
- Early Retirement (retire at 50)
- Late Retirement (retire at 65)

---

### 3.8 Future Goals

| Field | Description |
|-------|-------------|
| Goal Name | User-defined name |
| Icon | Visual identifier (emoji) |
| Target Amount | In today's value (₹) |
| Target Year | When the money is needed |
| Priority | High / Medium / Low |
| Is Recurring | Yes/No (for annual goals) |
| Recurrence | Yearly (if recurring) |

**Pre-defined Goal Templates:**

| Goal | Default Amount | Default Years |
|------|----------------|---------------|
| 🚗 Car Purchase | ₹12,00,000 | 3 |
| 🏠 House / Down Payment | ₹50,00,000 | 8 |
| 🎓 Child's Education | ₹25,00,000 | 15 |
| 💒 Child's Marriage | ₹30,00,000 | 20 |
| 🏖️ Retirement Corpus | ₹1,00,00,000 | 25 |
| ✈️ Annual Vacation | ₹2,00,000 | 1 (recurring) |

**Phase 2 Goals:**
- 🆘 Emergency Fund
- 💻 Business Start-up
- 💰 Wealth Creation

---

## 4. Analysis & Projections

### 4.0 High Priority Alerts (Dashboard)

The dashboard displays actionable financial alerts:

| Alert Type | Trigger Condition | Action |
|------------|-------------------|--------|
| **Corpus Gap** | Required > Projected | Increase SIP or investments |
| **Maturity Available** | FD/RD/PPF/Insurance maturing before retirement | Reinvest for growth |
| **Illiquid Asset Help** | Gold+RE can cover gap | Consider partial liquidation |
| **Post-Loan Opportunity** | Loan EMI ending within 10 years | Redirect EMI to investments |
| **Emergency Fund Gap** | Cash < 6 months expenses | Prioritize emergency fund |
| **Underfunded Goals** | Goal funding < 50% | Review goal or savings |

### 4.1 Core Calculations

| Metric | Formula/Logic |
|--------|---------------|
| **Net Worth** | Total Assets - Total Liabilities |
| **Retirement Corpus** | All liquid assets (PPF, EPF, MF, NPS, FD, RD, Stock, Cash) |
| **Excluded from Corpus** | Illiquid assets (Gold, Real Estate, Crypto) |
| **Savings Rate** | (Income - Expenses) / Income × 100 |
| **Investment Returns** | XIRR for actual, assumed rates for projection |
| **Inflation Adjustment** | User-configurable (Default: 6% p.a.) |
| **SIP Future Value** | FV = PMT × [((1+r)^n - 1) / r] × (1+r) |
| **Lumpsum Future Value** | FV = PV × (1 + r)^n |
| **Funding Gap** | Inflated Goal Value - Projected Portfolio |
| **Required SIP** | Reverse FV calculation |

### 4.2 Default Return Assumptions (User Configurable)

| Asset Class | Default Return | Configurable |
|-------------|----------------|--------------|
| Mutual Funds (Equity) | 12% p.a. | ✅ Yes |
| Direct Equity | 14% p.a. | ✅ Yes |
| Debt Funds | 7% p.a. | ✅ Yes |
| PPF | 7.1% p.a. | ✅ Yes |
| EPF | 8.15% p.a. | ✅ Yes |
| NPS | 10% p.a. | ✅ Yes |
| FD/RD | Per entry rate | ✅ Yes |
| Real Estate | 8% p.a. | ✅ Yes |
| Gold | 8% p.a. | ✅ Yes |
| Savings Account | 3.5% p.a. | ✅ Yes |
| **Inflation** | 6% p.a. | ✅ Yes |
| **Income Growth** | Inflation + 1% | ✅ Yes |

### 4.3 Output Metrics & Reports

| Metric | Included |
|--------|----------|
| Current Net Worth | ✅ |
| Projected Net Worth (5, 10, 15, 20, 25 years) | ✅ |
| Year-by-year projection table | ✅ |
| Goal-wise funding status (% funded) | ✅ |
| Monthly investment requirement | ✅ |
| Personalized recommendations | ✅ |
| Monte Carlo simulation (probability of success) | ✅ |
| Historical net worth tracking | ✅ |

### 4.4 Charts & Visualizations

| Chart | Type | Purpose |
|-------|------|---------|
| Net Worth Over Time | Line Chart | Track growth |
| Asset Allocation | Pie/Donut Chart | Diversification view |
| Income vs Expenses | Bar Chart | Monthly comparison |
| Goal Progress | Progress Bars | Funding status |
| Projection Scenarios | Area Chart | Best/Worst/Expected |
| Loan Amortization | Stacked Bar | Principal vs Interest |
| Expense Breakdown | Pie Chart | Category-wise |
| **Monthly Outflow Calendar** | Stacked Bar | Month-wise payment breakdown |
| **Cash Flow Heatmap** | Heatmap | High/Low outflow months |

---

## 5. User Experience

### 5.1 Navigation

| Feature | Included |
|---------|----------|
| Step-by-step wizard for first-time setup | ✅ |
| Tab-based navigation for returning users | ✅ |
| Dashboard with drill-down | ✅ |
| Charts on dashboard | ✅ |

### 5.2 Data Entry

| Feature | Included |
|---------|----------|
| Modal-based forms for add/edit | ✅ |
| Inline editing for quick updates | ✅ |
| Copy/Duplicate entry | ✅ |
| Bulk import from CSV/Excel | ❌ Phase 2 |

### 5.3 Data Management

| Feature | Included |
|---------|----------|
| MongoDB persistence | ✅ |
| Export to JSON backup | ✅ |
| Import from JSON backup | ✅ |
| Cloud sync | ❌ Phase 2 |
| Password protection | ❌ Phase 2 |

### 5.4 Reports & Export

| Feature | Included |
|---------|----------|
| Text report download | ✅ |
| PDF report generation | ✅ |
| Excel export | ❌ Phase 2 |
| Email report | ❌ Phase 2 |
| Print-friendly view | ❌ Phase 2 |

---

## 6. Data Model (MongoDB Collections)

### 6.1 Collections Overview

```
Retyrment_db
├── users (future - for multi-user)
├── income
├── investments
├── loans
├── insurance
├── expenses
├── goals
├── calendar_entries (manual calendar items)
├── retirement_scenarios (saved what-if scenarios)
├── settings
└── history (for net worth snapshots)
```

### 6.2 Sample Document Structures

**Income:**
```json
{
  "_id": "ObjectId",
  "source": "Salary - Company",
  "monthlyAmount": 150000,
  "annualIncrement": 7,
  "startDate": "2020-01-01",
  "isActive": true,
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

**Investment:**
```json
{
  "_id": "ObjectId",
  "type": "mutualFund",
  "name": "Axis Bluechip Fund",
  "investedAmount": 100000,
  "currentValue": 125000,
  "purchaseDate": "2023-06-15",
  "monthlySIP": 5000,
  "expectedReturn": 12,
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

**Loan:**
```json
{
  "_id": "ObjectId",
  "type": "homeLoan",
  "bank": "SBI",
  "originalAmount": 5000000,
  "outstandingAmount": 4200000,
  "interestRate": 8.5,
  "emi": 45000,
  "tenureMonths": 180,
  "startDate": "2022-01-01",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

**Insurance:**
```json
{
  "_id": "ObjectId",
  "type": "ULIP",
  "company": "ICICI Prudential",
  "policyName": "Signature",
  "sumAssured": 1000000,
  "annualPremium": 50000,
  "premiumFrequency": "yearly",
  "renewalMonth": 5,
  "startDate": "2020-05-01",
  "maturityDate": "2035-05-01",
  "fundValue": 180000,
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

**Calendar Entry (Manual):**
```json
{
  "_id": "ObjectId",
  "description": "School Fee - Term 1",
  "category": "education",
  "amount": 400000,
  "frequency": "quarterly",
  "dueMonths": [4, 9],
  "autoLinked": false,
  "linkedTo": null,
  "reminderDays": 7,
  "isActive": true,
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

**Retirement Scenario:**
```json
{
  "_id": "ObjectId",
  "name": "Moderate Plan",
  "description": "Expected returns with 10% SIP step-up",
  "currentAge": 35,
  "retirementAge": 60,
  "lifeExpectancy": 85,
  "assumptions": {
    "epfReturns": [
      { "fromYear": 1, "toYear": 5, "rate": 8.5 },
      { "fromYear": 6, "toYear": 10, "rate": 7.5 },
      { "fromYear": 11, "toYear": 15, "rate": 6.5 },
      { "fromYear": 16, "toYear": 25, "rate": 5.5 }
    ],
    "ppfReturns": [
      { "fromYear": 1, "toYear": 5, "rate": 7.1 },
      { "fromYear": 6, "toYear": 10, "rate": 6.1 },
      { "fromYear": 11, "toYear": 15, "rate": 5.1 },
      { "fromYear": 16, "toYear": 25, "rate": 4.1 }
    ],
    "mfReturns": [
      { "fromYear": 1, "toYear": 10, "rate": 12 },
      { "fromYear": 11, "toYear": 20, "rate": 10 },
      { "fromYear": 21, "toYear": 25, "rate": 8 }
    ],
    "inflationRate": 6,
    "sipStepUpPercent": 10,
    "lumpsumFrequency": "yearly",
    "lumpsumAmount": 500000
  },
  "isDefault": true,
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

---

## 7. API Endpoints (Spring Boot)

### 7.1 Income APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/income` | Get all income sources |
| POST | `/api/income` | Add new income |
| PUT | `/api/income/{id}` | Update income |
| DELETE | `/api/income/{id}` | Delete income |

### 7.2 Investment APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/investments` | Get all investments |
| GET | `/api/investments/type/{type}` | Get by type |
| POST | `/api/investments` | Add investment |
| PUT | `/api/investments/{id}` | Update investment |
| DELETE | `/api/investments/{id}` | Delete investment |

### 7.3 Loan APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/loans` | Get all loans |
| GET | `/api/loans/{id}/amortization` | Get amortization schedule |
| POST | `/api/loans` | Add loan |
| PUT | `/api/loans/{id}` | Update loan |
| DELETE | `/api/loans/{id}` | Delete loan |

### 7.4 Insurance APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/insurance` | Get all policies |
| POST | `/api/insurance` | Add policy |
| PUT | `/api/insurance/{id}` | Update policy |
| DELETE | `/api/insurance/{id}` | Delete policy |

### 7.5 Expense APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/expenses` | Get all expenses |
| POST | `/api/expenses` | Add expense |
| PUT | `/api/expenses/{id}` | Update expense |
| DELETE | `/api/expenses/{id}` | Delete expense |

### 7.6 Goal APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/goals` | Get all goals |
| POST | `/api/goals` | Add goal |
| PUT | `/api/goals/{id}` | Update goal |
| DELETE | `/api/goals/{id}` | Delete goal |

### 7.7 Analysis APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/analysis/networth` | Get current net worth |
| GET | `/api/analysis/projection` | Get future projections |
| GET | `/api/analysis/goals` | Get goal-wise analysis |
| GET | `/api/analysis/recommendations` | Get recommendations |
| GET | `/api/analysis/montecarlo` | Run Monte Carlo simulation |

### 7.8 Settings & Export APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/settings` | Get user settings |
| PUT | `/api/settings` | Update settings |
| GET | `/api/export/json` | Export all data as JSON |
| POST | `/api/import/json` | Import data from JSON |
| GET | `/api/export/pdf` | Generate PDF report |

---

## 8. Project Structure

```
Retyrment/
├── backend/
│   ├── src/main/java/com/Retyrment/
│   │   ├── RetyrmentApplication.java
│   │   ├── config/
│   │   │   ├── MongoConfig.java
│   │   │   └── CorsConfig.java
│   │   ├── controller/
│   │   │   ├── IncomeController.java
│   │   │   ├── InvestmentController.java
│   │   │   ├── LoanController.java
│   │   │   ├── InsuranceController.java
│   │   │   ├── ExpenseController.java
│   │   │   ├── GoalController.java
│   │   │   ├── CalendarController.java
│   │   │   ├── RetirementController.java
│   │   │   └── AnalysisController.java
│   │   ├── model/
│   │   │   ├── Income.java
│   │   │   ├── Investment.java
│   │   │   ├── Loan.java
│   │   │   ├── Insurance.java
│   │   │   ├── Expense.java
│   │   │   ├── Goal.java
│   │   │   ├── CalendarEntry.java
│   │   │   ├── RetirementScenario.java
│   │   │   └── RetirementAssumptions.java
│   │   ├── repository/
│   │   │   └── ...Repository.java
│   │   ├── service/
│   │   │   ├── CalculationService.java
│   │   │   ├── ProjectionService.java
│   │   │   ├── MonteCarloService.java
│   │   │   ├── CalendarService.java
│   │   │   ├── RetirementService.java
│   │   │   ├── ExcelExportService.java
│   │   │   └── ReportService.java
│   │   └── dto/
│   │       └── ...DTO.java
│   ├── src/main/resources/
│   │   ├── application.yml (common config)
│   │   ├── application-local.yml (local dev)
│   │   ├── application-dev.yml (development server)
│   │   └── application-prod.yml (production)
│   ├── scripts/
│   │   ├── run-local.bat (Windows)
│   │   ├── run-local.sh (Linux/Mac)
│   │   ├── run-dev.bat
│   │   ├── run-dev.sh
│   │   ├── run-prod.bat
│   │   └── run-prod.sh
│   └── pom.xml
│
├── frontend/
│   ├── index.html
│   ├── css/
│   │   └── styles.css (Tailwind)
│   ├── js/
│   │   ├── app.js
│   │   ├── api.js
│   │   ├── charts.js
│   │   └── calculations.js
│   └── assets/
│
└── README.md
```

---

## 9. Success Criteria

| Criteria | Target |
|----------|--------|
| User can add all investments | < 10 minutes |
| Net worth visible at a glance | ✅ Dashboard |
| Goal tracking clarity | ✅ Progress bars + % |
| Actionable recommendations | ✅ Personalized |
| Data persistence | ✅ MongoDB |
| Charts load time | < 2 seconds |
| PDF report generation | < 5 seconds |

---

## 10. Phase Plan

### Phase 1 (Current Scope)
- ✅ Income tracking with growth projection
- ✅ All asset categories with purchase dates
- ✅ Loan amortization schedules
- ✅ Insurance tracking (all types including ULIP/Endowment)
- ✅ Expense categorization
- ✅ Goal planning with recurring goals
- ✅ **Investment & Outflow Calendar** (12-month view)
- ✅ **Retirement Roadmap Matrix** (year-by-year projections)
- ✅ Charts (Chart.js)
- ✅ MongoDB backend
- ✅ Year-by-year projections
- ✅ Monte Carlo simulation
- ✅ PDF report export
- ✅ JSON backup/restore
- ✅ Excel export (for retirement matrix)

### Phase 2 (Future)
- [ ] External API for live MF/Stock prices
- [ ] Foreign currency investments
- [ ] Multi-platform aggregation
- [ ] Emergency fund & business goals
- [ ] Goal-investment linking
- [ ] CSV/Excel import
- [ ] Tax calculations (80C, 80D)
- [ ] Cloud sync & password protection
- [ ] Mobile responsive design
- [ ] Email reports

---

## 11. Authentication & Authorization

### 11.1 Google OAuth2 Login

| Feature | Description |
|---------|-------------|
| Provider | Google OAuth2 |
| Flow | OAuth2 Authorization Code |
| Token | JWT stored in localStorage |
| Session | Stateless (JWT-based) |

### 11.2 User Roles

| Role | Description | Features |
|------|-------------|----------|
| `FREE` | Default for new users | Basic tracking, limited goals |
| `PRO` | Premium subscribers | Custom assumptions, exports, recommendations |
| `ADMIN` | Superusers | User management, all features |

### 11.3 Role-Based Feature Access

| Feature | FREE | PRO | ADMIN |
|---------|------|-----|-------|
| Track investments | ✅ | ✅ | ✅ |
| Basic retirement view | ✅ | ✅ | ✅ |
| Goals (max 3) | ✅ | ✅ | ✅ |
| Custom retirement assumptions | ❌ | ✅ | ✅ |
| GAP analysis | ❌ | ✅ | ✅ |
| PDF/Excel export | ❌ | ✅ | ✅ |
| Recommendations | ❌ | ✅ | ✅ |
| Admin panel | ❌ | ❌ | ✅ |

---

## 12. Code Quality & Security

### 12.1 Backend Code Quality Plugins

| Plugin | Purpose | Command |
|--------|---------|---------|
| **JaCoCo** | Code coverage | `mvn test jacoco:report` |
| **SpotBugs** | Bug & security detection | `mvn spotbugs:check` |
| **FindSecBugs** | Security vulnerabilities | Included in SpotBugs |
| **PMD** | Code quality & complexity | `mvn pmd:check` |
| **CPD** | Copy/paste detection | `mvn pmd:cpd-check` |

### 12.2 Security Measures

| Measure | Implementation |
|---------|----------------|
| Authentication | Google OAuth2 + JWT |
| CORS | Configured for frontend origin |
| CSRF | Disabled (stateless API) |
| Input Validation | Spring Validation annotations |
| SQL Injection | N/A (MongoDB, no SQL) |
| XSS | Frontend escaping |

### 12.3 Testing Requirements

| Type | Tool | Coverage Target |
|------|------|-----------------|
| Unit Tests | JUnit 5 | 70% minimum |
| Integration Tests | Spring Boot Test | Critical paths |
| Security Tests | Spring Security Test | Auth flows |
| Embedded MongoDB | Flapdoodle | Test isolation |

---

## 13. Landing Page & Marketing

### 13.1 Landing Page (`landing.html`)

| Section | Content |
|---------|---------|
| Hero | Value proposition, CTA buttons |
| Features | 6 key features with icons |
| Pricing | Free vs Pro comparison |
| CTA | Final call-to-action |
| Footer | Links, copyright |

### 13.2 Pricing Model

| Plan | Price | Features |
|------|-------|----------|
| **Free** | ₹0/forever | Basic tracking, 3 goals, calendar |
| **Pro** | ₹499/month (₹4,999/year) | Unlimited goals, exports, assumptions |

---

## 14. Feature Access Control System

### 14.1 Overview
A comprehensive per-user feature access control system that allows admins to:
- Control page visibility on a per-user basis
- Restrict investment and insurance types per user
- Enable/disable specific features (reports, simulation, calendar, etc.)
- Override default role-based access

### 14.2 Implementation Details

**Backend Components:**
- `UserFeatureAccess` model - Stores per-user access settings in MongoDB
- `FeatureAccessService` - Manages access logic, defaults, and overrides
- `AdminController` - Endpoints for managing feature access (`/admin/users/{userId}/features`)
- `AuthController` - Returns feature access in `/auth/features` endpoint

**Frontend Components:**
- `common.js` - Applies feature restrictions on page load
- Navigation items automatically hidden for restricted pages
- Page access checks redirect unauthorized users to dashboard
- Feature-specific restrictions (investment types, report buttons, etc.)

### 14.3 Default Access Rules

**Always Visible (Cannot be restricted):**
- Income Page
- Loan Page
- Expense Page
- Settings Page
- Account Page

**Visible by Default (Can be restricted):**
- Investment Page
- Insurance Page
- Goals Page
- Retirement Page

**Restricted by Default (Admin-enabled):**
- Calendar Page
- Reports Page
- Simulation Page
- Preferences Page
- Retirement Strategy Planner Tab

**Admin Only:**
- Admin Panel

### 14.4 Investment Type Control

**Default Allowed Types:**
- Mutual Funds (MUTUAL_FUND)
- PPF
- EPF
- FD (Fixed Deposit)
- RD (Recurring Deposit)
- Real Estate

**Admin-Controlled Types:**
- Stocks (STOCK)
- NPS
- Gold
- Crypto
- Cash

**Behavior:**
- Only allowed types appear in investment type dropdown
- Restricted types are completely hidden from user interface
- Admin can customize allowed types per user via Admin Panel

### 14.5 Insurance Type Control

**Default Blocked Types:**
- Vehicle Insurance (VEHICLE)
- Pension Plans (PENSION)
- Life Savings Plans (LIFE_SAVINGS)

**Available Types:**
- Health Insurance
- Term Life Insurance
- ULIP
- Endowment
- Money-Back

**Behavior:**
- Blocked types don't appear in insurance type dropdown
- Admin can customize blocked types per user via Admin Panel

### 14.6 Retirement Page Tab Control

| Tab | Default Access | Admin Control |
|-----|----------------|---------------|
| Retirement Matrix | ✅ All users | Always visible |
| Strategy Planner | ❌ Restricted | Admin-enabled |

**Behavior:**
- If Strategy Planner tab is enabled, Retirement Page becomes visible
- Tab visibility is controlled per user
- Matrix tab is always visible if Retirement Page is accessible

### 14.7 Report Access Control

| Report Type | Default Access | Admin Control |
|-------------|----------------|---------------|
| PDF Export | ❌ Restricted | Admin-enabled |
| Excel Export | ❌ Restricted | Admin-enabled |
| JSON Export | ❌ Restricted | Admin-enabled |
| Data Import | ❌ Restricted | Admin-enabled |

**Behavior:**
- Export buttons are disabled for users without access
- Import functionality is hidden for restricted users
- Access is controlled per user via Admin Panel

### 14.8 Cross-Link Handling
- Navigation links respect feature flags and are hidden if access is denied
- Direct URL access to restricted pages redirects to dashboard with error message
- Feature-restricted buttons are disabled with visual indicators
- Access denied messages shown when appropriate

### 14.9 Admin Panel Features

**Feature Access Management:**
- View user's current feature access
- Toggle page visibility per user
- Set allowed investment types per user
- Set blocked insurance types per user
- Enable/disable retirement tabs per user
- Control report access (PDF/Excel/JSON/Import) per user

**UI:**
- "Features" button in user actions column
- Modal dialog with organized sections:
  - Page Access toggles
  - Investment Types checkboxes
  - Insurance Restrictions checkboxes
  - Retirement Tab toggles
  - Report Access toggles

## 15. Development Notes

```
2026-01-14: Requirements finalized
- Scope: Personal users only
- Backend: Java + Spring Boot + MongoDB
- Frontend: HTML/CSS (Tailwind) + JS + Chart.js
- Income tracking added with yearly increment feature
- Insurance models: Track Traditional, Endowment, ULIP, Money-back
- Loan amortization: Full detailed schedule
- Historical tracking: Yes, store net worth snapshots
- Charts: Essential for Phase 1
- Default income growth: Inflation + 1%
- Google OAuth2 authentication added
- User roles: FREE, PRO, ADMIN
- Code quality plugins: JaCoCo, SpotBugs, PMD, CPD
- Landing page for marketing

2026-01-15: Feature Access Control System
- Per-user feature access control implemented
- UserFeatureAccess model created
- FeatureAccessService for access management
- Admin panel enhanced with feature management UI
- My Account page added for subscription and access visibility
- Logout functionality fixed
- Watch Demo button removed from landing page
- Navigation respects feature flags
- Cross-link handling implemented
- Investment and insurance type restrictions
- Report access control
- Retirement tab visibility control
```

---

## 15. Quick Start

### Running Locally

```bash
# 1. Start MongoDB (Docker)
docker run -d -p 27017:27017 --name mongodb mongo:latest

# 2. Run Backend
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 3. Run Frontend
cd frontend
npx serve . -l 3000

# 4. Open browser
# Landing: http://localhost:3000/landing.html
# App: http://localhost:3000/login.html
```

### Code Quality Reports

```bash
cd backend

# Run tests with coverage
mvn test jacoco:report
# Report: target/site/jacoco/index.html

# Run SpotBugs (security)
mvn spotbugs:check
# Report: target/spotbugsXml.xml

# Run PMD (code quality)
mvn pmd:check
# Report: target/pmd.xml

# Run CPD (duplication)
mvn pmd:cpd-check
# Report: target/cpd.xml
```

---

## 16. Next Steps

1. [x] Set up Spring Boot project with MongoDB
2. [x] Create data models (entities)
3. [x] Implement CRUD APIs
4. [x] Build calculation services
5. [x] Create frontend with Tailwind CSS
6. [x] Integrate Chart.js
7. [x] Implement projections & Monte Carlo
8. [x] Add PDF export functionality
9. [x] Google OAuth2 authentication
10. [x] User roles (FREE/PRO/ADMIN)
11. [x] Code quality plugins
12. [x] Landing page
13. [ ] Complete unit test coverage (70%+)
14. [ ] Performance optimization
15. [ ] Production deployment

---

**Application is now feature-complete for Phase 1!**
