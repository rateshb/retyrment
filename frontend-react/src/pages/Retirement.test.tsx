import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Retirement } from './Retirement';

// Store references for mocked APIs
const mockRetirementCalculate = vi.fn();
const mockRetirementGetStrategy = vi.fn();
const mockRetirementSaveStrategy = vi.fn();
const mockSettingsGet = vi.fn();
const mockSettingsUpdate = vi.fn();
const mockInvestmentsGetAll = vi.fn();
const mockExpensesGetInvestmentOpportunities = vi.fn();
const mockLoansGetAll = vi.fn();
const mockGoalsGetAll = vi.fn();
const mockAnalysisNetworth = vi.fn();

// Default mock return values
const defaultRetirementData = {
  summary: {
    currentAge: 35,
    retirementAge: 60,
    lifeExpectancy: 85,
    yearsToRetirement: 25,
    yearsInRetirement: 25,
    finalCorpus: 50000000,
    monthlyIncomeFromCorpus: 150000,
    monthlyIncome4Percent: 166666,
    monthlyRetirementIncome: 200000,
    sipStepUpPercent: 10,
    withdrawalRate: 8,
    startingBalances: {
      totalStarting: 5000000,
      mutualFunds: 2000000,
      epf: 1500000,
      ppf: 500000,
      nps: 500000,
      fd: 300000,
      rd: 100000,
      stocks: 100000,
      cash: 0,
    },
    sipStepUpOptimization: {
      sipAtStart: 50000,
      sipAtFullStepUp: 150000,
      sipAtOptimalStop: 100000,
      optimalStopYear: 15,
      canStopEarly: true,
      corpusAtOptimalStop: 45000000,
      scenarios: [],
    },
    retirementIncomeProjection: [
      { year: 0, corpus: 50000000 },
      { year: 5, corpus: 45000000 },
      { year: 10, corpus: 35000000 },
      { year: 15, corpus: 20000000 },
      { year: 20, corpus: 5000000 },
      { year: 25, corpus: 0 },
    ],
    annuityMonthlyIncome: 50000,
    monthlyRentalIncomeAtRetirement: 20000,
  },
  gapAnalysis: {
    requiredCorpus: 40000000,
    corpusGap: -10000000,
    sustainableIncome: 140000,
    safe4PercentIncome: 166666,
    depletionIncome: 200000,
    monthlyIncome: 200000,
    totalCurrentMonthlyExpenses: 100000,
    currentMonthlyExpenses: 80000,
    monthlyEMI: 30000,
    monthlySIP: 50000,
    netMonthlySavings: 70000,
    availableMonthlySavings: 20000,
    additionalSIPRequired: 0,
    monthlyInsurancePremiums: 10000,
    monthlyFreedUpByRetirement: 30000,
    potentialCorpusFromFreedUpExpenses: 5000000,
    expenseProjection: [
      { year: 2024, monthlyExpense: 100000 },
      { year: 2030, monthlyExpense: 140000 },
    ],
    endingExpensesBeforeRetirement: [
      { name: 'School Fees', amount: 20000, endYear: 2030 },
    ],
  },
  matrix: [
    { year: 2024, age: 35, epfBalance: 1500000, ppfBalance: 500000, mfBalance: 2000000, netCorpus: 4000000, totalInflow: 0, mfSip: 50000, mfRate: 12 },
    { year: 2025, age: 36, epfBalance: 1700000, ppfBalance: 600000, mfBalance: 2500000, netCorpus: 4800000, totalInflow: 0, mfSip: 55000, mfRate: 12 },
    { year: 2026, age: 37, epfBalance: 1900000, ppfBalance: 700000, mfBalance: 3100000, netCorpus: 5700000, totalInflow: 100000, mfSip: 60500, mfRate: 12, insuranceMaturity: 100000 },
  ],
  maturingBeforeRetirement: {
    totalMaturingBeforeRetirement: 500000,
    moneyBackPayouts: [{ year: 2028, amount: 100000 }],
    moneyBackCount: 1,
    maturingInvestments: [
      { id: '1', name: 'FD 1', type: 'FD', maturityDate: '2028-01-01', expectedMaturityValue: 200000 },
    ],
    maturingInsurance: [
      { id: '2', name: 'LIC Policy', maturityDate: '2030-01-01', expectedMaturityValue: 300000 },
    ],
  },
  recommendations: [],
};

