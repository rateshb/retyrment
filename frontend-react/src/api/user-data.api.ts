import { request } from '../lib/api-client';

export const userDataApi = {
  getSummary: () => request<any>('/user/data/summary'),
  deleteAll: (confirmation: string) => request<any>(`/user/data/all?confirmation=${confirmation}`, {
    method: 'DELETE',
  }),
};
