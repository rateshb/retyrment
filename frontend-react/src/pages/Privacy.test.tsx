import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Privacy } from './Privacy';

describe('Privacy', () => {
  it('renders privacy policy title', () => {
    render(
      <MemoryRouter>
        <Privacy />
      </MemoryRouter>
    );
    expect(screen.getByText('Privacy Policy')).toBeInTheDocument();
  });

  it('renders back to home link', () => {
    render(
      <MemoryRouter>
        <Privacy />
      </MemoryRouter>
    );
    expect(screen.getByText('Back to Home')).toBeInTheDocument();
  });

  it('renders information we collect section', () => {
    render(
      <MemoryRouter>
        <Privacy />
      </MemoryRouter>
    );
    expect(screen.getByText('1. Information We Collect')).toBeInTheDocument();
  });

  it('renders data security section', () => {
    render(
      <MemoryRouter>
        <Privacy />
      </MemoryRouter>
    );
    expect(screen.getByText('3. Data Security')).toBeInTheDocument();
  });

  it('renders last updated date', () => {
    render(
      <MemoryRouter>
        <Privacy />
      </MemoryRouter>
    );
    const elements = screen.getAllByText(/Last updated/);
    expect(elements.length).toBeGreaterThan(0);
  });
});
