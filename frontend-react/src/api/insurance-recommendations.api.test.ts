import { describe, it, expect, vi, beforeEach } from 'vitest';
import { insuranceRecommendationsApi } from './insurance-recommendations.api';

const requestMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

describe('insuranceRecommendationsApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
  });

  it('gets overall recommendations', async () => {
    requestMock.mockResolvedValue({ ok: true });
    const result = await insuranceRecommendationsApi.getOverall();
    expect(result).toEqual({ ok: true });
    expect(requestMock).toHaveBeenCalledWith('/insurance/recommendations');
  });
});