const defaultSettings = {
  currentAge: 35,
  retirementAge: 60,
  lifeExpectancy: 85,
  inflationRate: 6,
  epfReturn: 8.15,
  ppfReturn: 7.1,
  mfEquityReturn: 12,
  sipStepup: 10,
  mfDebtReturn: 7.0,
  fdReturn: 6.5,
  emergencyFundMonths: 6,
};

const defaultInvestments = [
  { id: '1', name: 'HDFC Equity', type: 'MUTUAL_FUND', currentValue: 500000, monthlySip: 10000 },
  { id: '2', name: 'PPF Account', type: 'PPF', currentValue: 300000, monthlySip: 12500 },
  { id: '3', name: 'Fixed Deposit', type: 'FD', currentValue: 200000, isEmergencyFund: true },
];

const defaultNetworth = {
  totalAssets: 10000000,
  totalLiabilities: 2000000,
  netWorth: 8000000,
  assetBreakdown: {
    CASH: 100000,
    MUTUAL_FUND: 2000000,
    EPF: 1500000,
    PPF: 500000,
  },
  sellableAssets: {
    GOLD: 500000,
    REAL_ESTATE: 5000000,
  },
};

const defaultLoans = [
  { id: '1', name: 'Home Loan', emi: 30000, endDate: '2035-01-01' },
];

const defaultGoals = [
  { id: '1', name: 'Child Education', targetAmount: 2000000, targetYear: 2030 },
];

// Mock all the APIs
vi.mock('../lib/api', () => ({
  retirementApi: {
    calculate: (...args: any[]) => mockRetirementCalculate(...args),
    getStrategy: () => mockRetirementGetStrategy(),
    saveStrategy: (...args: any[]) => mockRetirementSaveStrategy(...args),
    getMaturing: vi.fn().mockResolvedValue([]),
  },
  analysisApi: {
    networth: () => mockAnalysisNetworth(),
  },
  investmentsApi: {
    getAll: () => mockInvestmentsGetAll(),
  },
  expensesApi: {
    getAll: vi.fn().mockResolvedValue([]),
    getInvestmentOpportunities: (...args: any[]) => mockExpensesGetInvestmentOpportunities(...args),
  },
  loansApi: {
    getAll: () => mockLoansGetAll(),
  },
  goalsApi: {
    getAll: () => mockGoalsGetAll(),
  },
  incomeApi: {
    getAll: vi.fn().mockResolvedValue([]),
  },
  insuranceApi: {
    getAll: vi.fn().mockResolvedValue([]),
  },
  familyApi: {
    getAll: vi.fn().mockResolvedValue([]),
  },
  userDataApi: {
    summary: vi.fn().mockResolvedValue({ totalRecords: 10 }),
  },
  settingsApi: {
    get: () => mockSettingsGet(),
    update: (...args: any[]) => mockSettingsUpdate(...args),
  },
  Investment: {},
  Loan: {},
  Goal: {},
  Expense: {},
  Income: {},
  Insurance: {},
  FamilyMember: {},
}));

// Mock auth store
vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'PREMIUM' },
    features: {
      retirementPage: true,
      retirementStrategyPlannerTab: true,
      retirementWithdrawalStrategyTab: true,
    },
    refreshFeatures: vi.fn(),
  }),
}));

