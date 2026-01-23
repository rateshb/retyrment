import { request } from '../lib/api-client';
import { Insurance } from '../lib/types';

export const insuranceApi = {
  getAll: () => request<Insurance[]>('/insurance'),
  create: (data: Insurance) => request<Insurance>('/insurance', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  update: (id: string, data: Insurance) => request<Insurance>(`/insurance/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  delete: (id: string) => request<void>(`/insurance/${id}`, { method: 'DELETE' }),
};
