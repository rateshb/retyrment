import { request } from '../lib/api-client';
import { User, FeatureAccess } from '../lib/types';
import { auth } from '../lib/auth';

export const authApi = {
  me: () => request<User>('/auth/me'),
  features: () => request<{ features: FeatureAccess }>('/auth/features'),
};
