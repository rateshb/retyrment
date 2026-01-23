import { request } from '../lib/api-client';

export const adminApi = {
  getUsers: () => request<any>('/admin/users'),
  getUser: (id: string) => request<any>(`/admin/users/${id}`),
  searchUsers: (email: string) => request<any>(`/admin/users/search?email=${email}`),
  updateRole: (id: string, data: { role: string; durationDays?: number; reason?: string }) => 
    request<any>(`/admin/users/${id}/role`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  deleteUser: (id: string) => request<any>(`/admin/users/${id}`, { method: 'DELETE' }),
  extendTrial: (id: string, days: number) => request<any>(`/admin/users/${id}/extend-trial`, {
    method: 'PUT',
    body: JSON.stringify({ days }),
  }),
  getFeatureAccess: (id: string) => request<any>(`/admin/users/${id}/features`),
  updateUserFeatures: (id: string, features: any) => request<any>(`/admin/users/${id}/features`, {
    method: 'PUT',
    body: JSON.stringify(features),
  }),
};
