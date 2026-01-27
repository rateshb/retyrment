import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Family } from './Family';

vi.mock('../lib/api', () => ({
  familyApi: {
    getAll: vi.fn().mockResolvedValue([]),
    create: vi.fn().mockResolvedValue({}),
    update: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
    getSelf: vi.fn().mockResolvedValue(null),
    getSpouse: vi.fn().mockResolvedValue(null),
  },
}));

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
    features: { familyPage: true },
    refreshFeatures: vi.fn(),
  }),
}));

describe('Family', () => {
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

  it('renders family page title', () => {
    renderWithProviders(<Family />);
    const headings = screen.getAllByRole('heading');
    expect(headings.length).toBeGreaterThan(0);
  });

  it('renders add member button', () => {
    renderWithProviders(<Family />);
    expect(screen.getByRole('button', { name: /add member/i })).toBeInTheDocument();
  });
});
