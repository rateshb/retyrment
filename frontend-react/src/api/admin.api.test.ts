import { describe, it, expect, vi, beforeEach } from 'vitest';
import { adminApi } from './admin.api';

const requestMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

describe('adminApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
  });

  it('gets users', async () => {
    requestMock.mockResolvedValue({ users: [] });
    const result = await adminApi.getUsers();
    expect(result).toEqual({ users: [] });
    expect(requestMock).toHaveBeenCalledWith('/admin/users');
  });

  it('gets a single user', async () => {
    requestMock.mockResolvedValue({ id: 'u1' });
    const result = await adminApi.getUser('u1');
    expect(result).toEqual({ id: 'u1' });
    expect(requestMock).toHaveBeenCalledWith('/admin/users/u1');
  });

  it('searches users by email', async () => {
    requestMock.mockResolvedValue({ users: [] });
    await adminApi.searchUsers('test@example.com');
    expect(requestMock).toHaveBeenCalledWith('/admin/users/search?email=test@example.com');
  });

  it('updates role', async () => {
    requestMock.mockResolvedValue({ ok: true });
    await adminApi.updateRole('u1', { role: 'PRO', durationDays: 7 });
    expect(requestMock).toHaveBeenCalledWith('/admin/users/u1/role', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify({ role: 'PRO', durationDays: 7 }),
    }));
  });

  it('deletes user', async () => {
    requestMock.mockResolvedValue({ ok: true });
    await adminApi.deleteUser('u2');
    expect(requestMock).toHaveBeenCalledWith('/admin/users/u2', { method: 'DELETE' });
  });

  it('extends trial', async () => {
    requestMock.mockResolvedValue({ ok: true });
    await adminApi.extendTrial('u3', 14);
    expect(requestMock).toHaveBeenCalledWith('/admin/users/u3/extend-trial', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify({ days: 14 }),
    }));
  });

  it('gets feature access', async () => {
    requestMock.mockResolvedValue({ features: {} });
    const result = await adminApi.getFeatureAccess('u4');
    expect(result).toEqual({ features: {} });
    expect(requestMock).toHaveBeenCalledWith('/admin/users/u4/features');
  });

  it('updates user features', async () => {
    requestMock.mockResolvedValue({ ok: true });
    const features = { reportsPage: true };
    await adminApi.updateUserFeatures('u5', features);
    expect(requestMock).toHaveBeenCalledWith('/admin/users/u5/features', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify(features),
    }));
  });
});
