import { request } from '../lib/api-client';
import { Goal } from '../lib/types';

export const goalsApi = {
  getAll: () => request<Goal[]>('/goals'),
  create: (data: Goal) => request<Goal>('/goals', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  update: (id: string, data: Goal) => request<Goal>(`/goals/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  delete: (id: string) => request<void>(`/goals/${id}`, { method: 'DELETE' }),
};
