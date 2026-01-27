import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Home } from './Home';

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    isAuthenticated: false,
    user: null,
  }),
}));

describe('Home', () => {
  it('renders home page', () => {
    render(
      <MemoryRouter>
        <Home />
      </MemoryRouter>
    );
    // Home page should render without errors
    expect(document.body).toBeInTheDocument();
  });
});
