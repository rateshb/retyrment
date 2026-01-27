import { describe, it, expect, vi, beforeEach } from 'vitest';
import { authApi } from './auth.api';

const requestMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

vi.mock('../lib/auth', () => ({
  auth: {
    getToken: vi.fn(),
  },
}));

describe('authApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
  });

  it('requests current user', async () => {
    requestMock.mockResolvedValue({ id: '1' });
    const result = await authApi.me();
    expect(result).toEqual({ id: '1' });
    expect(requestMock).toHaveBeenCalledWith('/auth/me');
  });

  it('requests feature access', async () => {
    requestMock.mockResolvedValue({ features: { incomePage: true } });
    const result = await authApi.features();
    expect(result).toEqual({ features: { incomePage: true } });
    expect(requestMock).toHaveBeenCalledWith('/auth/features');
  });
});
