import { describe, it, expect, vi, beforeEach } from 'vitest';
import { simulationApi } from './simulation.api';

const requestMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

describe('simulationApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
  });

  it('runs simulation with query params', async () => {
    requestMock.mockResolvedValue({ ok: true });
    const result = await simulationApi.run(500, 30);
    expect(result).toEqual({ ok: true });
    expect(requestMock).toHaveBeenCalledWith('/analysis/montecarlo?simulations=500&years=30');
  });
});
