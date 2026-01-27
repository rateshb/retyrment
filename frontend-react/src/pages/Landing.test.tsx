import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Landing } from './Landing';

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    isAuthenticated: false,
    user: null,
  }),
}));

describe('Landing', () => {
  it('renders landing page', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>
    );
    expect(screen.getByText('Retyrment')).toBeInTheDocument();
  });

  it('renders main heading', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>
    );
    expect(screen.getByText(/Plan Your Financial Future/i)).toBeInTheDocument();
  });

  it('renders get started button', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>
    );
    expect(screen.getByText('Get Started Free')).toBeInTheDocument();
  });

  it('renders feature highlights', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>
    );
    expect(screen.getByText(/Track Investments/i)).toBeInTheDocument();
  });
});