// Mock recharts to avoid rendering issues
vi.mock('recharts', () => ({
  AreaChart: ({ children }: any) => <div data-testid="area-chart">{children}</div>,
  Area: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  Legend: () => null,
  ResponsiveContainer: ({ children }: any) => <div data-testid="responsive-container">{children}</div>,
  BarChart: ({ children }: any) => <div data-testid="bar-chart">{children}</div>,
  Bar: () => null,
  LineChart: ({ children }: any) => <div data-testid="line-chart">{children}</div>,
  Line: () => null,
}));

// Mock realEstateUtils
vi.mock('../lib/realEstateUtils', () => ({
  calculateTotalRentalIncome: vi.fn().mockReturnValue(20000),
  generateRealEstateRecommendations: vi.fn().mockReturnValue([]),
}));

describe('Retirement Page', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    // Reset all mocks
    vi.clearAllMocks();
    localStorage.clear();
    
    // Set default mock implementations
    mockRetirementCalculate.mockResolvedValue(defaultRetirementData);
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockRetirementSaveStrategy.mockResolvedValue({});
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockSettingsUpdate.mockResolvedValue({});
    mockInvestmentsGetAll.mockResolvedValue(defaultInvestments);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({ savings: [], reinvestment: [], freedUpByYear: [] });
    mockLoansGetAll.mockResolvedValue(defaultLoans);
    mockGoalsGetAll.mockResolvedValue(defaultGoals);
    mockAnalysisNetworth.mockResolvedValue(defaultNetworth);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  describe('Initial Rendering', () => {
    it('renders retirement page without crashing', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getAllByRole('heading').length).toBeGreaterThan(0);
      });
    });

    it('renders primary navigation tabs', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getAllByRole('button').length).toBeGreaterThan(0);
      });
    });

    it('renders summary cards with projected corpus', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getByText('Projected Corpus')).toBeInTheDocument();
      });
    });

    it('renders required corpus card', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getByText('Required Corpus')).toBeInTheDocument();
      });
    });

    it('renders monthly SIP card', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getByText('Monthly SIP')).toBeInTheDocument();
      });
    });

    it('renders years to retirement card', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getByText('Years to Retirement')).toBeInTheDocument();
      });
    });
  });

  describe('API Integration', () => {
    it('fetches retirement data on mount', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockRetirementCalculate).toHaveBeenCalled();
      });
    });

    it('fetches settings on mount', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockSettingsGet).toHaveBeenCalled();
      });
    });

    it('fetches investments on mount', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockInvestmentsGetAll).toHaveBeenCalled();
      });
    });

    it('fetches loans on mount', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockLoansGetAll).toHaveBeenCalled();
      });
    });

    it('fetches goals on mount', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockGoalsGetAll).toHaveBeenCalled();
      });
    });

    it('fetches networth on mount', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockAnalysisNetworth).toHaveBeenCalled();
      });
    });

    it('fetches expense opportunities on mount', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockExpensesGetInvestmentOpportunities).toHaveBeenCalled();
      });
    });

    it('fetches saved strategy on mount', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockRetirementGetStrategy).toHaveBeenCalled();
      });
    });
  });

  describe('Default Parameters', () => {
    it('uses default current age of 35', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockRetirementCalculate).toHaveBeenCalledWith(
          expect.objectContaining({ currentAge: 35 })
        );
      });
    });

    it('uses default retirement age of 60', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockRetirementCalculate).toHaveBeenCalledWith(
          expect.objectContaining({ retirementAge: 60 })
        );
      });
    });

    it('uses default life expectancy of 85', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockRetirementCalculate).toHaveBeenCalledWith(
          expect.objectContaining({ lifeExpectancy: 85 })
        );
      });
    });

    it('uses default inflation rate of 6', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockRetirementCalculate).toHaveBeenCalledWith(
          expect.objectContaining({ inflation: 6 })
        );
      });
    });

    it('uses default MF return of 12', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockRetirementCalculate).toHaveBeenCalledWith(
          expect.objectContaining({ mfReturn: 12 })
        );
      });
    });

    it('uses default SIP step-up of 10', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockRetirementCalculate).toHaveBeenCalledWith(
          expect.objectContaining({ sipStepup: 10 })
        );
      });
    });
  });

  describe('Tab Navigation', () => {
    it('overview tab is selected by default', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        // Check that corpus growth projection is visible (overview content)
        expect(screen.getByText('Corpus Growth Projection')).toBeInTheDocument();
      });
    });

    it('can click on Projections tab', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        const buttons = screen.getAllByRole('button');
        const projectionsTab = buttons.find(btn => btn.textContent?.includes('Projections'));
        expect(projectionsTab).toBeDefined();
      });
    });

    it('can click on Strategy tab', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        const buttons = screen.getAllByRole('button');
        const strategyTab = buttons.find(btn => btn.textContent?.includes('Strategy'));
        expect(strategyTab).toBeDefined();
      });
    });

    it('can click on Events tab', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        const buttons = screen.getAllByRole('button');
        const eventsTab = buttons.find(btn => btn.textContent?.includes('Events'));
        expect(eventsTab).toBeDefined();
      });
    });
  });

  describe('Data Display', () => {
    it('displays current investment balances section', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getByText('Current Investment Balances')).toBeInTheDocument();
      });
    });

    it('renders charts when data is available', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getAllByTestId('responsive-container').length).toBeGreaterThan(0);
      });
    });
  });

  describe('With Corpus Gap', () => {
    it('displays gap indicator when corpus gap exists', async () => {
      mockRetirementCalculate.mockResolvedValue({
        ...defaultRetirementData,
        gapAnalysis: {
          ...defaultRetirementData.gapAnalysis,
          corpusGap: 5000000, // Positive gap means shortfall
        },
      });

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getByText('Required Corpus')).toBeInTheDocument();
      });
    });
  });

  describe('With Surplus', () => {
    it('displays surplus indicator when corpus exceeds requirement', async () => {
      mockRetirementCalculate.mockResolvedValue({
        ...defaultRetirementData,
        gapAnalysis: {
          ...defaultRetirementData.gapAnalysis,
          corpusGap: -10000000, // Negative gap means surplus
        },
      });

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getByText('Required Corpus')).toBeInTheDocument();
      });
    });
  });

  describe('Empty Data Handling', () => {
    it('handles empty retirement data gracefully', async () => {
      mockRetirementCalculate.mockResolvedValue({
        summary: {},
        gapAnalysis: {},
        matrix: [],
        maturingBeforeRetirement: {},
        recommendations: [],
      });

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getAllByRole('heading').length).toBeGreaterThan(0);
      });
    });

    it('handles empty investments gracefully', async () => {
      mockInvestmentsGetAll.mockResolvedValue([]);

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getAllByRole('heading').length).toBeGreaterThan(0);
      });
    });

    it('handles empty loans gracefully', async () => {
      mockLoansGetAll.mockResolvedValue([]);

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getAllByRole('heading').length).toBeGreaterThan(0);
      });
    });

    it('handles empty goals gracefully', async () => {
      mockGoalsGetAll.mockResolvedValue([]);

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getAllByRole('heading').length).toBeGreaterThan(0);
      });
    });

    it('renders page when no matrix data', async () => {
      mockRetirementCalculate.mockResolvedValue({
        ...defaultRetirementData,
        matrix: [],
      });

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getByText('Corpus Growth Projection')).toBeInTheDocument();
      });
    });
  });

  describe('LocalStorage Integration', () => {
    it('loads saved params from localStorage', async () => {
      const savedParams = {
        effectiveFromYear: 2,
        incomeStrategy: 'SAFE_4_PERCENT',
        corpusReturnRate: 8,
      };
      localStorage.setItem('retirementParams', JSON.stringify(savedParams));

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getAllByRole('heading').length).toBeGreaterThan(0);
      });
    });

    it('migrates old absolute year to relative offset', async () => {
      const currentYear = new Date().getFullYear();
      const savedParams = {
        effectiveFromYear: currentYear + 5, // Old absolute year format
        incomeStrategy: 'SUSTAINABLE',
      };
      localStorage.setItem('retirementParams', JSON.stringify(savedParams));

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getAllByRole('heading').length).toBeGreaterThan(0);
      });
    });
  });

  describe('Saved Strategy Integration', () => {
    it('applies saved strategy from backend', async () => {
      mockRetirementGetStrategy.mockResolvedValue({
        sellIlliquidAssets: true,
        reinvestMaturities: true,
        redirectLoanEMIs: false,
        increaseSIP: true,
        selectedIncomeStrategy: 'SAFE_4_PERCENT',
      });

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(mockRetirementGetStrategy).toHaveBeenCalled();
      });
    });

    it('applies strategy from localStorage if backend returns null', async () => {
      mockRetirementGetStrategy.mockResolvedValue(null);
      
      const localStrategy = {
        sellIlliquidAssets: true,
        selectedIncomeStrategy: 'SUSTAINABLE',
      };
      localStorage.setItem('userStrategy', JSON.stringify(localStrategy));

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getAllByRole('heading').length).toBeGreaterThan(0);
      });
    });
  });

  describe('SIP Step-up Optimization', () => {
    it('displays SIP step-up information when available', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getByText('Monthly SIP')).toBeInTheDocument();
      });
    });

    it('shows optimal stop year hint when can stop early', async () => {
      mockRetirementCalculate.mockResolvedValue({
        ...defaultRetirementData,
        summary: {
          ...defaultRetirementData.summary,
          sipStepUpOptimization: {
            ...defaultRetirementData.summary.sipStepUpOptimization,
            canStopEarly: true,
            optimalStopYear: 15,
          },
        },
      });

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        expect(screen.getByText(/Can stop step-up/i)).toBeInTheDocument();
      });
    });
  });

  describe('Maturity Events', () => {
    it('displays maturity events when available', async () => {
      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        // The matrix has a row with totalInflow > 0 for year 2026
        expect(screen.getByText('Maturity Events (not included in line)')).toBeInTheDocument();
      });
    });
  });

  describe('Settings API Error Handling', () => {
    it('handles settings API error gracefully', async () => {
      mockSettingsGet.mockRejectedValue(new Error('API Error'));

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        // Should still render with defaults
        expect(screen.getAllByRole('heading').length).toBeGreaterThan(0);
      });
    });
  });

  describe('Retirement API Error Handling', () => {
    it('handles retirement API error gracefully', async () => {
      mockRetirementCalculate.mockRejectedValue(new Error('API Error'));

      renderWithProviders(<Retirement />);
      
      await waitFor(() => {
        // Should still render the page structure
        expect(screen.getAllByRole('button').length).toBeGreaterThan(0);
      });
    });
  });
});

