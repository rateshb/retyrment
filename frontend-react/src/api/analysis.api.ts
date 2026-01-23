import { request } from '../lib/api-client';

export const analysisApi = {
  networth: () => request<any>('/analysis/networth'),
  goals: () => request<any>('/analysis/goals'),
  recommendations: () => request<any>('/analysis/recommendations'),
};
