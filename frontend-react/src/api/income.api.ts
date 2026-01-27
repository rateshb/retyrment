import { request } from '../lib/api-client';
import { trackEvent } from '../lib/analytics';
import { Income } from '../lib/types';

export const incomeApi = {
  getAll: () => request<Income[]>('/income'),
  create: (data: Income) => request<Income>('/income', {
    method: 'POST',
    body: JSON.stringify(data),
  }).then((result) => {
    trackEvent('record_created', { record_type: 'income' });
    return result;
  }),
  update: (id: string, data: Income) => request<Income>(`/income/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  delete: (id: string) => request<void>(`/income/${id}`, { method: 'DELETE' }),
};
