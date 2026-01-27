import { describe, it, expect, vi, beforeEach } from 'vitest';
import { goalsApi } from './goals.api';

const requestMock = vi.fn();
const trackEventMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

vi.mock('../lib/analytics', () => ({
  trackEvent: (...args: unknown[]) => trackEventMock(...args),
}));

describe('goalsApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
    trackEventMock.mockReset();
  });

  it('requests all goals', async () => {
    requestMock.mockResolvedValue([]);
    await goalsApi.getAll();
    expect(requestMock).toHaveBeenCalledWith('/goals');
  });

  it('creates a goal and tracks event', async () => {
    const goal = { name: 'Retirement', targetAmount: 1000000 };
    requestMock.mockResolvedValue(goal);

    await goalsApi.create(goal as any);

    expect(requestMock).toHaveBeenCalledWith('/goals', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(goal),
    }));
    expect(trackEventMock).toHaveBeenCalledWith('record_created', { record_type: 'goals' });
  });

  it('updates a goal', async () => {
    const goal = { name: 'Vacation', targetAmount: 100000 };
    requestMock.mockResolvedValue(goal);

    await goalsApi.update('goal-1', goal as any);

    expect(requestMock).toHaveBeenCalledWith('/goals/goal-1', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify(goal),
    }));
  });

  it('deletes a goal', async () => {
    requestMock.mockResolvedValue({});
    await goalsApi.delete('goal-2');
    expect(requestMock).toHaveBeenCalledWith('/goals/goal-2', { method: 'DELETE' });
  });
});
