// Environment configuration
export const config = {
  apiUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  oauthBaseUrl: import.meta.env.VITE_OAUTH_BASE_URL || 'http://localhost:8080',
  isDevelopment: import.meta.env.DEV,
  isProduction: import.meta.env.PROD,
};
