import { config } from '../config/env';

// API error handler
export class ApiError extends Error {
  status: number;
  
  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

// Generic API request function
export async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const { auth } = await import('./auth');
  const response = await fetch(`${config.apiUrl}${endpoint}`, {
    ...options,
    headers: {
      ...auth.getHeaders(),
      ...options.headers,
    },
  });

  if (response.status === 401) {
    // Import auth dynamically to avoid circular dependency
    const { auth } = await import('./auth');
    auth.logout();
    throw new ApiError(401, 'Session expired');
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: `HTTP ${response.status}` }));
    throw new ApiError(response.status, error.message || `HTTP ${response.status}`);
  }

  // Handle empty responses
  const text = await response.text();
  if (!text) return {} as T;
  
  return JSON.parse(text);
}
