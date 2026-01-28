import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Settings } from './Settings';

// Mock the settings API
vi.mock('../lib/api', () => ({
  settingsApi: {
    get: vi.fn().mockResolvedValue({
      currentAge: 35,
      retirementAge: 60,
      lifeExpectancy: 85,
      inflationRate: 6.0,
      epfReturn: 8.15,
      ppfReturn: 7.1,
      mfEquityReturn: 12.0,
      mfDebtReturn: 7.0,
      npsReturn: 10.0,
      fdReturn: 6.5,
      emergencyFundMonths: 6,
      sipStepup: 10,
    }),
    update: vi.fn().mockResolvedValue({}),
  },
}));

// Mock auth store
vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
    features: { incomePage: true },
    refreshFeatures: vi.fn(),
  }),
}));

describe('Settings', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
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

  it('renders settings page title', () => {
    renderWithProviders(<Settings />);
    const headings = screen.getAllByRole('heading');
    expect(headings.length).toBeGreaterThan(0);
  });

  it('renders age and timeline section', () => {
    renderWithProviders(<Settings />);
    expect(screen.getByText('Age & Timeline')).toBeInTheDocument();
  });

  it('renders expected returns section', () => {
    renderWithProviders(<Settings />);
    expect(screen.getByText('Expected Returns (%)')).toBeInTheDocument();
  });

  it('renders inflation section', () => {
    renderWithProviders(<Settings />);
    expect(screen.getByText('Inflation & Safety')).toBeInTheDocument();
  });

  it('renders save and reset buttons', () => {
    renderWithProviders(<Settings />);
    expect(screen.getByText('Save Settings')).toBeInTheDocument();
    expect(screen.getByText('Reset to Defaults')).toBeInTheDocument();
  });
});
