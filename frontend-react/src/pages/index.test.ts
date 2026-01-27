import { describe, it, expect } from 'vitest';
import * as Pages from './index';

describe('Pages index', () => {
  // Public/Static Pages
  it('exports Landing', () => {
    expect(Pages.Landing).toBeDefined();
  });

  it('exports About', () => {
    expect(Pages.About).toBeDefined();
  });

  it('exports Privacy', () => {
    expect(Pages.Privacy).toBeDefined();
  });

  it('exports Terms', () => {
    expect(Pages.Terms).toBeDefined();
  });

  it('exports Pricing', () => {
    expect(Pages.Pricing).toBeDefined();
  });

  it('exports Features', () => {
    expect(Pages.Features).toBeDefined();
  });

  it('exports Disclaimer', () => {
    expect(Pages.Disclaimer).toBeDefined();
  });

  it('exports ErrorPage', () => {
    expect(Pages.ErrorPage).toBeDefined();
  });

  // Home - Smart route
  it('exports Home', () => {
    expect(Pages.Home).toBeDefined();
  });

  // Main Pages
  it('exports Dashboard', () => {
    expect(Pages.Dashboard).toBeDefined();
  });

  it('exports Investments', () => {
    expect(Pages.Investments).toBeDefined();
  });

  it('exports Login', () => {
    expect(Pages.Login).toBeDefined();
  });

  // Data Entry Pages
  it('exports Income', () => {
    expect(Pages.Income).toBeDefined();
  });

  it('exports Loans', () => {
    expect(Pages.Loans).toBeDefined();
  });

  it('exports Insurance', () => {
    expect(Pages.Insurance).toBeDefined();
  });

  it('exports Expenses', () => {
    expect(Pages.Expenses).toBeDefined();
  });

  it('exports Goals', () => {
    expect(Pages.Goals).toBeDefined();
  });

  it('exports Family', () => {
    expect(Pages.Family).toBeDefined();
  });

  // Analysis Pages
  it('exports Calendar', () => {
    expect(Pages.Calendar).toBeDefined();
  });

  it('exports Retirement', () => {
    expect(Pages.Retirement).toBeDefined();
  });

  it('exports InsuranceRecommendations', () => {
    expect(Pages.InsuranceRecommendations).toBeDefined();
  });

  it('exports Simulation', () => {
    expect(Pages.Simulation).toBeDefined();
  });

  it('exports Reports', () => {
    expect(Pages.Reports).toBeDefined();
  });

  // Admin & Account Pages
  it('exports Admin', () => {
    expect(Pages.Admin).toBeDefined();
  });

  it('exports Account', () => {
    expect(Pages.Account).toBeDefined();
  });

  it('exports Settings', () => {
    expect(Pages.Settings).toBeDefined();
  });

  it('exports Preferences', () => {
    expect(Pages.Preferences).toBeDefined();
  });
});
