// API Configuration and Helpers
const API_BASE = 'http://localhost:8080/api';

// Auth helpers
export const auth = {
  getToken: () => localStorage.getItem('retyrment_token'),
  setToken: (token: string) => localStorage.setItem('retyrment_token', token),
  removeToken: () => localStorage.removeItem('retyrment_token'),
  getUser: () => {
    const user = localStorage.getItem('retyrment_user');
    return user ? JSON.parse(user) : null;
  },
  setUser: (user: User) => localStorage.setItem('retyrment_user', JSON.stringify(user)),
  removeUser: () => localStorage.removeItem('retyrment_user'),
  isLoggedIn: () => !!localStorage.getItem('retyrment_token'),
  logout: () => {
    auth.removeToken();
    auth.removeUser();
    localStorage.removeItem('retyrment_features');
    window.location.href = '/login';
  },
  getHeaders: () => {
    const token = auth.getToken();
    return {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    };
  },
};

// Types
export interface User {
  id: string;
  email: string;
  name: string;
  role: 'FREE' | 'PRO' | 'ADMIN';
  effectiveRole?: string;
  profilePicture?: string;
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
}

// API error handler
class ApiError extends Error {
  status: number;
  
  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

// Generic API request function
async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers: {
      ...auth.getHeaders(),
      ...options.headers,
    },
  });

  if (response.status === 401) {
    auth.logout();
    throw new ApiError(401, 'Session expired');
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: `HTTP ${response.status}` }));
    throw new ApiError(response.status, error.message || `HTTP ${response.status}`);
  }

  // Handle empty responses
  const text = await response.text();
  if (!text) return {} as T;
  
  return JSON.parse(text);
}

// API endpoints
export const api = {
  // Auth
  auth: {
    me: () => request<User>('/auth/me'),
    features: () => request<{ features: FeatureAccess }>('/auth/features'),
  },

  // Income
  income: {
    getAll: () => request<Income[]>('/income'),
    create: (data: Income) => request<Income>('/income', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    update: (id: string, data: Income) => request<Income>(`/income/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
    delete: (id: string) => request<void>(`/income/${id}`, { method: 'DELETE' }),
  },

  // Investments
  investments: {
    getAll: () => request<Investment[]>('/investments'),
    create: (data: Investment) => request<Investment>('/investments', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    update: (id: string, data: Investment) => request<Investment>(`/investments/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
    delete: (id: string) => request<void>(`/investments/${id}`, { method: 'DELETE' }),
  },

  // Loans
  loans: {
    getAll: () => request<Loan[]>('/loans'),
    create: (data: Loan) => request<Loan>('/loans', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    update: (id: string, data: Loan) => request<Loan>(`/loans/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
    delete: (id: string) => request<void>(`/loans/${id}`, { method: 'DELETE' }),
  },

  // Insurance
  insurance: {
    getAll: () => request<Insurance[]>('/insurance'),
    create: (data: Insurance) => request<Insurance>('/insurance', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    update: (id: string, data: Insurance) => request<Insurance>(`/insurance/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
    delete: (id: string) => request<void>(`/insurance/${id}`, { method: 'DELETE' }),
  },

  // Expenses
  expenses: {
    getAll: () => request<Expense[]>('/expenses'),
    create: (data: Expense) => request<Expense>('/expenses', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    update: (id: string, data: Expense) => request<Expense>(`/expenses/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
    delete: (id: string) => request<void>(`/expenses/${id}`, { method: 'DELETE' }),
    getInvestmentOpportunities: (currentAge: number, retirementAge: number) =>
      request<any>(`/expenses/investment-opportunities?currentAge=${currentAge}&retirementAge=${retirementAge}`),
  },

  // Goals
  goals: {
    getAll: () => request<Goal[]>('/goals'),
    create: (data: Goal) => request<Goal>('/goals', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    update: (id: string, data: Goal) => request<Goal>(`/goals/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
    delete: (id: string) => request<void>(`/goals/${id}`, { method: 'DELETE' }),
  },

  // Family Members
  family: {
    getAll: () => request<FamilyMember[]>('/family'),
    create: (data: FamilyMember) => request<FamilyMember>('/family', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    update: (id: string, data: FamilyMember) => request<FamilyMember>(`/family/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
    delete: (id: string) => request<void>(`/family/${id}`, { method: 'DELETE' }),
  },

  // Analysis
  analysis: {
    networth: () => request<any>('/analysis/networth'),
    goals: () => request<any>('/analysis/goals'),
    recommendations: () => request<any>('/analysis/recommendations'),
  },

  // Retirement
  retirement: {
    calculate: (scenario: any) => request<any>('/retirement/calculate', {
      method: 'POST',
      body: JSON.stringify(scenario),
    }),
    getMaturing: (currentAge: number, retirementAge: number) =>
      request<any>(`/retirement/maturing?currentAge=${currentAge}&retirementAge=${retirementAge}`),
    getStrategy: () => request<any>('/retirement/strategy'),
    saveStrategy: (strategy: any) => request<any>('/retirement/strategy', {
      method: 'POST',
      body: JSON.stringify(strategy),
    }),
  },

  // Insurance Recommendations
  insuranceRecommendations: {
    getOverall: () => request<any>('/insurance/recommendations'),
  },

  // Simulation
  simulation: {
    run: (simulations: number, years: number) => 
      request<any>(`/analysis/monte-carlo?simulations=${simulations}&years=${years}`),
  },

  // User Data
  userData: {
    getSummary: () => request<any>('/user/data/summary'),
    deleteAll: (confirmation: string) => request<any>(`/user/data/all?confirmation=${confirmation}`, {
      method: 'DELETE',
    }),
  },

  // Admin
  admin: {
    getUsers: () => request<any>('/admin/users'),
    getUser: (id: string) => request<any>(`/admin/users/${id}`),
    searchUsers: (email: string) => request<any>(`/admin/users/search?email=${email}`),
    updateRole: (id: string, data: { role: string; durationDays?: number; reason?: string }) => 
      request<any>(`/admin/users/${id}/role`, {
        method: 'PUT',
        body: JSON.stringify(data),
      }),
    deleteUser: (id: string) => request<any>(`/admin/users/${id}`, { method: 'DELETE' }),
    extendTrial: (id: string, days: number) => request<any>(`/admin/users/${id}/extend-trial`, {
      method: 'PUT',
      body: JSON.stringify({ days }),
    }),
    getFeatureAccess: (id: string) => request<any>(`/admin/users/${id}/features`),
    updateUserFeatures: (id: string, features: any) => request<any>(`/admin/users/${id}/features`, {
      method: 'PUT',
      body: JSON.stringify(features),
    }),
  },

  // User Settings
  settings: {
    get: () => request<any>('/settings'),
    update: (data: any) => request<any>('/settings', {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  },

  // Export
  export: {
    json: () => request<any>('/export/json'),
    importJson: (data: any) => request<any>('/export/import/json', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    getPdfUrl: () => `${API_BASE}/export/pdf?token=${auth.getToken()}`,
    getExcelUrl: () => `${API_BASE}/export/excel?token=${auth.getToken()}`,
  },
};
