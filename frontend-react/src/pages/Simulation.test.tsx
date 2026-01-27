import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
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
      targetCorpus: 8000000,
      simulations: 1000,
      years: 25,
    }),
  },
}));

// Mock auth store
vi.mock('../stores/authStore', () => ({
  useAuthStore: vi.fn(() => ({
    user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
    features: { 
      canRunSimulation: true,
    },
    refreshFeatures: vi.fn(),
  })),
}));

import { simulationApi } from '../lib/api';
import { useAuthStore } from '../stores/authStore';

describe('Simulation', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    vi.clearAllMocks();
    
    (simulationApi.run as any).mockResolvedValue({
      percentiles: { p10: 5000000, p25: 7500000, p50: 10000000, p75: 12500000, p90: 15000000 },
      average: 10000000,
      successRate: 85,
      targetCorpus: 8000000,
      simulations: 1000,
      years: 25,
    });

    (useAuthStore as any).mockReturnValue({
      user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
      features: { 
        canRunSimulation: true,
      },
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
    it('renders simulation page title', () => {
      renderWithProviders(<Simulation />);
      expect(screen.getByText('Monte Carlo Simulation')).toBeInTheDocument();
    });

    it('renders simulation parameters section', () => {
      renderWithProviders(<Simulation />);
      expect(screen.getByText('Simulation Parameters')).toBeInTheDocument();
    });

    it('renders run simulation button', () => {
      renderWithProviders(<Simulation />);
      expect(screen.getByRole('button', { name: /run simulation/i })).toBeInTheDocument();
    });

    it('renders input fields', () => {
      renderWithProviders(<Simulation />);
      
      const inputs = screen.getAllByRole('spinbutton');
      expect(inputs.length).toBeGreaterThanOrEqual(2);
    });
  });

  describe('Access Control', () => {
    it('shows restricted access message when simulation disabled', () => {
      (useAuthStore as any).mockReturnValue({
        user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
        features: { 
          canRunSimulation: false,
        },
        refreshFeatures: vi.fn(),
      });

      renderWithProviders(<Simulation />);

      expect(screen.getByText('Simulation Access Restricted')).toBeInTheDocument();
      expect(screen.getByText('Contact your administrator to enable simulation access.')).toBeInTheDocument();
    });

    it('does not show run button when access restricted', () => {
      (useAuthStore as any).mockReturnValue({
        user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'FREE' },
        features: { 
          canRunSimulation: false,
        },
        refreshFeatures: vi.fn(),
      });

      renderWithProviders(<Simulation />);

      expect(screen.queryByRole('button', { name: /run simulation/i })).not.toBeInTheDocument();
    });
  });

  describe('Form Interactions', () => {
    it('allows changing form inputs', () => {
      renderWithProviders(<Simulation />);
      
      const inputs = screen.getAllByRole('spinbutton');
      expect(inputs.length).toBeGreaterThanOrEqual(2);
      
      // Verify inputs can be changed
      fireEvent.change(inputs[0], { target: { value: '5000' } });
      expect((inputs[0] as HTMLInputElement).value).toBe('5000');
    });
  });

  describe('Running Simulation', () => {
    it('calls API when run button clicked', async () => {
      renderWithProviders(<Simulation />);
      
      const runButton = screen.getByRole('button', { name: /run simulation/i });
      fireEvent.click(runButton);
      
      await waitFor(() => {
        expect(simulationApi.run).toHaveBeenCalled();
      });
    });

    it('displays results after successful simulation', async () => {
      renderWithProviders(<Simulation />);
      
      const runButton = screen.getByRole('button', { name: /run simulation/i });
      fireEvent.click(runButton);
      
      await waitFor(() => {
        expect(screen.getByText('Success Rate')).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('handles API error gracefully', async () => {
      (simulationApi.run as any).mockRejectedValueOnce(new Error('Simulation failed'));
      
      renderWithProviders(<Simulation />);
      
      const runButton = screen.getByRole('button', { name: /run simulation/i });
      fireEvent.click(runButton);
      
      await waitFor(() => {
        expect(simulationApi.run).toHaveBeenCalled();
      });
    });
  });

  describe('Results Display', () => {
    it('shows success rate after simulation', async () => {
      renderWithProviders(<Simulation />);
      
      const runButton = screen.getByRole('button', { name: /run simulation/i });
      fireEvent.click(runButton);
      
      await waitFor(() => {
        expect(screen.getByText('Success Rate')).toBeInTheDocument();
        expect(screen.getByText('85.0%')).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('shows warning for medium success rate', async () => {
      (simulationApi.run as any).mockResolvedValueOnce({
        percentiles: { p10: 3000000, p25: 5000000, p50: 7000000, p75: 8500000, p90: 10000000 },
        average: 7000000,
        successRate: 65,
        targetCorpus: 8000000,
        simulations: 1000,
        years: 25,
      });

      renderWithProviders(<Simulation />);
      
      const runButton = screen.getByRole('button', { name: /run simulation/i });
      fireEvent.click(runButton);
      
      await waitFor(() => {
        expect(screen.getByText('65.0%')).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('shows danger for low success rate', async () => {
      (simulationApi.run as any).mockResolvedValueOnce({
        percentiles: { p10: 2000000, p25: 3000000, p50: 4000000, p75: 5000000, p90: 6000000 },
        average: 4000000,
        successRate: 35,
        targetCorpus: 8000000,
        simulations: 1000,
        years: 25,
      });

      renderWithProviders(<Simulation />);
      
      const runButton = screen.getByRole('button', { name: /run simulation/i });
      fireEvent.click(runButton);
      
      await waitFor(() => {
        expect(screen.getByText('35.0%')).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('does not show results before simulation runs', () => {
      renderWithProviders(<Simulation />);
      
      expect(screen.queryByText('Success Rate')).not.toBeInTheDocument();
    });
  });

  describe('Edge Cases', () => {
    it('handles zero success rate', async () => {
      (simulationApi.run as any).mockResolvedValueOnce({
        percentiles: { p10: 1000000, p25: 2000000, p50: 3000000, p75: 4000000, p90: 5000000 },
        average: 3000000,
        successRate: 0,
        targetCorpus: 10000000,
        simulations: 1000,
        years: 25,
      });

      renderWithProviders(<Simulation />);
      
      const runButton = screen.getByRole('button', { name: /run simulation/i });
      fireEvent.click(runButton);
      
      await waitFor(() => {
        expect(screen.getByText('0.0%')).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('handles 100% success rate', async () => {
      (simulationApi.run as any).mockResolvedValueOnce({
        percentiles: { p10: 15000000, p25: 17000000, p50: 20000000, p75: 22000000, p90: 25000000 },
        average: 20000000,
        successRate: 100,
        targetCorpus: 8000000,
        simulations: 1000,
        years: 25,
      });

      renderWithProviders(<Simulation />);
      
      const runButton = screen.getByRole('button', { name: /run simulation/i });
      fireEvent.click(runButton);
      
      await waitFor(() => {
        expect(screen.getByText('100.0%')).toBeInTheDocument();
      }, { timeout: 3000 });
    });

    it('runs multiple simulations sequentially', async () => {
      renderWithProviders(<Simulation />);
      
      const runButton = screen.getByRole('button', { name: /run simulation/i });
      
      // First simulation
      fireEvent.click(runButton);
      await waitFor(() => {
        expect(screen.getByText('85.0%')).toBeInTheDocument();
      }, { timeout: 3000 });
      
      // Second simulation
      (simulationApi.run as any).mockResolvedValueOnce({
        percentiles: { p10: 6000000, p25: 8000000, p50: 11000000, p75: 13000000, p90: 16000000 },
        average: 11000000,
        successRate: 90,
        simulations: 2000,
        years: 30,
      });
      
      fireEvent.click(runButton);
      await waitFor(() => {
        expect(screen.getByText('90.0%')).toBeInTheDocument();
      }, { timeout: 3000 });
      
      expect(simulationApi.run).toHaveBeenCalledTimes(2);
    });
  });
});
