import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Disclaimer } from './Disclaimer';

describe('Disclaimer', () => {
  it('renders disclaimer title', () => {
    render(
      <MemoryRouter>
        <Disclaimer />
      </MemoryRouter>
    );
    expect(screen.getByText('Disclaimer')).toBeInTheDocument();
  });

  it('renders back to home link', () => {
    render(
      <MemoryRouter>
        <Disclaimer />
      </MemoryRouter>
    );
    expect(screen.getByText('Back to Home')).toBeInTheDocument();
  });

  it('renders important notice section', () => {
    render(
      <MemoryRouter>
        <Disclaimer />
      </MemoryRouter>
    );
    expect(screen.getByText('Important Notice')).toBeInTheDocument();
  });

  it('renders not financial advice section', () => {
    render(
      <MemoryRouter>
        <Disclaimer />
      </MemoryRouter>
    );
    expect(screen.getByText('1. Not Financial Advice')).toBeInTheDocument();
  });

  it('renders no guarantees section', () => {
    render(
      <MemoryRouter>
        <Disclaimer />
      </MemoryRouter>
    );
    expect(screen.getByText('2. No Guarantees')).toBeInTheDocument();
  });
});
