import { request } from '../lib/api-client';

export const simulationApi = {
  run: (simulations: number, years: number) => 
    request<any>(`/analysis/montecarlo?simulations=${simulations}&years=${years}`),
};