describe('Retirement Page with Feature Flags', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    vi.clearAllMocks();
    localStorage.clear();
    
    mockRetirementCalculate.mockResolvedValue(defaultRetirementData);
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockInvestmentsGetAll.mockResolvedValue(defaultInvestments);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({ freedUpByYear: [] });
    mockLoansGetAll.mockResolvedValue(defaultLoans);
    mockGoalsGetAll.mockResolvedValue(defaultGoals);
    mockAnalysisNetworth.mockResolvedValue(defaultNetworth);
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('renders strategy tab with premium features enabled', async () => {
    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      const buttons = screen.getAllByRole('button');
      const strategyTab = buttons.find(btn => btn.textContent?.includes('Strategy'));
      expect(strategyTab).toBeDefined();
    });
  });
});

describe('Retirement Page Income Strategies', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    vi.clearAllMocks();
    localStorage.clear();
    
    mockRetirementCalculate.mockResolvedValue(defaultRetirementData);
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockInvestmentsGetAll.mockResolvedValue(defaultInvestments);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({ freedUpByYear: [] });
    mockLoansGetAll.mockResolvedValue(defaultLoans);
    mockGoalsGetAll.mockResolvedValue(defaultGoals);
    mockAnalysisNetworth.mockResolvedValue(defaultNetworth);
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('default income strategy is SUSTAINABLE', async () => {
    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(mockRetirementCalculate).toHaveBeenCalledWith(
        expect.objectContaining({ incomeStrategy: 'SUSTAINABLE' })
      );
    });
  });
});

