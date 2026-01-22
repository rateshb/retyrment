# Generating TypeScript Types from Backend

## Option 1: OpenAPI/Swagger (Recommended)

Add to backend `pom.xml`:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

Then generate TypeScript types:
```bash
npx openapi-typescript http://localhost:8080/api/v3/api-docs -o src/lib/api-types.ts
```

## Option 2: Manual Type Extraction

Reference the vanilla JS frontend's `api.js` to see exact field names used.

## Current Backend Model Fields

### Income
- source: string
- monthlyAmount: number
- annualIncrement: number
- startDate: date
- isActive: boolean

### Investment
- type: MUTUAL_FUND | STOCK | FD | RD | PPF | EPF | NPS | REAL_ESTATE | GOLD | CRYPTO | CASH | OTHER
- name: string
- investedAmount: number
- currentValue: number
- monthlySip: number
- expectedReturn: number
- maturityDate: date
- isEmergencyFund: boolean

### Loan
- type: HOME | VEHICLE | PERSONAL | EDUCATION | CREDIT_CARD | OTHER
- name: string
- originalAmount: number
- outstandingAmount: number
- emi: number
- interestRate: number
- tenureMonths: number
- remainingMonths: number
- startDate: date
- endDate: date

### Insurance
- type: TERM_LIFE | HEALTH | ULIP | ENDOWMENT | MONEY_BACK | ANNUITY | VEHICLE | OTHER
- company: string
- policyName: string
- policyNumber: string
- sumAssured: number
- annualPremium: number
- premiumFrequency: MONTHLY | QUARTERLY | HALF_YEARLY | YEARLY | SINGLE
- startDate: date
- maturityDate: date
- moneyBackPayouts: array (for MONEY_BACK type)

### Expense
- category: RENT | UTILITIES | GROCERIES | TRANSPORT | HEALTHCARE | ... (see backend)
- name: string
- amount: number
- frequency: MONTHLY | QUARTERLY | HALF_YEARLY | YEARLY | ONE_TIME
- isEssential: boolean
- isTimeBound: boolean
- endAge: number
- endDate: date

### Goal
- name: string
- description: string
- targetAmount: number
- targetYear: number
- currentSavings: number
- priority: HIGH | MEDIUM | LOW
- isRecurring: boolean
- recurrenceInterval: number
- recurrenceEndYear: number

### FamilyMember
- name: string
- relationship: SELF | SPOUSE | CHILD | PARENT | SIBLING | OTHER
- dateOfBirth: date
- gender: MALE | FEMALE | OTHER
- isEarning: boolean
- isDependent: boolean
- hasPreExistingConditions: boolean
- existingHealthCover: number
- existingLifeCover: number
