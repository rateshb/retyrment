import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { render, screen, fireEvent, within, waitFor } from '@testing-library/react';

let storeState: any;

const defaultStoreState = () => ({
  user: { role: 'FREE', name: 'User', email: 'user@test.com' },
  features: {
    incomePage: true,
    investmentPage: true,
    loanPage: true,
    insurancePage: true,
    expensePage: true,
    goalsPage: true,
    familyPage: true,
    calendarPage: true,
    retirementPage: true,
    insuranceRecommendationsPage: true,
    simulationPage: true,
    reportsPage: true,
    preferencesPage: true,
    settingsPage: true,
  },
  logout: vi.fn(),
});

vi.mock('../../stores/authStore', () => ({
  useAuthStore: () => storeState,
}));

import { Sidebar } from './Sidebar';

describe('Sidebar', () => {
  beforeEach(() => {
    storeState = defaultStoreState();
  });

  it('renders sections and nav items based on features', () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Sidebar />
      </MemoryRouter>
    );

    expect(screen.getByText('Overview')).toBeInTheDocument();
    expect(screen.getByText('Data Entry')).toBeInTheDocument();
    expect(screen.getByText('Analysis')).toBeInTheDocument();
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Reports')).toBeInTheDocument();
  });

  it('hides sections when no features are enabled', () => {
    storeState = { ...storeState, features: {} };

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Sidebar />
      </MemoryRouter>
    );

    expect(screen.queryByText('Data Entry')).toBeNull();
    expect(screen.queryByText('Analysis')).toBeNull();
  });

  it('shows admin panel for admin users', () => {
    storeState = { ...storeState, user: { role: 'ADMIN', name: 'Admin', email: 'admin@test.com' } };

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Sidebar />
      </MemoryRouter>
    );

    expect(screen.getByText('Administration')).toBeInTheDocument();
    expect(screen.getByText('Admin Panel')).toBeInTheDocument();
  });

  it('calls logout on click', () => {
    const logout = vi.fn();
    storeState = { ...storeState, logout, user: { role: 'FREE', name: 'Test User', email: 'user@test.com' } };

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Sidebar />
      </MemoryRouter>
    );

    // First click the settings toggle to reveal Logout button
    const settingsToggle = screen.getAllByRole('button').find(btn => 
      btn.textContent?.includes('Test User')
    );
    expect(settingsToggle).toBeDefined();
    fireEvent.click(settingsToggle!);
    
    // Now click Logout
    fireEvent.click(screen.getByText('Logout'));
    expect(logout).toHaveBeenCalled();
  });

  it('shows profile image when available', () => {
    storeState = {
      ...storeState,
      user: {
        role: 'FREE',
        name: 'Test User',
        email: 'user@test.com',
        picture: 'https://example.com/avatar.png',
      },
    };

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Sidebar />
      </MemoryRouter>
    );

    expect(screen.getByRole('img', { name: 'Test User' })).toBeInTheDocument();
  });

  it('falls back to avatar when image fails to load', async () => {
    storeState = {
      ...storeState,
      user: {
        role: 'FREE',
        name: 'Test User',
        email: 'user@test.com',
        picture: 'https://example.com/broken.png',
      },
    };

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Sidebar />
      </MemoryRouter>
    );

    const img = screen.getByRole('img', { name: 'Test User' });

    const settingsToggle = screen.getAllByRole('button').find(btn =>
      btn.textContent?.includes('Test User')
    );
    expect(settingsToggle).toBeDefined();

    const initialSvgCount = settingsToggle!.querySelectorAll('svg').length;
    fireEvent.error(img);

    await waitFor(() => {
      expect(within(settingsToggle!).queryByRole('img')).toBeNull();
    });

    const afterErrorSvgCount = settingsToggle!.querySelectorAll('svg').length;
    expect(afterErrorSvgCount).toBeGreaterThan(initialSvgCount);
  });
});
