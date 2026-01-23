import { request } from '../lib/api-client';
import { Expense } from '../lib/types';

export const expensesApi = {
  getAll: () => request<Expense[]>('/expenses'),
  create: (data: Expense) => request<Expense>('/expenses', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  update: (id: string, data: Expense) => request<Expense>(`/expenses/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  delete: (id: string) => request<void>(`/expenses/${id}`, { method: 'DELETE' }),
  getInvestmentOpportunities: (currentAge: number, retirementAge: number) =>
    request<any>(`/expenses/investment-opportunities?currentAge=${currentAge}&retirementAge=${retirementAge}`),
};
