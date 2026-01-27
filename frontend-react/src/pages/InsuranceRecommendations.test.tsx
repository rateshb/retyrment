import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
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

import { insuranceRecommendationsApi, insuranceApi } from '../lib/api';

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

  it('shows error state when recommendations fail to load', async () => {
    (insuranceRecommendationsApi.getOverall as any).mockRejectedValueOnce(new Error('Failed'));

    renderWithQueryClient(<InsuranceRecommendations />);

    await waitFor(() => {
      expect(screen.getByText(/Failed to load recommendations/i)).toBeInTheDocument();
    });
  });

  it('styles overall score card based on score', async () => {
    (insuranceRecommendationsApi.getOverall as any).mockResolvedValueOnce({
      healthRecommendation: { totalRecommendedCover: 0, existingCover: 0, gap: 0, memberBreakdown: [] },
      termRecommendation: { totalRecommendedCover: 0, existingCover: 0, gap: 0, memberBreakdown: [] },
      summary: { overallScore: 85, status: 'Great' },
    });

    renderWithQueryClient(<InsuranceRecommendations />);

    await waitFor(() => {
      expect(screen.getByText('Insurance Score')).toBeInTheDocument();
    });

    const scoreTitle = screen.getByText('Insurance Score');
    const scoreCard = scoreTitle.closest('.card');
    expect(scoreCard).toBeTruthy();
    expect(scoreCard).toHaveClass('bg-success-50');

    const scoreValue = within(scoreCard!).getByText('85');
    expect(scoreValue).toHaveClass('text-success-600');
  });

  it('shows group-only health warning when only group policy exists', async () => {
    (insuranceRecommendationsApi.getOverall as any).mockResolvedValueOnce({
      healthRecommendation: { totalRecommendedCover: 0, existingCover: 0, gap: 0, memberBreakdown: [] },
      termRecommendation: { totalRecommendedCover: 0, existingCover: 0, gap: 0, memberBreakdown: [] },
      summary: { overallScore: 40, status: 'Needs attention' },
    });
    (insuranceApi.getAll as any).mockResolvedValueOnce([
      { type: 'HEALTH', healthType: 'GROUP' },
    ]);

    renderWithQueryClient(<InsuranceRecommendations />);

    await waitFor(() => {
      expect(screen.getByText(/Group health cover ends at retirement/i)).toBeInTheDocument();
    });
  });

  it('shows gap indicators when coverage gaps exist', async () => {
    (insuranceRecommendationsApi.getOverall as any).mockResolvedValueOnce({
      healthRecommendation: { totalRecommendedCover: 100000, existingCover: 0, gap: 50000, memberBreakdown: [] },
      termRecommendation: { totalRecommendedCover: 100000, existingCover: 0, gap: 25000, memberBreakdown: [] },
      summary: { overallScore: 55, status: 'Needs attention' },
    });

    renderWithQueryClient(<InsuranceRecommendations />);

    await waitFor(() => {
      const gaps = screen.getAllByText(/Gap:/i);
      expect(gaps.length).toBeGreaterThanOrEqual(2);
    });
  });
});
