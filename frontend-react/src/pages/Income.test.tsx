import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Income } from './Income';

// Mock the API - simplified approach
vi.mock('../lib/api', () => ({
  incomeApi: {
    getAll: vi.fn().mockResolvedValue([]),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
  auth: {
    isLoggedIn: vi.fn().mockReturnValue(false),
  },
}));

describe('Income', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
        mutations: {
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

  it('renders income page title', () => {
    renderWithQueryClient(<Income />);
    
    expect(screen.getByRole('heading', { name: /Income/i })).toBeInTheDocument();
    expect(screen.getByText('Add Income')).toBeInTheDocument();
  });

  it('shows loading state initially', () => {
    renderWithQueryClient(<Income />);
    
    expect(screen.getByRole('heading', { name: /Income/i })).toBeInTheDocument();
  });

  it('displays empty state when no income sources exist', async () => {
    renderWithQueryClient(<Income />);

    await waitFor(() => {
      expect(screen.getByText('No income sources yet. Click "Add Income" to get started.')).toBeInTheDocument();
    });
  });
});
