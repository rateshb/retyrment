import { describe, it, expect, vi, beforeEach } from 'vitest';
import { incomeApi } from './income.api';

const requestMock = vi.fn();
const trackEventMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

vi.mock('../lib/analytics', () => ({
  trackEvent: (...args: unknown[]) => trackEventMock(...args),
}));

describe('incomeApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
    trackEventMock.mockReset();
  });

  it('requests all income records', async () => {
    requestMock.mockResolvedValue([]);
    await incomeApi.getAll();
    expect(requestMock).toHaveBeenCalledWith('/income');
  });

  it('creates an income record and tracks event', async () => {
    const income = { source: 'Salary', monthlyAmount: 50000 };
    requestMock.mockResolvedValue(income);

    await incomeApi.create(income as any);

    expect(requestMock).toHaveBeenCalledWith('/income', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(income),
    }));
    expect(trackEventMock).toHaveBeenCalledWith('record_created', { record_type: 'income' });
  });

  it('updates an income record', async () => {
    const income = { source: 'Bonus', monthlyAmount: 10000 };
    requestMock.mockResolvedValue(income);

    await incomeApi.update('inc-1', income as any);

    expect(requestMock).toHaveBeenCalledWith('/income/inc-1', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify(income),
    }));
  });

  it('deletes an income record', async () => {
    requestMock.mockResolvedValue({});
    await incomeApi.delete('inc-2');
    expect(requestMock).toHaveBeenCalledWith('/income/inc-2', { method: 'DELETE' });
  });
});
