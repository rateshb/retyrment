import { describe, it, expect, vi, beforeEach } from 'vitest';
import { analysisApi } from './analysis.api';

const requestMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

describe('analysisApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
  });

  it('requests net worth', async () => {
    requestMock.mockResolvedValue({ netWorth: 100 });
    const result = await analysisApi.networth();
    expect(result).toEqual({ netWorth: 100 });
    expect(requestMock).toHaveBeenCalledWith('/analysis/networth');
  });

  it('requests goals analysis', async () => {
    requestMock.mockResolvedValue({ goals: [] });
    const result = await analysisApi.goals();
    expect(result).toEqual({ goals: [] });
    expect(requestMock).toHaveBeenCalledWith('/analysis/goals');
  });

  it('requests recommendations', async () => {
    requestMock.mockResolvedValue({ recommendations: [] });
    const result = await analysisApi.recommendations();
    expect(result).toEqual({ recommendations: [] });
    expect(requestMock).toHaveBeenCalledWith('/analysis/recommendations');
  });
});
