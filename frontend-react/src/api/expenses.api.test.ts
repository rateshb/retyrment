import { describe, it, expect, vi, beforeEach } from 'vitest';
import { expensesApi } from './expenses.api';

const requestMock = vi.fn();
const trackEventMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

vi.mock('../lib/analytics', () => ({
  trackEvent: (...args: unknown[]) => trackEventMock(...args),
}));

describe('expensesApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
    trackEventMock.mockReset();
  });

  it('requests all expenses', async () => {
    requestMock.mockResolvedValue([]);
    await expensesApi.getAll();
    expect(requestMock).toHaveBeenCalledWith('/expenses');
  });

  it('creates an expense and tracks event', async () => {
    const expense = { source: 'Groceries', monthlyAmount: 1000 };
    requestMock.mockResolvedValue(expense);

    await expensesApi.create(expense as any);

    expect(requestMock).toHaveBeenCalledWith('/expenses', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(expense),
    }));
    expect(trackEventMock).toHaveBeenCalledWith('record_created', { record_type: 'expenses' });
  });

  it('updates an expense', async () => {
    const expense = { source: 'Rent', monthlyAmount: 5000 };
    requestMock.mockResolvedValue(expense);

    await expensesApi.update('exp-1', expense as any);

    expect(requestMock).toHaveBeenCalledWith('/expenses/exp-1', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify(expense),
    }));
  });

  it('deletes an expense', async () => {
    requestMock.mockResolvedValue({});
    await expensesApi.delete('exp-2');
    expect(requestMock).toHaveBeenCalledWith('/expenses/exp-2', { method: 'DELETE' });
  });

  it('requests investment opportunities', async () => {
    requestMock.mockResolvedValue({ ok: true });
    await expensesApi.getInvestmentOpportunities(30, 60);
    expect(requestMock).toHaveBeenCalledWith('/expenses/investment-opportunities?currentAge=30&retirementAge=60');
  });
});