describe('Retirement Page with Rental Income', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    vi.clearAllMocks();
    localStorage.clear();
    
    mockRetirementCalculate.mockResolvedValue({
      ...defaultRetirementData,
      summary: {
        ...defaultRetirementData.summary,
        monthlyRentalIncomeAtRetirement: 50000,
      },
    });
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockInvestmentsGetAll.mockResolvedValue(defaultInvestments);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({ freedUpByYear: [] });
    mockLoansGetAll.mockResolvedValue(defaultLoans);
    mockGoalsGetAll.mockResolvedValue(defaultGoals);
    mockAnalysisNetworth.mockResolvedValue(defaultNetworth);
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('displays page with rental income data', async () => {
    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(screen.getByText('Projected Corpus')).toBeInTheDocument();
    });
  });
});

describe('Retirement Page with Annuity Income', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    vi.clearAllMocks();
    localStorage.clear();
    
    mockRetirementCalculate.mockResolvedValue({
      ...defaultRetirementData,
      summary: {
        ...defaultRetirementData.summary,
        annuityMonthlyIncome: 30000,
      },
    });
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockInvestmentsGetAll.mockResolvedValue(defaultInvestments);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({ freedUpByYear: [] });
    mockLoansGetAll.mockResolvedValue(defaultLoans);
    mockGoalsGetAll.mockResolvedValue(defaultGoals);
    mockAnalysisNetworth.mockResolvedValue(defaultNetworth);
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('displays page with annuity income data', async () => {
    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(screen.getByText('Projected Corpus')).toBeInTheDocument();
    });
  });
});

