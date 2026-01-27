import { describe, it, expect, vi, beforeEach } from 'vitest';
import { exportApi } from './export.api';

const fetchMock = vi.fn();

vi.mock('../config/env', () => ({
  config: {
    apiUrl: 'http://localhost:8080/api',
  },
}));

vi.mock('../lib/auth', () => ({
  auth: {
    getHeaders: vi.fn(() => ({ Authorization: 'Bearer token' })),
    getToken: vi.fn(() => 'token'),
  },
}));

describe('exportApi', () => {
  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
  });

  it('fetches JSON export with auth headers', async () => {
    fetchMock.mockResolvedValue({ json: () => Promise.resolve({ ok: true }) });
    const result = await exportApi.json();
    expect(result).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/export/json', {
      headers: { Authorization: 'Bearer token' },
    });
  });

  it('posts JSON import payload', async () => {
    fetchMock.mockResolvedValue({ json: () => Promise.resolve({ imported: true }) });
    const payload = { incomes: [] };
    const result = await exportApi.importJson(payload);
    expect(result).toEqual({ imported: true });
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/export/import/json', {
      method: 'POST',
      headers: { Authorization: 'Bearer token' },
      body: JSON.stringify(payload),
    });
  });

  it('builds export URLs with token', () => {
    expect(exportApi.getPdfUrl()).toBe('http://localhost:8080/api/export/pdf?token=token');
    expect(exportApi.getExcelUrl()).toBe('http://localhost:8080/api/export/excel?token=token');
  });
});
