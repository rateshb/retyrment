import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ApiError, request } from './api-client';

// Mock the config module
vi.mock('../config/env', () => ({
  config: {
    apiUrl: 'http://localhost:8080/api',
  },
}));

// Mock the auth module
vi.mock('./auth', () => ({
  auth: {
    getHeaders: vi.fn().mockReturnValue({ 'Content-Type': 'application/json' }),
    logout: vi.fn(),
  },
}));

// Mock fetch
const mockFetch = vi.fn();
global.fetch = mockFetch;

describe('ApiError', () => {
  it('creates an error with status and message', () => {
    const error = new ApiError(404, 'Not Found');
    expect(error.status).toBe(404);
    expect(error.message).toBe('Not Found');
    expect(error.name).toBe('ApiError');
  });
});

describe('request', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('makes a request with correct URL and headers', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify({ data: 'test' })),
    });

    await request('/test');

    expect(mockFetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/test',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      })
    );
  });

  it('returns parsed JSON response', async () => {
    const responseData = { id: 1, name: 'Test' };
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify(responseData)),
    });

    const result = await request('/test');
    expect(result).toEqual(responseData);
  });

  it('handles empty response', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 204,
      text: () => Promise.resolve(''),
    });

    const result = await request('/test');
    expect(result).toEqual({});
  });

  it('throws ApiError on non-ok response', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 404,
      json: () => Promise.resolve({ message: 'Resource not found' }),
    });

    await expect(request('/test')).rejects.toThrow('Resource not found');
  });

  it('handles 401 by calling logout', async () => {
    const { auth } = await import('./auth');
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      json: () => Promise.resolve({}),
    });

    await expect(request('/test')).rejects.toThrow('Session expired');
    expect(auth.logout).toHaveBeenCalled();
  });

  it('handles non-JSON error response', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.reject(new Error('Not JSON')),
    });

    await expect(request('/test')).rejects.toThrow('HTTP 500');
  });
});
