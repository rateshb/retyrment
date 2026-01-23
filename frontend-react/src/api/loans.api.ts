import { request } from '../lib/api-client';
import { Loan } from '../lib/types';

export const loansApi = {
  getAll: () => request<Loan[]>('/loans'),
  create: (data: Loan) => request<Loan>('/loans', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  update: (id: string, data: Loan) => request<Loan>(`/loans/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  delete: (id: string) => request<void>(`/loans/${id}`, { method: 'DELETE' }),
};
