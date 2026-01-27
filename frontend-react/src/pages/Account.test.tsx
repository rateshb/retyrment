import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Account } from './Account';

vi.mock('../lib/api', () => ({
  userDataApi: {
    summary: vi.fn().mockResolvedValue({ totalRecords: 50 }),
    deleteAll: vi.fn().mockResolvedValue({}),
  },
  exportApi: {
    json: vi.fn().mockResolvedValue({ data: {} }),
    pdf: vi.fn().mockResolvedValue(new Blob()),
    excel: vi.fn().mockResolvedValue(new Blob()),
  },
}));

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE', picture: null },
    features: { incomePage: true },
    refreshFeatures: vi.fn(),
    logout: vi.fn(),
  }),
}));

describe('Account', () => {
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

  it('renders account page title', () => {
    renderWithProviders(<Account />);
    const headings = screen.getAllByRole('heading');
    expect(headings.length).toBeGreaterThan(0);
  });

  it('renders user name', () => {
    renderWithProviders(<Account />);
    const userNames = screen.getAllByText('Test User');
    expect(userNames.length).toBeGreaterThan(0);
  });

  it('renders user email', () => {
    renderWithProviders(<Account />);
    const emails = screen.getAllByText('test@example.com');
    expect(emails.length).toBeGreaterThan(0);
  });
});
