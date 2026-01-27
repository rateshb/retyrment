import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Expenses } from './Expenses';

// Mock the expenses API
vi.mock('../lib/api', () => ({
  expensesApi: {
    getAll: vi.fn().mockResolvedValue([]),
    create: vi.fn().mockResolvedValue({}),
    update: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  },
  Expense: {},
}));

// Mock auth store
vi.mock('../stores/authStore', () => ({
  useAuthStore: vi.fn(() => ({
    user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
    features: { expensePage: true },
    refreshFeatures: vi.fn(),
  })),
}));

import { expensesApi } from '../lib/api';

describe('Expenses', () => {
  let queryClient: QueryClient;

  const mockExpense = {
    id: 'exp-1',
    name: 'Monthly Rent',
    category: 'RENT',
    amount: 50000,
    frequency: 'MONTHLY',
    isEssential: true,
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    vi.clearAllMocks();
    
    (expensesApi.getAll as any).mockResolvedValue([]);
    (expensesApi.create as any).mockResolvedValue(mockExpense);
    (expensesApi.update as any).mockResolvedValue(mockExpense);
    (expensesApi.delete as any).mockResolvedValue({});
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

  describe('Page Rendering', () => {
    it('renders expenses page title', async () => {
      renderWithProviders(<Expenses />);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /expenses/i })).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('renders add expense button', async () => {
      renderWithProviders(<Expenses />);
      
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add expense/i })).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('displays empty state when no expenses', async () => {
      renderWithProviders(<Expenses />);
      
      await waitFor(() => {
        expect(screen.getByText(/no expenses yet/i)).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('displays expenses when data available', async () => {
      (expensesApi.getAll as any).mockResolvedValue([mockExpense]);
      
      renderWithProviders(<Expenses />);
      
      await waitFor(() => {
        expect(screen.getByText('Monthly Rent')).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('displays multiple expenses', async () => {
      (expensesApi.getAll as any).mockResolvedValue([
        mockExpense,
        { ...mockExpense, id: 'exp-2', name: 'Groceries', category: 'GROCERIES', amount: 15000 },
        { ...mockExpense, id: 'exp-3', name: 'Utilities', category: 'UTILITIES', amount: 5000 },
      ]);
      
      renderWithProviders(<Expenses />);
      
      await waitFor(() => {
        expect(screen.getByText('Monthly Rent')).toBeInTheDocument();
        expect(screen.getAllByText('Groceries')).toHaveLength(2);
        expect(screen.getAllByText('Utilities')).toHaveLength(2);
      }, { timeout: 3000 });
    });
  });

  describe('Modal Interactions', () => {
    it('opens add modal when add button clicked', async () => {
      renderWithProviders(<Expenses />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add expense/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add expense/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add expense/i })).toBeInTheDocument();
      });
    });

    it('closes modal when cancel button clicked', async () => {
      renderWithProviders(<Expenses />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add expense/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add expense/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add expense/i })).toBeInTheDocument();
      });
      
      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      fireEvent.click(cancelButton);
      
      await waitFor(() => {
        expect(screen.queryByRole('heading', { name: /add expense/i })).not.toBeInTheDocument();
      });
    });

    it('opens edit modal when edit button clicked', async () => {
      (expensesApi.getAll as any).mockResolvedValue([mockExpense]);
      
      renderWithProviders(<Expenses />);
      
      await waitFor(() => {
        expect(screen.getByText('Monthly Rent')).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const editButton = screen.getByRole('button', { name: /edit expense/i });
      fireEvent.click(editButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /edit expense/i })).toBeInTheDocument();
      });
    });
  });

  describe('Form Validation', () => {
    it('validates required fields on submit', async () => {
      renderWithProviders(<Expenses />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add expense/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add expense/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add expense/i })).toBeInTheDocument();
      });
      
      // Clear name field (default might be empty already)
      const nameInput = screen.getByLabelText(/name/i);
      fireEvent.change(nameInput, { target: { value: '' } });
      
      const saveButtons = screen.getAllByRole('button', { name: /add expense/i });
      const saveButton = saveButtons[saveButtons.length - 1];
      fireEvent.click(saveButton);
      
      await waitFor(() => {
        expect(screen.getByText(/name is required/i)).toBeInTheDocument();
      });
    });

    it('validates category field', async () => {
      renderWithProviders(<Expenses />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add expense/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add expense/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add expense/i })).toBeInTheDocument();
      });
      
      const nameInput = screen.getByLabelText(/name/i);
      fireEvent.change(nameInput, { target: { value: 'Test Expense' } });
      
      const saveButtons = screen.getAllByRole('button', { name: /add expense/i });
      const saveButton = saveButtons[saveButtons.length - 1];
      fireEvent.click(saveButton);
      
      await waitFor(() => {
        expect(screen.getByText(/category is required/i)).toBeInTheDocument();
      });
    });

    it('validates amount field', async () => {
      renderWithProviders(<Expenses />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add expense/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add expense/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add expense/i })).toBeInTheDocument();
      });
      
      const nameInput = screen.getByLabelText(/name/i);
      fireEvent.change(nameInput, { target: { value: 'Test Expense' } });
      
      const categorySelect = screen.getByLabelText(/category/i);
      fireEvent.change(categorySelect, { target: { value: 'RENT' } });
      
      const saveButtons = screen.getAllByRole('button', { name: /add expense/i });
      const saveButton = saveButtons[saveButtons.length - 1];
      fireEvent.click(saveButton);
      
      await waitFor(() => {
        expect(screen.getByText(/enter a valid amount/i)).toBeInTheDocument();
      });
    });
  });

  describe('CRUD Operations', () => {
    it('creates new expense successfully', async () => {
      renderWithProviders(<Expenses />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add expense/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add expense/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add expense/i })).toBeInTheDocument();
      });
      
      const nameInput = screen.getByLabelText(/name/i);
      fireEvent.change(nameInput, { target: { value: 'Monthly Rent' } });
      
      const categorySelect = screen.getByLabelText(/category/i);
      fireEvent.change(categorySelect, { target: { value: 'RENT' } });
      
      const amountInput = screen.getByLabelText(/amount/i);
      fireEvent.change(amountInput, { target: { value: '50000' } });
      
      const saveButtons = screen.getAllByRole('button', { name: /add expense/i });
      const saveButton = saveButtons[saveButtons.length - 1];
      fireEvent.click(saveButton);
      
      await waitFor(() => {
        expect(expensesApi.create).toHaveBeenCalled();
      }, { timeout: 3000 });
    });

    it('updates existing expense', async () => {
      (expensesApi.getAll as any).mockResolvedValue([mockExpense]);
      
      renderWithProviders(<Expenses />);
      
      await waitFor(() => {
        expect(screen.getByText('Monthly Rent')).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const editButton = screen.getByRole('button', { name: /edit expense/i });
      fireEvent.click(editButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /edit expense/i })).toBeInTheDocument();
      });
      
      const amountInput = screen.getByLabelText(/amount/i);
      fireEvent.change(amountInput, { target: { value: '55000' } });
      
      const saveButton = screen.getByRole('button', { name: /save changes/i });
      fireEvent.click(saveButton);
      
      await waitFor(() => {
        expect(expensesApi.update).toHaveBeenCalledWith('exp-1', expect.any(Object));
      });
    });

    it('deletes expense', async () => {
      (expensesApi.getAll as any).mockResolvedValue([mockExpense]);
      global.confirm = vi.fn(() => true);
      
      renderWithProviders(<Expenses />);
      
      await waitFor(() => {
        expect(screen.getByText('Monthly Rent')).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const deleteButton = screen.getByRole('button', { name: /delete expense/i });
      fireEvent.click(deleteButton);
      
      await waitFor(() => {
        expect(expensesApi.delete).toHaveBeenCalledWith('exp-1');
      });
    });

    it('cancels delete when user declines confirmation', async () => {
      (expensesApi.getAll as any).mockResolvedValue([mockExpense]);
      global.confirm = vi.fn(() => false);
      
      renderWithProviders(<Expenses />);
      
      await waitFor(() => {
        expect(screen.getByText('Monthly Rent')).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const deleteButton = screen.getByRole('button', { name: /delete expense/i });
      fireEvent.click(deleteButton);
      expect(expensesApi.delete).not.toHaveBeenCalled();
    });
  });

  describe('Error Handling', () => {
    it('handles API error on fetch', async () => {
      (expensesApi.getAll as any).mockRejectedValueOnce(new Error('Failed to fetch'));
      
      renderWithProviders(<Expenses />);
      
      // Even with error, page should still render with add button
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add expense/i })).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('handles API error on create', async () => {
      (expensesApi.create as any).mockRejectedValueOnce(new Error('Failed to create'));
      
      renderWithProviders(<Expenses />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add expense/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add expense/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add expense/i })).toBeInTheDocument();
      });
      
      const nameInput = screen.getByLabelText(/name/i);
      fireEvent.change(nameInput, { target: { value: 'Test Expense' } });
      
      const categorySelect = screen.getByLabelText(/category/i);
      fireEvent.change(categorySelect, { target: { value: 'GROCERIES' } });
      
      const amountInput = screen.getByLabelText(/amount/i);
      fireEvent.change(amountInput, { target: { value: '5000' } });
      
      const saveButtons = screen.getAllByRole('button', { name: /add expense/i });
      const saveButton = saveButtons[saveButtons.length - 1];
      fireEvent.click(saveButton);
      
      await waitFor(() => {
        expect(expensesApi.create).toHaveBeenCalled();
      }, { timeout: 3000 });
    });
  });

  describe('Different Expense Types', () => {
    it('handles essential expenses', async () => {
      (expensesApi.getAll as any).mockResolvedValue([
        { ...mockExpense, isEssential: true }
      ]);
      
      renderWithProviders(<Expenses />);
      
      await waitFor(() => {
        expect(screen.getByText('Monthly Rent')).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('handles non-essential expenses', async () => {
      (expensesApi.getAll as any).mockResolvedValue([
        { ...mockExpense, isEssential: false, name: 'Entertainment', category: 'ENTERTAINMENT' }
      ]);
      
      renderWithProviders(<Expenses />);
      
      await waitFor(() => {
        expect(screen.getAllByText('Entertainment')).toHaveLength(2);
      }, { timeout: 3000 });
    });

    it('handles different frequencies', async () => {
      (expensesApi.getAll as any).mockResolvedValue([
        mockExpense,
        { ...mockExpense, id: 'exp-2', name: 'Quarterly Tax', frequency: 'QUARTERLY' },
        { ...mockExpense, id: 'exp-3', name: 'Annual Insurance', frequency: 'YEARLY' },
      ]);
      
      renderWithProviders(<Expenses />);
      
      await waitFor(() => {
        expect(screen.getByText('Monthly Rent')).toBeInTheDocument();
        expect(screen.getByText('Quarterly Tax')).toBeInTheDocument();
        expect(screen.getByText('Annual Insurance')).toBeInTheDocument();
      }, { timeout: 3000 });
    });
  });

  describe('Form Default Values', () => {
    it('sets default frequency to MONTHLY for new expense', async () => {
      renderWithProviders(<Expenses />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add expense/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add expense/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add expense/i })).toBeInTheDocument();
      });
      
      const frequencySelect = screen.getByLabelText(/frequency/i) as HTMLSelectElement;
      expect(frequencySelect.value).toBe('MONTHLY');
    });
  });
});
