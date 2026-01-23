import { request } from '../lib/api-client';
import { Income } from '../lib/types';

export const incomeApi = {
  getAll: () => request<Income[]>('/income'),
  create: (data: Income) => request<Income>('/income', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  update: (id: string, data: Income) => request<Income>(`/income/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  delete: (id: string) => request<void>(`/income/${id}`, { method: 'DELETE' }),
};
