import { request } from '../lib/api-client';
import { Investment } from '../lib/types';

export const investmentsApi = {
  getAll: () => request<Investment[]>('/investments'),
  create: (data: Investment) => request<Investment>('/investments', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  update: (id: string, data: Investment) => request<Investment>(`/investments/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  delete: (id: string) => request<void>(`/investments/${id}`, { method: 'DELETE' }),
};
