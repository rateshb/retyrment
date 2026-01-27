import { describe, it, expect, vi, beforeEach } from 'vitest';
import { investmentsApi } from './investments.api';

const requestMock = vi.fn();
const trackEventMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

vi.mock('../lib/analytics', () => ({
  trackEvent: (...args: unknown[]) => trackEventMock(...args),
}));

describe('investmentsApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
    trackEventMock.mockReset();
  });

  it('requests all investments', async () => {
    requestMock.mockResolvedValue([]);
    await investmentsApi.getAll();
    expect(requestMock).toHaveBeenCalledWith('/investments');
  });

  it('creates an investment and tracks event', async () => {
    const investment = { name: 'MF', currentValue: 10000 };
    requestMock.mockResolvedValue(investment);

    await investmentsApi.create(investment as any);

    expect(requestMock).toHaveBeenCalledWith('/investments', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(investment),
    }));
    expect(trackEventMock).toHaveBeenCalledWith('record_created', { record_type: 'investments' });
  });

  it('updates an investment', async () => {
    const investment = { name: 'FD', currentValue: 20000 };
    requestMock.mockResolvedValue(investment);

    await investmentsApi.update('inv-1', investment as any);

    expect(requestMock).toHaveBeenCalledWith('/investments/inv-1', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify(investment),
    }));
  });

  it('deletes an investment', async () => {
    requestMock.mockResolvedValue({});
    await investmentsApi.delete('inv-2');
    expect(requestMock).toHaveBeenCalledWith('/investments/inv-2', { method: 'DELETE' });
  });
});
