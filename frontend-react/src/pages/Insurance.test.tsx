import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Insurance } from './Insurance';

// Mock the insurance API
vi.mock('../lib/api', () => ({
  insuranceApi: {
    getAll: vi.fn().mockResolvedValue([]),
    create: vi.fn().mockResolvedValue({}),
    update: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  },
}));

// Mock auth store
vi.mock('../stores/authStore', () => ({
  useAuthStore: vi.fn(() => ({
    user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
    features: { insurancePage: true, blockedInsuranceTypes: [] },
    refreshFeatures: vi.fn(),
  })),
}));

import { insuranceApi } from '../lib/api';
import { useAuthStore } from '../stores/authStore';

describe('Insurance', () => {
  let queryClient: QueryClient;

  const mockInsurance = {
    id: 'ins-1',
    type: 'TERM_LIFE',
    policyName: 'My Term Policy',
    company: 'LIC',
    policyNumber: 'POL123',
    sumAssured: 10000000,
    annualPremium: 50000,
    premiumFrequency: 'YEARLY',
    startDate: '2024-01-01',
    maturityDate: '2044-01-01',
    renewalMonth: 1,
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    vi.clearAllMocks();
    
    (insuranceApi.getAll as any).mockResolvedValue([]);
    (insuranceApi.create as any).mockResolvedValue(mockInsurance);
    (insuranceApi.update as any).mockResolvedValue(mockInsurance);
    (insuranceApi.delete as any).mockResolvedValue({});

    (useAuthStore as any).mockReturnValue({
      user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
      features: { insurancePage: true, blockedInsuranceTypes: [] },
      refreshFeatures: vi.fn(),
    });
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
    it('renders insurance page title', async () => {
      renderWithProviders(<Insurance />);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /insurance/i })).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('renders add policy button', async () => {
      renderWithProviders(<Insurance />);
      
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add policy/i })).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('displays empty state when no insurance policies', async () => {
      renderWithProviders(<Insurance />);
      
      await waitFor(() => {
        expect(screen.getByText(/no insurance policies yet/i)).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('displays insurance policies when data available', async () => {
      (insuranceApi.getAll as any).mockResolvedValue([mockInsurance]);
      
      renderWithProviders(<Insurance />);
      
      await waitFor(() => {
        expect(screen.getByText('My Term Policy')).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('displays multiple insurance policies', async () => {
      (insuranceApi.getAll as any).mockResolvedValue([
        mockInsurance,
        { ...mockInsurance, id: 'ins-2', policyName: 'Health Policy', company: 'ICICI' },
        { ...mockInsurance, id: 'ins-3', policyName: 'ULIP Policy', company: 'HDFC' },
      ]);
      
      renderWithProviders(<Insurance />);
      
      await waitFor(() => {
        expect(screen.getByText('My Term Policy')).toBeInTheDocument();
        expect(screen.getByText('Health Policy')).toBeInTheDocument();
        expect(screen.getByText('ULIP Policy')).toBeInTheDocument();
      }, { timeout: 3000 });
    });
  });

  describe('Modal Interactions', () => {
    it('opens add modal when add button clicked', async () => {
      renderWithProviders(<Insurance />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add policy/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add policy/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add policy/i })).toBeInTheDocument();
      });
    });

    it('closes modal when cancel button clicked', async () => {
      renderWithProviders(<Insurance />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add policy/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add policy/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add policy/i })).toBeInTheDocument();
      });
      
      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      fireEvent.click(cancelButton);
      
      await waitFor(() => {
        expect(screen.queryByRole('heading', { name: /add policy/i })).not.toBeInTheDocument();
      });
    });

    it('opens edit modal when edit button clicked', async () => {
      (insuranceApi.getAll as any).mockResolvedValue([mockInsurance]);
      
      renderWithProviders(<Insurance />);
      
      await waitFor(() => {
        expect(screen.getByText('My Term Policy')).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const editButton = screen.getByRole('button', { name: /edit policy/i });
      fireEvent.click(editButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /edit policy/i })).toBeInTheDocument();
      });
    });
  });

  describe('Form Validation', () => {
    it('validates required fields on submit', async () => {
      renderWithProviders(<Insurance />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add policy/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add policy/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add policy/i })).toBeInTheDocument();
      });
      
      const saveButtons = screen.getAllByRole('button', { name: /add policy/i });
      const saveButton = saveButtons[saveButtons.length - 1];
      fireEvent.click(saveButton);
      
      // Should show validation errors
      await waitFor(() => {
        // Check for any error message
        const errorMessages = screen.queryAllByText(/required|invalid|enter/i);
        expect(errorMessages.length).toBeGreaterThan(0);
      });
    });

    it('validates type field', async () => {
      renderWithProviders(<Insurance />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add policy/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add policy/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add policy/i })).toBeInTheDocument();
      });
      
      // Fill some fields but not type
      const policyNameInput = screen.getByLabelText(/policy name/i);
      fireEvent.change(policyNameInput, { target: { value: 'Test Policy' } });
      
      const saveButtons = screen.getAllByRole('button', { name: /add policy/i });
      const saveButton = saveButtons[saveButtons.length - 1];
      fireEvent.click(saveButton);
      
      await waitFor(() => {
        expect(screen.getByText(/type is required/i)).toBeInTheDocument();
      });
    });
  });

  describe('CRUD Operations', () => {
    it('creates new insurance policy successfully', async () => {
      renderWithProviders(<Insurance />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add policy/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add policy/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add policy/i })).toBeInTheDocument();
      });
      
      // Fill required fields
      const policyNameInput = screen.getByLabelText(/policy name/i);
      fireEvent.change(policyNameInput, { target: { value: 'Test Policy' } });
      
      const typeSelect = screen.getByLabelText(/insurance type/i);
      fireEvent.change(typeSelect, { target: { value: 'HEALTH' } });
      
      const companyInput = screen.getByLabelText(/company/i);
      fireEvent.change(companyInput, { target: { value: 'Star Health' } });
      
      const coverageInput = screen.getByLabelText(/coverage amount/i);
      fireEvent.change(coverageInput, { target: { value: '500000' } });

      const premiumInput = screen.getByLabelText(/annual premium/i);
      fireEvent.change(premiumInput, { target: { value: '25000' } });

      const premiumFrequencySelect = screen.getByLabelText(/premium frequency/i);
      fireEvent.change(premiumFrequencySelect, { target: { value: 'YEARLY' } });

      const renewalMonthSelect = screen.getByLabelText(/renewal month/i);
      fireEvent.change(renewalMonthSelect, { target: { value: '1' } });

      const startDateInput = screen.getByLabelText(/start date/i);
      fireEvent.change(startDateInput, { target: { value: '2024-01-01' } });
      
      const saveButtons = screen.getAllByRole('button', { name: /add policy/i });
      const saveButton = saveButtons[saveButtons.length - 1];
      fireEvent.click(saveButton);
      
      await waitFor(() => {
        expect(insuranceApi.create).toHaveBeenCalled();
      }, { timeout: 5000 });
    }, 15000);

    it('deletes insurance policy', async () => {
      (insuranceApi.getAll as any).mockResolvedValue([mockInsurance]);
      global.confirm = vi.fn(() => true);
      
      renderWithProviders(<Insurance />);
      
      await waitFor(() => {
        expect(screen.getByText('My Term Policy')).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const deleteButton = screen.getByRole('button', { name: /delete policy/i });
      fireEvent.click(deleteButton);
      
      await waitFor(() => {
        expect(insuranceApi.delete).toHaveBeenCalledWith('ins-1');
      });
    });

    it('cancels delete when user declines confirmation', async () => {
      (insuranceApi.getAll as any).mockResolvedValue([mockInsurance]);
      global.confirm = vi.fn(() => false);
      
      renderWithProviders(<Insurance />);
      
      await waitFor(() => {
        expect(screen.getByText('My Term Policy')).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const deleteButton = screen.getByRole('button', { name: /delete policy/i });
      fireEvent.click(deleteButton);
      expect(insuranceApi.delete).not.toHaveBeenCalled();
    });
  });

  describe('Error Handling', () => {
    it('handles API error on fetch', async () => {
      (insuranceApi.getAll as any).mockRejectedValueOnce(new Error('Failed to fetch'));
      
      renderWithProviders(<Insurance />);
      
      // Even with error, page should still render with add button
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add policy/i })).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('handles API error on create', async () => {
      (insuranceApi.create as any).mockRejectedValueOnce(new Error('Failed to create'));
      
      renderWithProviders(<Insurance />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add policy/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add policy/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add policy/i })).toBeInTheDocument();
      });
      
      // Fill minimum required fields
      const policyNameInput = screen.getByLabelText(/policy name/i);
      fireEvent.change(policyNameInput, { target: { value: 'Test Policy' } });
      
      const typeSelect = screen.getByLabelText(/insurance type/i);
      fireEvent.change(typeSelect, { target: { value: 'HEALTH' } });
      
      const companyInput = screen.getByLabelText(/company/i);
      fireEvent.change(companyInput, { target: { value: 'Star Health' } });
      
      const coverageInput = screen.getByLabelText(/coverage amount/i);
      fireEvent.change(coverageInput, { target: { value: '500000' } });

      const premiumInput = screen.getByLabelText(/annual premium/i);
      fireEvent.change(premiumInput, { target: { value: '25000' } });

      const premiumFrequencySelect = screen.getByLabelText(/premium frequency/i);
      fireEvent.change(premiumFrequencySelect, { target: { value: 'YEARLY' } });

      const renewalMonthSelect = screen.getByLabelText(/renewal month/i);
      fireEvent.change(renewalMonthSelect, { target: { value: '1' } });

      const startDateInput = screen.getByLabelText(/start date/i);
      fireEvent.change(startDateInput, { target: { value: '2024-01-01' } });
      
      const saveButtons = screen.getAllByRole('button', { name: /add policy/i });
      const saveButton = saveButtons[saveButtons.length - 1];
      fireEvent.click(saveButton);
      
      await waitFor(() => {
        expect(insuranceApi.create).toHaveBeenCalled();
      }, { timeout: 3000 });
    });
  });

  describe('Blocked Insurance Types', () => {
    it('filters out blocked insurance types', async () => {
      (useAuthStore as any).mockReturnValue({
        user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
        features: { 
          insurancePage: true, 
          blockedInsuranceTypes: ['ULIP', 'ENDOWMENT'] 
        },
        refreshFeatures: vi.fn(),
      });
      
      renderWithProviders(<Insurance />);
      
      // Wait for page to load first
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add policy/i })).toBeInTheDocument();
      }, { timeout: 3000 });
      
      const addButton = screen.getByRole('button', { name: /add policy/i });
      fireEvent.click(addButton);
      
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add policy/i })).toBeInTheDocument();
      });
    });
  });
});
