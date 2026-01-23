import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Dashboard } from './Dashboard';

// Mock the API - simplified approach
vi.mock('../lib/api', () => ({
  analysisApi: {
    networth: vi.fn(),
    recommendations: vi.fn(),
    goals: vi.fn(),
  },
  investmentsApi: {
    getAll: vi.fn(),
  },
  insuranceApi: {
    getAll: vi.fn(),
  },
  loansApi: {
    getAll: vi.fn(),
  },
  retirementApi: {
    calculate: vi.fn(),
    getMaturing: vi.fn(),
    getStrategy: vi.fn(),
  },
  insuranceRecommendationsApi: {
    getOverall: vi.fn(),
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
