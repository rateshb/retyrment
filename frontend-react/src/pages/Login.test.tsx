import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Login } from './Login';

// Mock useNavigate and useSearchParams
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useSearchParams: () => [new URLSearchParams()],
  };
});

// Mock auth store
vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    isAuthenticated: false,
    login: vi.fn(),
  }),
}));

// Mock config
vi.mock('../config/env', () => ({
  config: {
    oauthBaseUrl: 'http://localhost:8080',
  },
}));

describe('Login', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(window, 'location', {
      value: {
        href: 'http://localhost:3000/login',
        origin: 'http://localhost:3000',
      },
      writable: true,
    });
  });

  it('renders login page with logo and title', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    expect(screen.getByText('Retyrment')).toBeInTheDocument();
    expect(screen.getByText('Welcome Back')).toBeInTheDocument();
  });

  it('renders Google login button', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    expect(screen.getByText('Continue with Google')).toBeInTheDocument();
  });

  it('renders terms and privacy links', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    expect(screen.getByText('Terms of Service')).toBeInTheDocument();
    expect(screen.getByText('Privacy Policy')).toBeInTheDocument();
  });

  it('renders feature preview section', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    expect(screen.getByText('Track Net Worth')).toBeInTheDocument();
    expect(screen.getByText('Set Goals')).toBeInTheDocument();
    expect(screen.getByText('Plan Retirement')).toBeInTheDocument();
  });
});
