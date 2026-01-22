import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { render, screen, fireEvent } from '@testing-library/react';

let storeState: any = {
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
};

vi.mock('../../stores/authStore', () => ({
  useAuthStore: () => storeState,
}));

import { Sidebar } from './Sidebar';

describe('Sidebar', () => {
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
    storeState = { ...storeState, logout };

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Sidebar />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByText('Logout'));
    expect(logout).toHaveBeenCalled();
  });
});
