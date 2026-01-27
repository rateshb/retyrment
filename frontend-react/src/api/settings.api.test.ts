import { describe, it, expect, vi, beforeEach } from 'vitest';
import { settingsApi } from './settings.api';

const requestMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

describe('settingsApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
  });

  it('gets settings', async () => {
    requestMock.mockResolvedValue({ currentAge: 35 });
    const result = await settingsApi.get();
    expect(result).toEqual({ currentAge: 35 });
    expect(requestMock).toHaveBeenCalledWith('/settings');
  });

  it('updates settings', async () => {
    const payload = { retirementAge: 60 };
    requestMock.mockResolvedValue({ ok: true });
    await settingsApi.update(payload);
    expect(requestMock).toHaveBeenCalledWith('/settings', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify(payload),
    }));
  });
});
