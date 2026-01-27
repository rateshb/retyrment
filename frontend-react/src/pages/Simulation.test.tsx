import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Simulation } from './Simulation';

// Mock the simulation API
vi.mock('../lib/api', () => ({
  simulationApi: {
    run: vi.fn().mockResolvedValue({
      percentiles: { p10: 5000000, p25: 7500000, p50: 10000000, p75: 12500000, p90: 15000000 },
      average: 10000000,
      successRate: 85,
      simulations: 1000,
      years: 25,
    }),
  },
}));

// Mock auth store
vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
    features: { 
      canRunSimulation: true,
    },
    refreshFeatures: vi.fn(),
  }),
}));

describe('Simulation', () => {
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

  it('renders simulation page title', () => {
    renderWithProviders(<Simulation />);
    const headings = screen.getAllByRole('heading');
    expect(headings.length).toBeGreaterThan(0);
  });

  it('renders simulation parameters section', () => {
    renderWithProviders(<Simulation />);
    expect(screen.getByText('Simulation Parameters')).toBeInTheDocument();
  });

  it('renders run simulation button', () => {
    renderWithProviders(<Simulation />);
    expect(screen.getByRole('button', { name: /run simulation/i })).toBeInTheDocument();
  });
});