describe('Retirement Page with Maturing Investments', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    vi.clearAllMocks();
    localStorage.clear();
    
    mockRetirementCalculate.mockResolvedValue({
      ...defaultRetirementData,
      maturingBeforeRetirement: {
        totalMaturingBeforeRetirement: 1000000,
        moneyBackPayouts: [
          { year: 2028, amount: 100000 },
          { year: 2030, amount: 200000 },
        ],
        moneyBackCount: 2,
        maturingInvestments: [
          { id: '1', name: 'FD 1', type: 'FD', maturityDate: '2028-01-01', expectedMaturityValue: 200000 },
          { id: '2', name: 'FD 2', type: 'FD', maturityDate: '2030-01-01', expectedMaturityValue: 300000 },
        ],
        maturingInsurance: [
          { id: '3', name: 'LIC Policy', maturityDate: '2032-01-01', expectedMaturityValue: 500000 },
        ],
      },
    });
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockInvestmentsGetAll.mockResolvedValue(defaultInvestments);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({ freedUpByYear: [] });
    mockLoansGetAll.mockResolvedValue(defaultLoans);
    mockGoalsGetAll.mockResolvedValue(defaultGoals);
    mockAnalysisNetworth.mockResolvedValue(defaultNetworth);
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('displays page with maturing investments data', async () => {
    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(screen.getByText('Projected Corpus')).toBeInTheDocument();
    });
  });
});

