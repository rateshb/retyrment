import { request } from '../lib/api-client';

export const settingsApi = {
  get: () => request<any>('/settings'),
  update: (data: any) => request<any>('/settings', {
    method: 'PUT',
    body: JSON.stringify(data),
  }),
};
