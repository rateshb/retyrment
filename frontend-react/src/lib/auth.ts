import { User, FeatureAccess } from './types';

// Auth helpers
export const auth = {
  getToken: () => localStorage.getItem('retyrment_token'),
  setToken: (token: string) => localStorage.setItem('retyrment_token', token),
  removeToken: () => localStorage.removeItem('retyrment_token'),
  getUser: () => {
    const user = localStorage.getItem('retyrment_user');
    return user ? JSON.parse(user) : null;
  },
  setUser: (user: User) => localStorage.setItem('retyrment_user', JSON.stringify(user)),
  removeUser: () => localStorage.removeItem('retyrment_user'),
  isLoggedIn: () => !!localStorage.getItem('retyrment_token'),
  logout: () => {
    auth.removeToken();
    auth.removeUser();
    localStorage.removeItem('retyrment_features');
    window.location.href = '/login';
  },
  getHeaders: () => {
    const token = auth.getToken();
    return {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    };
  },
};
