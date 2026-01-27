import { describe, it, expect, vi, beforeEach } from 'vitest';
import { retirementApi } from './retirement.api';

const requestMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

describe('retirementApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
  });

  it('calculates retirement scenario', async () => {
    const scenario = { currentAge: 30, retirementAge: 60 };
    requestMock.mockResolvedValue({ ok: true });

    const result = await retirementApi.calculate(scenario);

    expect(result).toEqual({ ok: true });
    expect(requestMock).toHaveBeenCalledWith('/retirement/calculate', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(scenario),
    }));
  });

  it('gets maturing investments', async () => {
    requestMock.mockResolvedValue([]);
    await retirementApi.getMaturing(30, 60);
    expect(requestMock).toHaveBeenCalledWith('/retirement/maturing?currentAge=30&retirementAge=60');
  });

  it('gets saved strategy', async () => {
    requestMock.mockResolvedValue({ strategy: 'SUSTAINABLE' });
    const result = await retirementApi.getStrategy();
    expect(result).toEqual({ strategy: 'SUSTAINABLE' });
    expect(requestMock).toHaveBeenCalledWith('/retirement/strategy');
  });

  it('saves strategy', async () => {
    const payload = { incomeStrategy: 'AGGRESSIVE' };
    requestMock.mockResolvedValue({ ok: true });
    await retirementApi.saveStrategy(payload);
    expect(requestMock).toHaveBeenCalledWith('/retirement/strategy', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(payload),
    }));
  });
});
