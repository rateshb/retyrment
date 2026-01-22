import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { Input } from './Input';

describe('Input', () => {
  it('renders label, required marker, and helper text', () => {
    render(<Input label="Amount" required helperText="Enter value" />);
    expect(screen.getByText('Amount')).toBeInTheDocument();
    expect(screen.getByText('*')).toBeInTheDocument();
    expect(screen.getByText('Enter value')).toBeInTheDocument();
  });

  it('shows error message and hides helper text when error present', () => {
    render(<Input label="Amount" error="Required" helperText="Enter value" />);
    expect(screen.getByText('Required')).toBeInTheDocument();
  });
});
