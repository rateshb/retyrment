import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MainLayout } from './MainLayout';

const refreshFeaturesIfNeeded = vi.fn();
const fetchFeatures = vi.fn();

vi.mock('../../stores/authStore', () => ({
  useAuthStore: () => ({
    refreshFeaturesIfNeeded,
    fetchFeatures,
    isAuthenticated: true,
  }),
}));

vi.mock('./Sidebar', () => ({
  Sidebar: () => <div>Sidebar</div>,
}));

describe('MainLayout', () => {
  it('renders title, subtitle, and children', () => {
    render(
      <MainLayout title="Title" subtitle="Subtitle">
        <div>Content</div>
      </MainLayout>
    );

    expect(screen.getByText('Title')).toBeInTheDocument();
    expect(screen.getByText('Subtitle')).toBeInTheDocument();
    expect(screen.getByText('Content')).toBeInTheDocument();
    expect(screen.getByText('Sidebar')).toBeInTheDocument();
    expect(document.title).toBe('Title | Retyrment');
  });

  it('refreshes features on mount and on button click', async () => {
    render(
      <MainLayout title="Title">
        <div>Content</div>
      </MainLayout>
    );

    expect(refreshFeaturesIfNeeded).toHaveBeenCalled();

    const btn = screen.getByTitle('Refresh permissions');
    fireEvent.click(btn);
    expect(fetchFeatures).toHaveBeenCalled();
  });
});
