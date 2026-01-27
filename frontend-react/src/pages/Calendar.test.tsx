import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Calendar } from './Calendar';

vi.mock('../lib/api', () => ({
  insuranceApi: { getAll: vi.fn().mockResolvedValue([]) },
  loansApi: { getAll: vi.fn().mockResolvedValue([]) },
  goalsApi: { getAll: vi.fn().mockResolvedValue([]) },
  investmentsApi: { getAll: vi.fn().mockResolvedValue([]) },
}));

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
    features: { calendarPage: true },
    refreshFeatures: vi.fn(),
  }),
}));

describe('Calendar', () => {
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

  it('renders calendar page title', () => {
    renderWithProviders(<Calendar />);
    const headings = screen.getAllByRole('heading');
    expect(headings.length).toBeGreaterThan(0);
  });

  it('renders view mode buttons', () => {
    renderWithProviders(<Calendar />);
    const buttons = screen.getAllByRole('button');
    expect(buttons.length).toBeGreaterThan(0);
  });
});
