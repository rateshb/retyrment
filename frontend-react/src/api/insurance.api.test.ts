import { describe, it, expect, vi, beforeEach } from 'vitest';
import { insuranceApi } from './insurance.api';

const requestMock = vi.fn();
const trackEventMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

vi.mock('../lib/analytics', () => ({
  trackEvent: (...args: unknown[]) => trackEventMock(...args),
}));

describe('insuranceApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
    trackEventMock.mockReset();
  });

  it('requests all policies', async () => {
    requestMock.mockResolvedValue([]);
    await insuranceApi.getAll();
    expect(requestMock).toHaveBeenCalledWith('/insurance');
  });

  it('creates a policy and tracks event', async () => {
    const policy = { policyName: 'Term', annualPremium: 5000 };
    requestMock.mockResolvedValue(policy);

    await insuranceApi.create(policy as any);

    expect(requestMock).toHaveBeenCalledWith('/insurance', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(policy),
    }));
    expect(trackEventMock).toHaveBeenCalledWith('record_created', { record_type: 'insurance' });
  });

  it('updates a policy', async () => {
    const policy = { policyName: 'Health', annualPremium: 3000 };
    requestMock.mockResolvedValue(policy);

    await insuranceApi.update('ins-1', policy as any);

    expect(requestMock).toHaveBeenCalledWith('/insurance/ins-1', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify(policy),
    }));
  });

  it('deletes a policy', async () => {
    requestMock.mockResolvedValue({});
    await insuranceApi.delete('ins-2');
    expect(requestMock).toHaveBeenCalledWith('/insurance/ins-2', { method: 'DELETE' });
  });
});
