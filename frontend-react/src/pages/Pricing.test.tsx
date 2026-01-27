import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Pricing } from './Pricing';

describe('Pricing', () => {
  it('renders pricing page title', () => {
    render(
      <MemoryRouter>
        <Pricing />
      </MemoryRouter>
    );
    const elements = screen.getAllByText(/Pricing/i);
    expect(elements.length).toBeGreaterThan(0);
  });

  it('renders back to home link', () => {
    render(
      <MemoryRouter>
        <Pricing />
      </MemoryRouter>
    );
    expect(screen.getByText('Back to Home')).toBeInTheDocument();
  });

  it('renders free plan', () => {
    render(
      <MemoryRouter>
        <Pricing />
      </MemoryRouter>
    );
    expect(screen.getByText('Free')).toBeInTheDocument();
    expect(screen.getByText('₹0')).toBeInTheDocument();
  });

  it('renders pro plan', () => {
    render(
      <MemoryRouter>
        <Pricing />
      </MemoryRouter>
    );
    expect(screen.getByText('Pro')).toBeInTheDocument();
    expect(screen.getByText('₹499')).toBeInTheDocument();
  });

  it('renders plan features', () => {
    render(
      <MemoryRouter>
        <Pricing />
      </MemoryRouter>
    );
    expect(screen.getByText('Track unlimited investments')).toBeInTheDocument();
    expect(screen.getByText('Monte Carlo simulation')).toBeInTheDocument();
  });
});
