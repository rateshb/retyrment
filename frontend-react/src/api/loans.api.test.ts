import { describe, it, expect, vi, beforeEach } from 'vitest';
import { loansApi } from './loans.api';

const requestMock = vi.fn();
const trackEventMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

vi.mock('../lib/analytics', () => ({
  trackEvent: (...args: unknown[]) => trackEventMock(...args),
}));

describe('loansApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
    trackEventMock.mockReset();
  });

  it('requests all loans', async () => {
    requestMock.mockResolvedValue([]);
    await loansApi.getAll();
    expect(requestMock).toHaveBeenCalledWith('/loans');
  });

  it('creates a loan and tracks event', async () => {
    const loan = { name: 'Home Loan', principal: 1000000 };
    requestMock.mockResolvedValue(loan);

    await loansApi.create(loan as any);

    expect(requestMock).toHaveBeenCalledWith('/loans', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(loan),
    }));
    expect(trackEventMock).toHaveBeenCalledWith('record_created', { record_type: 'loans' });
  });

  it('updates a loan', async () => {
    const loan = { name: 'Car Loan', principal: 500000 };
    requestMock.mockResolvedValue(loan);

    await loansApi.update('loan-1', loan as any);

    expect(requestMock).toHaveBeenCalledWith('/loans/loan-1', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify(loan),
    }));
  });

  it('deletes a loan', async () => {
    requestMock.mockResolvedValue({});
    await loansApi.delete('loan-2');
    expect(requestMock).toHaveBeenCalledWith('/loans/loan-2', { method: 'DELETE' });
  });
});
