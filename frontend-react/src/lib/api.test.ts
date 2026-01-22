import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { api, auth } from './api';

const mockFetch = vi.fn();
const mockStorage = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
};

describe('api', () => {
  const originalLocation = window.location;

  beforeEach(() => {
    mockFetch.mockReset();
    mockStorage.getItem.mockReset();
    mockStorage.setItem.mockReset();
    mockStorage.removeItem.mockReset();
    vi.stubGlobal('fetch', mockFetch);
    vi.stubGlobal('localStorage', mockStorage);
    // Prevent jsdom navigation errors when auth.logout sets window.location.href
    // @ts-expect-error override for test
    delete window.location;
    // @ts-expect-error override for test
    window.location = { href: '' };
  });

  afterEach(() => {
    window.location = originalLocation;
  });

  it('auth helpers read/write localStorage', () => {
    mockStorage.getItem.mockReturnValue('token');
    expect(auth.getToken()).toBe('token');
    auth.setToken('t2');
    expect(mockStorage.setItem).toHaveBeenCalledWith('retyrment_token', 't2');
    auth.removeToken();
    expect(mockStorage.removeItem).toHaveBeenCalledWith('retyrment_token');
  });

  it('auth.logout clears storage and redirects', () => {
    auth.logout();

    expect(mockStorage.removeItem).toHaveBeenCalledWith('retyrment_token');
    expect(mockStorage.removeItem).toHaveBeenCalledWith('retyrment_user');
    expect(mockStorage.removeItem).toHaveBeenCalledWith('retyrment_features');
    expect(window.location.href).toBe('/login');
  });

  it('request handles 401 by logging out', async () => {
    mockFetch.mockResolvedValue({
      status: 401,
      ok: false,
      json: async () => ({ message: 'Session expired' }),
      text: async () => '',
    });

    await expect(api.auth.me()).rejects.toThrow('Session expired');
    expect(mockStorage.removeItem).toHaveBeenCalledWith('retyrment_token');
  });

  it('request returns parsed JSON on success', async () => {
    mockFetch.mockResolvedValue({
      status: 200,
      ok: true,
      text: async () => JSON.stringify({ ok: true }),
    });

    const result = await api.analysis.networth();
    expect(result).toEqual({ ok: true });
  });

  it('request returns empty object on empty response', async () => {
    mockFetch.mockResolvedValue({
      status: 200,
      ok: true,
      text: async () => '',
    });

    const result = await api.analysis.goals();
    expect(result).toEqual({});
  });
});
