import { describe, it, expect, vi, beforeEach } from 'vitest';
import { userDataApi } from './user-data.api';

const requestMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

describe('userDataApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
  });

  it('gets user data summary', async () => {
    requestMock.mockResolvedValue({ totalRecords: 5 });
    const result = await userDataApi.getSummary();
    expect(result).toEqual({ totalRecords: 5 });
    expect(requestMock).toHaveBeenCalledWith('/user/data/summary');
  });

  it('deletes all data with confirmation token', async () => {
    requestMock.mockResolvedValue({ success: true });
    await userDataApi.deleteAll('DELETE_ALL_DATA');
    expect(requestMock).toHaveBeenCalledWith('/user/data/all?confirmation=DELETE_ALL_DATA', {
      method: 'DELETE',
    });
  });
});
