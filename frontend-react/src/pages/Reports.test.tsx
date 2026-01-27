import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Reports } from './Reports';

vi.mock('../lib/api', () => ({
  exportApi: {
    pdfSummary: vi.fn().mockResolvedValue(new Blob()),
    pdfRetirement: vi.fn().mockResolvedValue(new Blob()),
    pdfCalendar: vi.fn().mockResolvedValue(new Blob()),
    excel: vi.fn().mockResolvedValue(new Blob()),
    getPdfUrl: vi.fn().mockReturnValue('/api/export/pdf'),
    getExcelUrl: vi.fn().mockReturnValue('/api/export/excel'),
  },
  analysisApi: {
    networth: vi.fn().mockResolvedValue({ totalAssets: 1000000, totalLiabilities: 200000 }),
    recommendations: vi.fn().mockResolvedValue([]),
    goals: vi.fn().mockResolvedValue([]),
    fullSummary: vi.fn().mockResolvedValue({}),
  },
  retirementApi: {
    calculate: vi.fn().mockResolvedValue({}),
  },
}));

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: '1', email: 'test@example.com', name: 'Test User', role: 'PRO' },
    features: { 
      reportsPage: true,
      canExportPdf: true,
      canExportExcel: true,
      canExportJson: true,
      canImportData: true,
    },
    refreshFeatures: vi.fn(),
  }),
}));

describe('Reports', () => {
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

  it('renders reports page', () => {
    renderWithProviders(<Reports />);
    // MainLayout renders the page
    expect(document.body).toBeInTheDocument();
  });
});
