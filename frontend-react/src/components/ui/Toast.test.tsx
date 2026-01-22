import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ToastContainer, toast } from './Toast';

describe('Toast', () => {
  it('shows and dismisses toast messages', () => {
    vi.useFakeTimers();
    render(<ToastContainer />);

    act(() => {
      toast.success('Saved');
    });
    expect(screen.getByText('Saved')).toBeInTheDocument();

    // Auto-dismiss after 5s
    act(() => {
      vi.advanceTimersByTime(5000);
    });
    expect(screen.queryByText('Saved')).toBeNull();

    vi.useRealTimers();
  });
});
