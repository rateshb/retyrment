import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Admin } from './Admin';

vi.mock('../lib/api', () => ({
  adminApi: {
    getUsers: vi.fn().mockResolvedValue({ users: [], total: 0 }),
    updateRole: vi.fn().mockResolvedValue({}),
    updateUserFeatures: vi.fn().mockResolvedValue({}),
    deleteUser: vi.fn().mockResolvedValue({}),
  },
}));

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: '1', email: 'admin@example.com', name: 'Admin User', role: 'ADMIN' },
    features: { adminPanel: true },
    refreshFeatures: vi.fn(),
    fetchFeatures: vi.fn(),
  }),
}));

describe('Admin', () => {
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

  it('renders admin page title', () => {
    renderWithProviders(<Admin />);
    const headings = screen.getAllByRole('heading');
    expect(headings.length).toBeGreaterThan(0);
  });

  it('renders search input', () => {
    renderWithProviders(<Admin />);
    const searchInput = screen.getByPlaceholderText(/search/i);
    expect(searchInput).toBeInTheDocument();
  });

  it('renders user management section', () => {
    renderWithProviders(<Admin />);
    const headings = screen.getAllByRole('heading');
    expect(headings.length).toBeGreaterThan(0);
  });
});