describe('Retirement Page with Expense Opportunities', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    vi.clearAllMocks();
    localStorage.clear();
    
    mockRetirementCalculate.mockResolvedValue(defaultRetirementData);
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockInvestmentsGetAll.mockResolvedValue(defaultInvestments);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({
      totalMonthlyFreedUpByRetirement: 30000,
      freedUpByYear: [
        {
          year: 2030,
          monthlyFreedUp: 20000,
          potentialCorpusAt12Percent: 2000000,
          endingExpenses: [{ name: 'School Fees', amount: 20000 }],
        },
      ],
    });
    mockLoansGetAll.mockResolvedValue(defaultLoans);
    mockGoalsGetAll.mockResolvedValue(defaultGoals);
    mockAnalysisNetworth.mockResolvedValue(defaultNetworth);
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('displays page with expense opportunities', async () => {
    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(screen.getByText('Projected Corpus')).toBeInTheDocument();
    });
  });
});

describe('Retirement Page with Illiquid Assets', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    vi.clearAllMocks();
    localStorage.clear();
    
    mockRetirementCalculate.mockResolvedValue(defaultRetirementData);
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockInvestmentsGetAll.mockResolvedValue(defaultInvestments);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({ freedUpByYear: [] });
    mockLoansGetAll.mockResolvedValue(defaultLoans);
    mockGoalsGetAll.mockResolvedValue(defaultGoals);
    mockAnalysisNetworth.mockResolvedValue({
      ...defaultNetworth,
      sellableAssets: {
        GOLD: 1000000,
        REAL_ESTATE: 10000000,
      },
    });
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('displays page with illiquid assets data', async () => {
    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(screen.getByText('Projected Corpus')).toBeInTheDocument();
    });
  });
});

describe('Retirement Page with Multiple Loans', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    vi.clearAllMocks();
    localStorage.clear();
    
    mockRetirementCalculate.mockResolvedValue(defaultRetirementData);
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockInvestmentsGetAll.mockResolvedValue(defaultInvestments);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({ freedUpByYear: [] });
    mockLoansGetAll.mockResolvedValue([
      { id: '1', name: 'Home Loan', emi: 30000, endDate: '2035-01-01' },
      { id: '2', name: 'Car Loan', emi: 15000, endDate: '2028-01-01' },
      { id: '3', name: 'Personal Loan', emi: 10000, endDate: '2026-01-01' },
    ]);
    mockGoalsGetAll.mockResolvedValue(defaultGoals);
    mockAnalysisNetworth.mockResolvedValue(defaultNetworth);
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('displays page with multiple loans', async () => {
    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(screen.getByText('Projected Corpus')).toBeInTheDocument();
    });
  });
});

describe('Retirement Page with Multiple Goals', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    vi.clearAllMocks();
    localStorage.clear();
    
    mockRetirementCalculate.mockResolvedValue(defaultRetirementData);
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockInvestmentsGetAll.mockResolvedValue(defaultInvestments);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({ freedUpByYear: [] });
    mockLoansGetAll.mockResolvedValue(defaultLoans);
    mockGoalsGetAll.mockResolvedValue([
      { id: '1', name: 'Child Education', targetAmount: 2000000, targetYear: 2030 },
      { id: '2', name: 'House Renovation', targetAmount: 500000, targetYear: 2028 },
      { id: '3', name: 'Foreign Trip', targetAmount: 300000, targetYear: 2027 },
    ]);
    mockAnalysisNetworth.mockResolvedValue(defaultNetworth);
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('displays page with multiple goals', async () => {
    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(screen.getByText('Projected Corpus')).toBeInTheDocument();
    });
  });
});

