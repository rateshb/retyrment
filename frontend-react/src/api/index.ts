// Central API exports
export { authApi } from './auth.api';
export { incomeApi } from './income.api';
export { investmentsApi } from './investments.api';
export { insuranceApi } from './insurance.api';
export { loansApi } from './loans.api';
export { expensesApi } from './expenses.api';
export { goalsApi } from './goals.api';
export { familyApi } from './family.api';
export { analysisApi } from './analysis.api';
export { retirementApi } from './retirement.api';
export { insuranceRecommendationsApi } from './insurance-recommendations.api';
export { simulationApi } from './simulation.api';
export { userDataApi } from './user-data.api';
export { adminApi } from './admin.api';
export { settingsApi } from './settings.api';
export { exportApi } from './export.api';

// Re-export types and auth for convenience
export * from '../lib/types';
export * from '../lib/auth';
export { ApiError, request } from '../lib/api-client';
