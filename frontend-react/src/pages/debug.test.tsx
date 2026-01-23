import { describe, it, expect } from 'vitest';

// Debug test to isolate the issue
describe('Debug Test', () => {
  it('should pass a simple test', () => {
    expect(1 + 1).toBe(2);
  });

  it('should import vitest correctly', () => {
    expect(vitest).toBeDefined();
  });

  it('should test basic import without mocking', async () => {
    // Try to import without any mocking first
    try {
      const module = await import('./Dashboard');
      expect(module).toBeDefined();
      console.log('Dashboard import successful');
    } catch (error) {
      console.error('Dashboard import failed:', error);
      throw error;
    }
  }, 15000);
});
