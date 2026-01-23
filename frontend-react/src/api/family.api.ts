import { request } from '../lib/api-client';
import { FamilyMember } from '../lib/types';

export const familyApi = {
  getAll: () => request<FamilyMember[]>('/family'),
  create: (data: FamilyMember) => request<FamilyMember>('/family', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  update: (id: string, data: FamilyMember) => request<FamilyMember>(`/family/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
  delete: (id: string) => request<void>(`/family/${id}`, { method: 'DELETE' }),
};
