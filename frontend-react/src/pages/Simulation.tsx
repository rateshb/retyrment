import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, CardContent, Button, Input, toast } from '../components/ui';
import { simulationApi } from '../lib/api';
import { formatCurrency } from '../lib/utils';
import { useAuthStore } from '../stores/authStore';
import { Dice5, TrendingUp, AlertTriangle, CheckCircle, Play } from 'lucide-react';

export function Simulation() {
  const { features } = useAuthStore();
  const [simulations, setSimulations] = useState(1000);
  const [years, setYears] = useState(25);
  const [results, setResults] = useState<any>(null);

  const simulationMutation = useMutation({
    mutationFn: () => simulationApi.run(simulations, years),
    onSuccess: (data) => {
      setResults(data);
      toast.success(`Completed ${simulations} simulations`);
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const canRunSimulation = features?.canRunSimulation;

  if (!canRunSimulation) {
    return (
      <MainLayout title="Monte Carlo Simulation" subtitle="Simulate your financial future">
        <Card className="bg-warning-50 border-warning-200">
          <CardContent className="text-center py-12">
            <Dice5 className="mx-auto text-warning-500 mb-4" size={48} />
            <h3 className="text-lg font-semibold text-warning-800 mb-2">Simulation Access Restricted</h3>
            <p className="text-warning-600">Contact your administrator to enable simulation access.</p>
          </CardContent>
        </Card>
      </MainLayout>
    );
  }

  return (
    <MainLayout title="Monte Carlo Simulation" subtitle="Simulate your financial future">
      {/* Configuration */}
      <Card className="mb-6">
        <CardContent>
          <h3 className="text-lg font-semibold text-slate-800 mb-4">Simulation Parameters</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <Input
              label="Number of Simulations"
              type="number"
              value={simulations}
              onChange={e => setSimulations(Number(e.target.value))}
              min={100}
              max={10000}
              helperText="More simulations = more accurate results"
            />
            <Input
              label="Projection Years"
              type="number"
              value={years}
              onChange={e => setYears(Number(e.target.value))}
              min={5}
              max={50}
              helperText="Number of years to simulate"
            />
            <div className="flex items-end">
              <Button 
                onClick={() => simulationMutation.mutate()}
                isLoading={simulationMutation.isPending}
                className="w-full"
              >
                <Play size={18} className="mr-2" /> Run Simulation
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Results */}
      {results && (
        <>
          {/* Success Rate */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <Card className={results.successRate >= 80 ? 'bg-success-50 border-success-200' : results.successRate >= 50 ? 'bg-warning-50 border-warning-200' : 'bg-danger-50 border-danger-200'}>
              <CardContent>
                <div className="flex items-center gap-4">
                  <div className="p-3 rounded-xl bg-white shadow-sm">
                    {results.successRate >= 80 ? (
                      <CheckCircle className="text-success-500" size={24} />
                    ) : (
                      <AlertTriangle className="text-warning-500" size={24} />
                    )}
                  </div>
                  <div>
                    <p className="text-sm text-slate-600">Success Rate</p>
                    <p className="text-3xl font-bold">{results.successRate?.toFixed(1)}%</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="bg-primary-50 border-primary-200">
              <CardContent>
                <div className="flex items-center gap-4">
                  <div className="p-3 rounded-xl bg-white shadow-sm">
                    <TrendingUp className="text-primary-500" size={24} />
                  </div>
                  <div>
                    <p className="text-sm text-primary-600">Median Outcome</p>
                    <p className="text-2xl font-bold text-primary-700">{formatCurrency(results.percentiles?.p50 || 0)}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="bg-slate-50 border-slate-200">
              <CardContent>
                <div className="flex items-center gap-4">
                  <div className="p-3 rounded-xl bg-white shadow-sm">
                    <Dice5 className="text-slate-500" size={24} />
                  </div>
                  <div>
                    <p className="text-sm text-slate-600">Simulations Run</p>
                    <p className="text-2xl font-bold text-slate-700">{simulations.toLocaleString()}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Percentile Distribution */}
          <Card className="mb-6">
            <CardContent>
              <h3 className="text-lg font-semibold text-slate-800 mb-4">Outcome Distribution</h3>
              <div className="space-y-4">
                {[
                  { label: 'Worst Case (10th percentile)', key: 'p10', color: 'bg-danger-500' },
                  { label: 'Conservative (25th percentile)', key: 'p25', color: 'bg-warning-500' },
                  { label: 'Median (50th percentile)', key: 'p50', color: 'bg-primary-500' },
                  { label: 'Optimistic (75th percentile)', key: 'p75', color: 'bg-success-400' },
                  { label: 'Best Case (90th percentile)', key: 'p90', color: 'bg-success-600' },
                ].map(({ label, key, color }) => {
                  const value = results.percentiles?.[key] || 0;
                  const maxValue = results.percentiles?.p90 || 1;
                  const width = (value / maxValue) * 100;

                  return (
                    <div key={key}>
                      <div className="flex justify-between text-sm mb-1">
                        <span className="text-slate-600">{label}</span>
                        <span className="font-medium text-slate-800">{formatCurrency(value)}</span>
                      </div>
                      <div className="h-3 bg-slate-200 rounded-full overflow-hidden">
                        <div 
                          className={`h-full ${color} transition-all`}
                          style={{ width: `${width}%` }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            </CardContent>
          </Card>

          {/* Interpretation */}
          <Card>
            <CardContent>
              <h3 className="text-lg font-semibold text-slate-800 mb-4">What This Means</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="p-4 bg-slate-50 rounded-lg">
                  <h4 className="font-medium text-slate-700 mb-2">Success Rate: {results.successRate?.toFixed(1)}%</h4>
                  <p className="text-sm text-slate-600">
                    {results.successRate >= 80 
                      ? "Excellent! Your financial plan has a high probability of success. Stay the course."
                      : results.successRate >= 50 
                      ? "Your plan has moderate success probability. Consider increasing savings or reducing goals."
                      : "Your plan needs adjustment. Consider significant changes to reach your goals."}
                  </p>
                </div>
                <div className="p-4 bg-slate-50 rounded-lg">
                  <h4 className="font-medium text-slate-700 mb-2">Range of Outcomes</h4>
                  <p className="text-sm text-slate-600">
                    Your corpus could range from {formatCurrency(results.percentiles?.p10 || 0)} (worst case) 
                    to {formatCurrency(results.percentiles?.p90 || 0)} (best case), 
                    with a median expected value of {formatCurrency(results.percentiles?.p50 || 0)}.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </>
      )}

      {!results && (
        <Card>
          <CardContent className="text-center py-12">
            <Dice5 className="mx-auto text-slate-300 mb-4" size={64} />
            <h3 className="text-lg font-semibold text-slate-600 mb-2">No Results Yet</h3>
            <p className="text-slate-400">Configure your parameters and click "Run Simulation" to see probabilistic outcomes.</p>
          </CardContent>
        </Card>
      )}
    </MainLayout>
  );
}

export default Simulation;
