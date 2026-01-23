import { request } from '../lib/api-client';

export const insuranceRecommendationsApi = {
  getOverall: () => request<any>('/insurance/recommendations'),
};
