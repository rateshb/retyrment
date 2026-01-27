import { describe, it, expect } from 'vitest';

// This test file specifically checks that all page components can be imported successfully
// It will catch any import/export issues from the API refactoring

describe('Page Component Imports', () => {
  it('should import Dashboard without errors', async () => {
    const module = await import('./Dashboard');
    expect(module.Dashboard).toBeDefined();
  }, 30000);

  it('should import Income without errors', async () => {
    const module = await import('./Income');
    expect(module.Income).toBeDefined();
  }, 10000);

  it('should import Expenses without errors', async () => {
    const module = await import('./Expenses');
    expect(module.Expenses).toBeDefined();
  }, 10000);

  it('should import Investments without errors', async () => {
    const module = await import('./Investments');
    expect(module.Investments).toBeDefined();
  }, 10000);

  it('should import Insurance without errors', async () => {
    const module = await import('./Insurance');
    expect(module.Insurance).toBeDefined();
  }, 10000);

  it('should import Loans without errors', async () => {
    const module = await import('./Loans');
    expect(module.Loans).toBeDefined();
  }, 10000);

  it('should import Goals without errors', async () => {
    const module = await import('./Goals');
    expect(module.Goals).toBeDefined();
  }, 10000);

  it('should import Family without errors', async () => {
    const module = await import('./Family');
    expect(module.Family).toBeDefined();
  }, 10000);

  it('should import Calendar without errors', async () => {
    const module = await import('./Calendar');
    expect(module.Calendar).toBeDefined();
  }, 10000);

  it('should import Retirement without errors', async () => {
    const module = await import('./Retirement');
    expect(module.Retirement).toBeDefined();
  }, 10000);

  it('should import InsuranceRecommendations without errors', async () => {
    const module = await import('./InsuranceRecommendations');
    expect(module.InsuranceRecommendations).toBeDefined();
  }, 10000);

  it('should import Account without errors', async () => {
    const module = await import('./Account');
    expect(module.Account).toBeDefined();
  }, 10000);

  it('should import Admin without errors', async () => {
    const module = await import('./Admin');
    expect(module.Admin).toBeDefined();
  }, 10000);

  it('should import Reports without errors', async () => {
    const module = await import('./Reports');
    expect(module.Reports).toBeDefined();
  }, 10000);

  it('should import Settings without errors', async () => {
    const module = await import('./Settings');
    expect(module.Settings).toBeDefined();
  }, 10000);

  it('should import Simulation without errors', async () => {
    const module = await import('./Simulation');
    expect(module.Simulation).toBeDefined();
  }, 10000);
});
