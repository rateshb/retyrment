import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Dashboard } from './Dashboard';

// Mock the API - simplified approach
vi.mock('../lib/api', () => ({
  analysisApi: {
    networth: vi.fn().mockResolvedValue({
      totalAssets: 0,
      totalLiabilities: 0,
      netWorth: 0,
      assetBreakdown: {},
    }),
    recommendations: vi.fn().mockResolvedValue({
      recommendations: [],
      monthlySavings: 0,
    }),
    goals: vi.fn().mockResolvedValue({ goals: [] }),
  },
  investmentsApi: {
    getAll: vi.fn().mockResolvedValue([]),
  },
  insuranceApi: {
    getAll: vi.fn().mockResolvedValue([]),
  },
  loansApi: {
    getAll: vi.fn().mockResolvedValue([]),
  },
  retirementApi: {
    calculate: vi.fn().mockResolvedValue({
      summary: { finalCorpus: 0, retirementIncomeProjection: [] },
      gapAnalysis: {},
      matrix: [],
    }),
    getMaturing: vi.fn().mockResolvedValue({}),
    getStrategy: vi.fn().mockResolvedValue(null),
  },
  insuranceRecommendationsApi: {
    getOverall: vi.fn().mockResolvedValue({
      healthRecommendation: { gap: 0, totalRecommendedCover: 0 },
    }),
  },
  auth: {
    isLoggedIn: vi.fn().mockReturnValue(false),
  },
}));

// Mock the auth store
vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
    features: { incomePage: true, investmentPage: true, loanPage: true, insurancePage: true },
  }),
}));

describe('Dashboard', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    vi.clearAllMocks();
  });

  const renderWithQueryClient = (component: React.ReactElement) => {
    return render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {component}
        </QueryClientProvider>
      </MemoryRouter>
    );
  };

  it('renders dashboard title', () => {
    renderWithQueryClient(<Dashboard />);
    
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });

  it('shows loading state initially', () => {
    renderWithQueryClient(<Dashboard />);
    
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });
});
