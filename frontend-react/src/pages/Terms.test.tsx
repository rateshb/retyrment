import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Terms } from './Terms';

describe('Terms', () => {
  it('renders terms of service title', () => {
    render(
      <MemoryRouter>
        <Terms />
      </MemoryRouter>
    );
    expect(screen.getByText('Terms of Service')).toBeInTheDocument();
  });

  it('renders back to home link', () => {
    render(
      <MemoryRouter>
        <Terms />
      </MemoryRouter>
    );
    expect(screen.getByText('Back to Home')).toBeInTheDocument();
  });

  it('renders acceptance of terms section', () => {
    render(
      <MemoryRouter>
        <Terms />
      </MemoryRouter>
    );
    expect(screen.getByText('1. Acceptance of Terms')).toBeInTheDocument();
  });

  it('renders description of service section', () => {
    render(
      <MemoryRouter>
        <Terms />
      </MemoryRouter>
    );
    expect(screen.getByText('2. Description of Service')).toBeInTheDocument();
  });

  it('renders last updated date', () => {
    render(
      <MemoryRouter>
        <Terms />
      </MemoryRouter>
    );
    const elements = screen.getAllByText(/Last updated/);
    expect(elements.length).toBeGreaterThan(0);
  });
});
