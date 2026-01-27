import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Features } from './Features';

describe('Features', () => {
  it('renders features page', () => {
    render(
      <MemoryRouter>
        <Features />
      </MemoryRouter>
    );
    const elements = screen.getAllByText(/Features/i);
    expect(elements.length).toBeGreaterThan(0);
  });

  it('renders back to home link', () => {
    render(
      <MemoryRouter>
        <Features />
      </MemoryRouter>
    );
    expect(screen.getByText('Back to Home')).toBeInTheDocument();
  });

  it('renders investment tracking feature', () => {
    render(
      <MemoryRouter>
        <Features />
      </MemoryRouter>
    );
    expect(screen.getByText('Investment Tracking')).toBeInTheDocument();
  });

  it('renders retirement planning feature', () => {
    render(
      <MemoryRouter>
        <Features />
      </MemoryRouter>
    );
    expect(screen.getByText('Retirement Planning')).toBeInTheDocument();
  });

  it('renders insurance advisor feature', () => {
    render(
      <MemoryRouter>
        <Features />
      </MemoryRouter>
    );
    expect(screen.getByText('Insurance Advisor')).toBeInTheDocument();
  });

  it('renders goal planning feature', () => {
    render(
      <MemoryRouter>
        <Features />
      </MemoryRouter>
    );
    expect(screen.getByText('Goal Planning')).toBeInTheDocument();
  });
});
