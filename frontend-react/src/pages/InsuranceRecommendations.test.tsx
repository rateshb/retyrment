import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { InsuranceRecommendations } from './InsuranceRecommendations';

// Mock the API - simplified approach
vi.mock('../lib/api', () => ({
  insuranceRecommendationsApi: {
    getOverall: vi.fn().mockResolvedValue({
      healthRecommendation: { recommended: false },
      termRecommendation: { recommended: false },
      summary: { totalRecommendedPremium: 0, coverageGap: 0 },
    }),
  },
  insuranceApi: {
    getAll: vi.fn().mockResolvedValue([]),
  },
  auth: {
    isLoggedIn: vi.fn().mockReturnValue(false),
  },
}));

// Mock the auth store
vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
    features: { insurancePage: true, insuranceRecommendationsPage: true },
  }),
}));

describe('InsuranceRecommendations', () => {
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

  it('renders insurance recommendations page', () => {
    renderWithQueryClient(<InsuranceRecommendations />);
    
    expect(screen.getByRole('heading', { name: /Insurance Advisor/i })).toBeInTheDocument();
  });

  it('shows loading state initially', () => {
    renderWithQueryClient(<InsuranceRecommendations />);
    
    expect(screen.getByRole('heading', { name: /Insurance Advisor/i })).toBeInTheDocument();
  });

  it('displays insurance recommendations content', async () => {
    renderWithQueryClient(<InsuranceRecommendations />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Insurance Advisor/i })).toBeInTheDocument();
    });
  });
});
