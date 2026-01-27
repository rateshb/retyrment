// Mock localStorage before any other imports (needed for authStore initialization)
const localStorageStore = new Map<string, string>();
const localStorageMock = {
  getItem: (key: string) => (localStorageStore.has(key) ? localStorageStore.get(key)! : null),
  setItem: (key: string, value: string) => {
    localStorageStore.set(key, String(value));
  },
  removeItem: (key: string) => {
    localStorageStore.delete(key);
  },
  clear: () => {
    localStorageStore.clear();
  },
  key: (index: number) => Array.from(localStorageStore.keys())[index] ?? null,
  get length() {
    return localStorageStore.size;
  },
};
Object.defineProperty(global, 'localStorage', {
  value: localStorageMock,
  writable: true,
});

// Mock window.location for tests that use navigation
Object.defineProperty(global, 'location', {
  value: {
    href: 'http://localhost:3000',
    origin: 'http://localhost:3000',
    pathname: '/',
    search: '',
    hash: '',
    assign: () => {},
    replace: () => {},
    reload: () => {},
  },
  writable: true,
});

import '@testing-library/jest-dom/vitest';
import { afterEach, beforeEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';

// Reset mocks before each test
beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(() => {
  cleanup();
});