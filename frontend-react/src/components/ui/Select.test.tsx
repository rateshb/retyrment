import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { Select } from './Select';

describe('Select', () => {
  it('renders a placeholder and handles missing options', () => {
    render(<Select aria-label="sample-select" />);
    const select = screen.getByLabelText('sample-select');
    expect(select).toBeInTheDocument();
    expect(screen.getByText('Select...')).toBeInTheDocument();
  });

  it('renders provided options', () => {
    render(
      <Select
        label="Type"
        options={[
          { value: 'A', label: 'Option A' },
          { value: 'B', label: 'Option B' },
        ]}
      />
    );
    expect(screen.getByText('Option A')).toBeInTheDocument();
    expect(screen.getByText('Option B')).toBeInTheDocument();
  });
});
