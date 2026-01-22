import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { render, screen } from '@testing-library/react';

let isLoggedIn = false;
let storeState: any = {
  isAuthenticated: false,
  user: null,
  features: null,
  fetchUser: () => Promise.resolve(),
  fetchFeatures: () => Promise.resolve(),
};

vi.mock('../lib/api', () => ({
  auth: {
    isLoggedIn: () => isLoggedIn,
  },
}));

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => storeState,
}));

import { ProtectedRoute } from './ProtectedRoute';

describe('ProtectedRoute', () => {
  it('redirects to login when not authenticated', () => {
    isLoggedIn = false;
    storeState = { ...storeState, user: null, features: null };

    render(
      <MemoryRouter initialEntries={['/private']}>
        <Routes>
          <Route
            path="/private"
            element={
              <ProtectedRoute>
                <div>Secret</div>
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<div>Login</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Login')).toBeInTheDocument();
  });

  it('redirects to home when admin required', () => {
    isLoggedIn = true;
    storeState = { ...storeState, user: { role: 'FREE' }, features: {} };

    render(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route
            path="/admin"
            element={
              <ProtectedRoute requireAdmin>
                <div>Admin</div>
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<div>Home</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Home')).toBeInTheDocument();
  });

  it('redirects to home when feature missing', () => {
    isLoggedIn = true;
    storeState = { ...storeState, user: { role: 'FREE' }, features: { reportsPage: false } };

    render(
      <MemoryRouter initialEntries={['/reports']}>
        <Routes>
          <Route
            path="/reports"
            element={
              <ProtectedRoute requireFeature="reportsPage">
                <div>Reports</div>
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<div>Home</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Home')).toBeInTheDocument();
  });

  it('renders children when allowed', () => {
    isLoggedIn = true;
    storeState = { ...storeState, user: { role: 'ADMIN' }, features: { reportsPage: true } };

    render(
      <MemoryRouter initialEntries={['/reports']}>
        <Routes>
          <Route
            path="/reports"
            element={
              <ProtectedRoute requireAdmin requireFeature="reportsPage">
                <div>Reports</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Reports')).toBeInTheDocument();
  });
});
