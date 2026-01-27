import { describe, it, expect, vi, beforeEach } from 'vitest';
import { familyApi } from './family.api';

const requestMock = vi.fn();

vi.mock('../lib/api-client', () => ({
  request: (...args: unknown[]) => requestMock(...args),
}));

describe('familyApi', () => {
  beforeEach(() => {
    requestMock.mockReset();
  });

  it('requests all family members', async () => {
    requestMock.mockResolvedValue([]);
    await familyApi.getAll();
    expect(requestMock).toHaveBeenCalledWith('/family');
  });

  it('creates a family member', async () => {
    const member = { name: 'Spouse', relation: 'SPOUSE' };
    requestMock.mockResolvedValue(member);

    await familyApi.create(member as any);

    expect(requestMock).toHaveBeenCalledWith('/family', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(member),
    }));
  });

  it('updates a family member', async () => {
    const member = { name: 'Child', relation: 'CHILD' };
    requestMock.mockResolvedValue(member);

    await familyApi.update('fam-1', member as any);

    expect(requestMock).toHaveBeenCalledWith('/family/fam-1', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify(member),
    }));
  });

  it('deletes a family member', async () => {
    requestMock.mockResolvedValue({});
    await familyApi.delete('fam-2');
    expect(requestMock).toHaveBeenCalledWith('/family/fam-2', { method: 'DELETE' });
  });
});
