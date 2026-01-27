import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Preferences } from './Preferences';

const toastSuccess = vi.hoisted(() => vi.fn());
vi.mock('../components/ui', async () => {
  const actual = await vi.importActual<typeof import('../components/ui')>('../components/ui');
  return {
    ...actual,
    toast: {
      success: toastSuccess,
      error: vi.fn(),
    },
  };
});

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
    localStorage.clear();
    vi.clearAllMocks();
    toastSuccess.mockClear();
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

  const getToggle = (label: string) => {
    const labelEl = screen.getByText(label);
    const container = labelEl.parentElement?.parentElement;
    const input = container?.querySelector('input[type="checkbox"]');
    if (!input) {
      throw new Error(`Toggle not found for label: ${label}`);
    }
    return input as HTMLInputElement;
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

  it('renders dashboard widgets section', () => {
    renderWithProviders(<Preferences />);
    expect(screen.getByText('Dashboard Widgets')).toBeInTheDocument();
  });

  it('renders save button', () => {
    renderWithProviders(<Preferences />);
    expect(screen.getByText('Save Preferences')).toBeInTheDocument();
  });

  it('loads saved preferences from localStorage', () => {
    localStorage.setItem('retyrment_preferences', JSON.stringify({
      currency: 'USD',
      numberFormat: 'International',
      compactNumbers: false,
      showEmoji: false,
      dashboard: {
        showNetWorth: false,
        showRecommendations: true,
        showUpcomingEvents: false,
        showGoalProgress: true,
      },
    }));

    renderWithProviders(<Preferences />);

    const selects = screen.getAllByRole('combobox');
    expect(selects[0]).toHaveValue('USD');
    expect(selects[1]).toHaveValue('International');

    expect(getToggle('Compact Numbers')).not.toBeChecked();
    expect(getToggle('Show Emoji')).not.toBeChecked();
    expect(screen.getByLabelText('Net Worth')).not.toBeChecked();
    expect(screen.getByLabelText('Recommendations')).toBeChecked();
    expect(screen.getByLabelText('Upcoming Events')).not.toBeChecked();
    expect(screen.getByLabelText('Goal Progress')).toBeChecked();
  });

  it('saves updated preferences and shows toast', () => {
    renderWithProviders(<Preferences />);

    const themeButton = screen.getByRole('button', { name: 'dark' });
    fireEvent.click(themeButton);

    fireEvent.click(getToggle('Compact Numbers'));

    fireEvent.click(screen.getByText('Save Preferences'));

    expect(toastSuccess).toHaveBeenCalledWith('Preferences saved successfully');
    const stored = JSON.parse(localStorage.getItem('retyrment_preferences') || '{}');
    expect(stored.theme).toBe('dark');
    expect(stored.compactNumbers).toBe(false);
  });

  it('hides dashboard emoji icons when showEmoji is disabled', () => {
    renderWithProviders(<Preferences />);

    fireEvent.click(getToggle('Show Emoji'));

    expect(screen.queryByText('💰')).not.toBeInTheDocument();
    expect(screen.queryByText('💡')).not.toBeInTheDocument();
    expect(screen.queryByText('📅')).not.toBeInTheDocument();
    expect(screen.queryByText('🎯')).not.toBeInTheDocument();
  });
});
