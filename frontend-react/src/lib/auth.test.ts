import { describe, it, expect, vi, beforeEach } from 'vitest';
import { auth } from './auth';

// Create a real localStorage mock for testing
const createLocalStorageMock = () => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => { store[key] = value; },
    removeItem: (key: string) => { delete store[key]; },
    clear: () => { store = {}; },
    get length() { return Object.keys(store).length; },
    key: (index: number) => Object.keys(store)[index] || null,
  };
};

describe('auth', () => {
  let localStorageMock: ReturnType<typeof createLocalStorageMock>;

  beforeEach(() => {
    localStorageMock = createLocalStorageMock();
    Object.defineProperty(global, 'localStorage', {
      value: localStorageMock,
      writable: true,
    });
    vi.clearAllMocks();
  });

  describe('getToken', () => {
    it('returns null when no token exists', () => {
      expect(auth.getToken()).toBeNull();
    });

    it('returns token when it exists', () => {
      localStorage.setItem('retyrment_token', 'test-token');
      expect(auth.getToken()).toBe('test-token');
    });
  });

  describe('setToken', () => {
    it('stores token in localStorage', () => {
      auth.setToken('my-token');
      expect(localStorage.getItem('retyrment_token')).toBe('my-token');
    });
  });

  describe('removeToken', () => {
    it('removes token from localStorage', () => {
      localStorage.setItem('retyrment_token', 'test-token');
      auth.removeToken();
      expect(localStorage.getItem('retyrment_token')).toBeNull();
    });
  });

  describe('getUser', () => {
    it('returns null when no user exists', () => {
      expect(auth.getUser()).toBeNull();
    });

    it('returns parsed user when it exists', () => {
      const user = { id: '1', name: 'Test User', email: 'test@example.com' };
      localStorage.setItem('retyrment_user', JSON.stringify(user));
      expect(auth.getUser()).toEqual(user);
    });
  });

  describe('setUser', () => {
    it('stores user in localStorage as JSON', () => {
      const user = { id: '1', name: 'Test User', email: 'test@example.com', role: 'FREE' };
      auth.setUser(user as any);
      const stored = JSON.parse(localStorage.getItem('retyrment_user') || '');
      expect(stored).toEqual(user);
    });
  });

  describe('removeUser', () => {
    it('removes user from localStorage', () => {
      localStorage.setItem('retyrment_user', JSON.stringify({ id: '1' }));
      auth.removeUser();
      expect(localStorage.getItem('retyrment_user')).toBeNull();
    });
  });

  describe('isLoggedIn', () => {
    it('returns false when no token exists', () => {
      expect(auth.isLoggedIn()).toBe(false);
    });

    it('returns true when token exists', () => {
      localStorage.setItem('retyrment_token', 'test-token');
      expect(auth.isLoggedIn()).toBe(true);
    });
  });

  describe('getHeaders', () => {
    it('returns content-type header when no token', () => {
      const headers = auth.getHeaders();
      expect(headers).toEqual({ 'Content-Type': 'application/json' });
    });

    it('includes authorization header when token exists', () => {
      localStorage.setItem('retyrment_token', 'my-token');
      const headers = auth.getHeaders();
      expect(headers).toEqual({
        'Content-Type': 'application/json',
        'Authorization': 'Bearer my-token',
      });
    });
  });
});
