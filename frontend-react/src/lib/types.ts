// API Types and Interfaces

export interface User {
  id: string;
  email: string;
  name: string;
  role: 'FREE' | 'PRO' | 'ADMIN';
  effectiveRole?: string;
  profilePicture?: string;
}

export interface FeatureAccess {
  incomePage: boolean;
  investmentPage: boolean;
  loanPage: boolean;
  insurancePage: boolean;
  expensePage: boolean;
  goalsPage: boolean;
  familyPage: boolean;
  calendarPage: boolean;
  retirementPage: boolean;
  insuranceRecommendationsPage: boolean;
  reportsPage: boolean;
  simulationPage: boolean;
  canRunSimulation: boolean;
  adminPanel: boolean;
  preferencesPage: boolean;
  settingsPage: boolean;
  accountPage: boolean;
  retirementStrategyPlannerTab: boolean;
  retirementWithdrawalStrategyTab: boolean;
  canExportPdf: boolean;
  canExportExcel: boolean;
  canExportJson: boolean;
  canImportData: boolean;
  allowedInvestmentTypes: string[];
  blockedInsuranceTypes: string[];
}

export interface Income {
  id?: string;
  source: string;
  monthlyAmount: number;
  annualIncrement?: number;
  startDate?: string;
  isActive?: boolean;
}

export interface Investment {
  id?: string;
  name: string;
  type: string;
  description?: string;
  investedAmount?: number;
  currentValue: number;
  purchasePrice?: number;
  // SIP/Recurring fields
  monthlySip?: number;
  sipDay?: number;
  rdDay?: number;
  yearlyContribution?: number;
  // Dates
  purchaseDate?: string;
  evaluationDate?: string;
  maturityDate?: string;
  startDate?: string;
  // Returns
  interestRate?: number;
  expectedReturn?: number;
  // Emergency fund tagging (for FD/RD)
  isEmergencyFund?: boolean;
  tenureMonths?: number;
  // Real Estate specific fields
  realEstateType?: 'SELF_OCCUPIED' | 'RENTAL' | 'INVESTMENT' | 'INHERITED';
  monthlyRentalIncome?: number;
  rentalYield?: number;
  isPrimaryResidence?: boolean;
  expectedAppreciation?: number;
  maintenanceCost?: number;
  propertyTax?: number;
  vacancyRate?: number;
}

export interface Loan {
  id?: string;
  type: 'HOME' | 'VEHICLE' | 'PERSONAL' | 'EDUCATION' | 'CREDIT_CARD' | 'OTHER';
  name: string;
  description?: string;
  originalAmount: number;
  outstandingAmount: number;
  emi: number;
  emiDay?: number;
  interestRate: number;
  tenureMonths?: number;
  remainingMonths?: number;
  startDate?: string;
  endDate?: string;
}

export interface Insurance {
  id?: string;
  type: string;
  healthType?: string;
  company: string;
  policyName: string;
  policyNumber?: string;
  sumAssured: number;
  annualPremium: number;
  premiumFrequency: string;
  renewalMonth?: number;
  startDate: string;
  maturityDate?: string;
  continuesAfterRetirement?: boolean;
  coverageEndAge?: number;
  policyTerm?: number;
  maturityBenefit?: number;
  moneyBackYears?: string;
  moneyBackPercent?: number;
  moneyBackAmount?: number;
  moneyBackPayouts?: Array<{
    policyYear?: number;
    percentage?: number;
    fixedAmount?: number;
    includesBonus?: boolean;
    description?: string;
  }>;
  isAnnuityPolicy?: boolean;
  premiumPaymentYears?: number;
  annuityStartYear?: number;
  monthlyAnnuityAmount?: number;
  annuityGrowthRate?: number;
}

export interface Expense {
  id?: string;
  category: string;
  name: string;
  amount: number;
  monthlyAmount?: number;
  isFixed?: boolean;
  frequency?: string;
  isTimeBound?: boolean;
  startDate?: string;
  endDate?: string;
  endAge?: number;
  dependentName?: string;
  dependentDob?: string;
  annualIncreasePercent?: number;
  inflationRate?: number;
  isEssential?: boolean;
}

export interface Goal {
  id?: string;
  name: string;
  description?: string;
  targetAmount: number;
  targetYear: number;
  currentSavings: number;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  isRecurring?: boolean;
  recurrenceInterval?: number;
  recurrenceEndYear?: number;
}

export interface FamilyMember {
  id?: string;
  name: string;
  relationship: string;
  dateOfBirth: string;
  gender?: string;
  isEarning?: boolean;
  isDependent?: boolean;
  hasPreExistingConditions?: boolean;
  existingHealthCover?: number;
  existingLifeCover?: number;
}
