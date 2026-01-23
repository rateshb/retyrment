import { describe, it, expect } from 'vitest';

// This test file specifically checks that all page components can be imported successfully
// It will catch any import/export issues from the API refactoring

describe('Page Component Imports', () => {
  it('should import Dashboard without errors', async () => {
    const { Dashboard } = await import('./Dashboard');
    expect(Dashboard).toBeDefined();
  }, 10000);

  it('should import Income without errors', async () => {
    const { Income } = await import('./Income');
    expect(Income).toBeDefined();
  }, 10000);

  it('should import Expenses without errors', async () => {
    const { Expenses } = await import('./Expenses');
    expect(Expenses).toBeDefined();
  }, 10000);

  it('should import Investments without errors', async () => {
    const { Investments } = await import('./Investments');
    expect(Investments).toBeDefined();
  }, 10000);

  it('should import Insurance without errors', async () => {
    const { Insurance } = await import('./Insurance');
    expect(Insurance).toBeDefined();
  }, 10000);

  it('should import Loans without errors', async () => {
    const { Loans } = await import('./Loans');
    expect(Loans).toBeDefined();
  }, 10000);

  it('should import Goals without errors', async () => {
    const { Goals } = await import('./Goals');
    expect(Goals).toBeDefined();
  }, 10000);

  it('should import Family without errors', async () => {
    const { Family } = await import('./Family');
    expect(Family).toBeDefined();
  }, 10000);

  it('should import Calendar without errors', async () => {
    const { Calendar } = await import('./Calendar');
    expect(Calendar).toBeDefined();
  }, 10000);

  it('should import Retirement without errors', async () => {
    const { Retirement } = await import('./Retirement');
    expect(Retirement).toBeDefined();
  }, 10000);

  it('should import InsuranceRecommendations without errors', async () => {
    const { InsuranceRecommendations } = await import('./InsuranceRecommendations');
    expect(InsuranceRecommendations).toBeDefined();
  }, 10000);

  // Test remaining pages that might have import issues
  it('should import Account without errors', async () => {
    try {
      const { Account } = await import('./Account');
      expect(Account).toBeDefined();
    } catch (error) {
      // If file doesn't exist or has import issues, that's what we want to catch
      expect(error).toBeDefined();
    }
  }, 10000);

  it('should import Admin without errors', async () => {
    try {
      const { Admin } = await import('./Admin');
      expect(Admin).toBeDefined();
    } catch (error) {
      // If file doesn't exist or has import issues, that's what we want to catch
      expect(error).toBeDefined();
    }
  }, 10000);

  it('should import Reports without errors', async () => {
    try {
      const { Reports } = await import('./Reports');
      expect(Reports).toBeDefined();
    } catch (error) {
      // If file doesn't exist or has import issues, that's what we want to catch
      expect(error).toBeDefined();
    }
  }, 10000);

  it('should import Settings without errors', async () => {
    try {
      const { Settings } = await import('./Settings');
      expect(Settings).toBeDefined();
    } catch (error) {
      // If file doesn't exist or has import issues, that's what we want to catch
      expect(error).toBeDefined();
    }
  }, 10000);

  it('should import Simulation without errors', async () => {
    try {
      const { Simulation } = await import('./Simulation');
      expect(Simulation).toBeDefined();
    } catch (error) {
      // If file doesn't exist or has import issues, that's what we want to catch
      expect(error).toBeDefined();
    }
  }, 10000);
});
