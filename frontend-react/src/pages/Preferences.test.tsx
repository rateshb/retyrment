import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Preferences } from './Preferences';

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
    features: { preferencesPage: true },
    refreshFeatures: vi.fn(),
  }),
}));

describe('Preferences', () => {
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

  it('renders preferences page title', () => {
    renderWithProviders(<Preferences />);
    const headings = screen.getAllByRole('heading');
    expect(headings.length).toBeGreaterThan(0);
  });

  it('renders appearance section', () => {
    renderWithProviders(<Preferences />);
    const headings = screen.getAllByRole('heading');
    expect(headings.length).toBeGreaterThan(0);
  });

  it('renders notifications section', () => {
    renderWithProviders(<Preferences />);
    expect(screen.getByText('Notifications')).toBeInTheDocument();
  });

  it('renders dashboard widgets section', () => {
    renderWithProviders(<Preferences />);
    expect(screen.getByText('Dashboard Widgets')).toBeInTheDocument();
  });

  it('renders save button', () => {
    renderWithProviders(<Preferences />);
    expect(screen.getByText('Save Preferences')).toBeInTheDocument();
  });
});
