// Environment configuration
export const config = {
  apiUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  oauthBaseUrl: import.meta.env.VITE_OAUTH_BASE_URL || 'http://localhost:8080',
  isDevelopment: import.meta.env.DEV,
  isProduction: import.meta.env.PROD,
  ga4MeasurementId: import.meta.env.VITE_GA4_MEASUREMENT_ID || '',
  gtmContainerId: import.meta.env.VITE_GTM_CONTAINER_ID || '',
};
