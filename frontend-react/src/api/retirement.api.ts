import { request } from '../lib/api-client';

export const retirementApi = {
  calculate: (scenario: any) => request<any>('/retirement/calculate', {
    method: 'POST',
    body: JSON.stringify(scenario),
  }),
  getMaturing: (currentAge: number, retirementAge: number) =>
    request<any>(`/retirement/maturing?currentAge=${currentAge}&retirementAge=${retirementAge}`),
  getStrategy: () => request<any>('/retirement/strategy'),
  saveStrategy: (strategy: any) => request<any>('/retirement/strategy', {
    method: 'POST',
    body: JSON.stringify(strategy),
  }),
};