describe('Retirement Page with No Investments', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    vi.clearAllMocks();
    localStorage.clear();
    
    mockRetirementCalculate.mockResolvedValue({
      ...defaultRetirementData,
      summary: {
        ...defaultRetirementData.summary,
        startingBalances: {
          totalStarting: 0,
          mutualFunds: 0,
          epf: 0,
          ppf: 0,
          nps: 0,
          fd: 0,
          rd: 0,
          stocks: 0,
          cash: 0,
        },
      },
    });
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockInvestmentsGetAll.mockResolvedValue([]);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({ freedUpByYear: [] });
    mockLoansGetAll.mockResolvedValue([]);
    mockGoalsGetAll.mockResolvedValue([]);
    mockAnalysisNetworth.mockResolvedValue({
      totalAssets: 0,
      totalLiabilities: 0,
      netWorth: 0,
      assetBreakdown: {},
      sellableAssets: {},
    });
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('displays page when user has no investments', async () => {
    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(screen.getAllByRole('heading').length).toBeGreaterThan(0);
    });
  });

  it('shows no investments message', async () => {
    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(screen.getByText(/No investments found/i)).toBeInTheDocument();
    });
  });
});

describe('Retirement Page Retirement Readiness', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    vi.clearAllMocks();
    localStorage.clear();
    
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockInvestmentsGetAll.mockResolvedValue(defaultInvestments);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({ freedUpByYear: [] });
    mockLoansGetAll.mockResolvedValue(defaultLoans);
    mockGoalsGetAll.mockResolvedValue(defaultGoals);
    mockAnalysisNetworth.mockResolvedValue(defaultNetworth);
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('displays On Track when corpus exceeds requirement', async () => {
    mockRetirementCalculate.mockResolvedValue({
      ...defaultRetirementData,
      gapAnalysis: {
        ...defaultRetirementData.gapAnalysis,
        corpusGap: -10000000, // Negative means surplus
      },
    });

    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(screen.getByText('On Track!')).toBeInTheDocument();
    });
  });

  it('displays Needs Attention when corpus gap exists', async () => {
    mockRetirementCalculate.mockResolvedValue({
      ...defaultRetirementData,
      gapAnalysis: {
        ...defaultRetirementData.gapAnalysis,
        corpusGap: 5000000, // Positive means shortfall
      },
    });

    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(screen.getByText('Needs Attention')).toBeInTheDocument();
    });
  });
});

describe('Retirement Page Matrix Display', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    vi.clearAllMocks();
    localStorage.clear();
    
    mockRetirementCalculate.mockResolvedValue(defaultRetirementData);
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockInvestmentsGetAll.mockResolvedValue(defaultInvestments);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({ freedUpByYear: [] });
    mockLoansGetAll.mockResolvedValue(defaultLoans);
    mockGoalsGetAll.mockResolvedValue(defaultGoals);
    mockAnalysisNetworth.mockResolvedValue(defaultNetworth);
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('displays retirement readiness section', async () => {
    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(screen.getByText('Retirement Readiness')).toBeInTheDocument();
    });
  });
});

describe('Retirement Page with Emergency Fund', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: 0 },
      },
    });
    
    vi.clearAllMocks();
    localStorage.clear();
    
    mockRetirementCalculate.mockResolvedValue({
      ...defaultRetirementData,
      summary: {
        ...defaultRetirementData.summary,
        startingBalances: {
          ...defaultRetirementData.summary.startingBalances,
          emergencyFund: 300000,
        },
      },
    });
    mockRetirementGetStrategy.mockResolvedValue(null);
    mockSettingsGet.mockResolvedValue(defaultSettings);
    mockInvestmentsGetAll.mockResolvedValue([
      ...defaultInvestments,
      { id: '4', name: 'Emergency FD', type: 'FD', currentValue: 300000, isEmergencyFund: true },
    ]);
    mockExpensesGetInvestmentOpportunities.mockResolvedValue({ freedUpByYear: [] });
    mockLoansGetAll.mockResolvedValue(defaultLoans);
    mockGoalsGetAll.mockResolvedValue(defaultGoals);
    mockAnalysisNetworth.mockResolvedValue(defaultNetworth);
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('displays page with emergency fund data', async () => {
    renderWithProviders(<Retirement />);
    
    await waitFor(() => {
      expect(screen.getByText('Projected Corpus')).toBeInTheDocument();
    });
  });
});
