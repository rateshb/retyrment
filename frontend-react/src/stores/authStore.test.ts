import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const mockApi = {
  auth: {
    me: vi.fn(),
    features: vi.fn(),
  },
};

const mockAuth = {
  isLoggedIn: vi.fn(),
  setToken: vi.fn(),
  setUser: vi.fn(),
  logout: vi.fn(),
};

vi.mock('../lib/api', () => ({
  api: mockApi,
  auth: mockAuth,
}));

let useAuthStore: typeof import('./authStore').useAuthStore;

beforeEach(async () => {
  vi.resetModules();
  mockApi.auth.me.mockReset();
  mockApi.auth.features.mockReset();
  mockAuth.isLoggedIn.mockReset();
  mockAuth.setToken.mockReset();
  mockAuth.setUser.mockReset();
  mockAuth.logout.mockReset();
  const mockStorage = {
    getItem: vi.fn(),
    setItem: vi.fn(),
    removeItem: vi.fn(),
    clear: vi.fn(),
  };
  vi.stubGlobal('localStorage', mockStorage);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('authStore', () => {
  it('initializes isAuthenticated from auth.isLoggedIn', async () => {
    mockAuth.isLoggedIn.mockReturnValue(false);
    ({ useAuthStore } = await import('./authStore'));
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
  });

  it('login fetches user and features', async () => {
    mockAuth.isLoggedIn.mockReturnValue(false);
    mockApi.auth.me.mockResolvedValue({ id: 'u1', email: 'u1@test.com', role: 'FREE' });
    mockApi.auth.features.mockResolvedValue({ features: { incomePage: true } });

    ({ useAuthStore } = await import('./authStore'));

    await useAuthStore.getState().login('token-123');

    expect(mockAuth.setToken).toHaveBeenCalledWith('token-123');
    expect(mockApi.auth.me).toHaveBeenCalled();
    expect(mockApi.auth.features).toHaveBeenCalled();
    expect(useAuthStore.getState().user?.email).toBe('u1@test.com');
    expect(useAuthStore.getState().features?.incomePage).toBe(true);
  });

  it('refreshFeaturesIfNeeded calls fetch when cache is stale', async () => {
    mockAuth.isLoggedIn.mockReturnValue(true);
    mockApi.auth.features.mockResolvedValue({ features: { reportsPage: true } });

    ({ useAuthStore } = await import('./authStore'));

    useAuthStore.setState({
      isAuthenticated: true,
      lastFeaturesRefresh: Date.now() - 2 * 60 * 1000,
    });

    await useAuthStore.getState().refreshFeaturesIfNeeded();

    expect(mockApi.auth.features).toHaveBeenCalled();
  });

  it('logout clears auth state', async () => {
    mockAuth.isLoggedIn.mockReturnValue(true);
    ({ useAuthStore } = await import('./authStore'));

    useAuthStore.setState({ user: { id: 'u1' } as any, isAuthenticated: true });
    useAuthStore.getState().logout();

    expect(mockAuth.logout).toHaveBeenCalled();
    expect(useAuthStore.getState().user).toBeNull();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
  });
});
