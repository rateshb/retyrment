import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { Card, CardContent, CardHeader } from './Card';

describe('Card', () => {
  it('renders children', () => {
    render(
      <Card>
        <div>Body</div>
      </Card>
    );
    expect(screen.getByText('Body')).toBeInTheDocument();
  });

  it('renders header with subtitle and action', () => {
    render(
      <CardHeader title="Title" subtitle="Subtitle" action={<button>Action</button>} />
    );
    expect(screen.getByText('Title')).toBeInTheDocument();
    expect(screen.getByText('Subtitle')).toBeInTheDocument();
    expect(screen.getByText('Action')).toBeInTheDocument();
  });

  it('renders content wrapper', () => {
    render(
      <CardContent>
        <span>Content</span>
      </CardContent>
    );
    expect(screen.getByText('Content')).toBeInTheDocument();
  });
});
