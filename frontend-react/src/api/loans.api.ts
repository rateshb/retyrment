import { request } from '../lib/api-client';
import { trackEvent } from '../lib/analytics';
import { Loan } from '../lib/types';

export const loansApi = {
  getAll: () => request<Loan[]>('/loans'),
  create: (data: Loan) => request<Loan>('/loans', {
    method: 'POST',
    body: JSON.stringify(data),
  }).then((result) => {
    trackEvent('record_created', { record_type: 'loans' });
    return result;
  }),
  update: (id: string, data: Loan) => request<Loan>(`/loans/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  delete: (id: string) => request<void>(`/loans/${id}`, { method: 'DELETE' }),
};
