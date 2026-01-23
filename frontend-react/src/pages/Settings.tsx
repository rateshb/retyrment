import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, CardContent, Button, Input, toast } from '../components/ui';
import { Settings as SettingsIcon, Save, RotateCcw } from 'lucide-react';
import { settingsApi } from '../lib/api';

interface AppSettings {
  currentAge: number;
  retirementAge: number;
  lifeExpectancy: number;
  inflationRate: number;
  epfReturn: number;
  ppfReturn: number;
  mfEquityReturn: number;
  mfDebtReturn: number;
  fdReturn: number;
  emergencyFundMonths: number;
  sipStepup: number;
}

const defaultSettings: AppSettings = {
  currentAge: 35,
  retirementAge: 60,
  lifeExpectancy: 85,
  inflationRate: 6.0,
  epfReturn: 8.15,
  ppfReturn: 7.1,
  mfEquityReturn: 12.0,
  mfDebtReturn: 7.0,
  fdReturn: 6.5,
  emergencyFundMonths: 6,
  sipStepup: 10,
};

export function Settings() {
  const queryClient = useQueryClient();
  const [settings, setSettings] = useState<AppSettings>(defaultSettings);

  // Load settings from backend
  const { data: serverSettings, isLoading } = useQuery({
    queryKey: ['user-settings'],
    queryFn: settingsApi.get,
  });

  useEffect(() => {
    if (serverSettings) {
      setSettings({ ...defaultSettings, ...serverSettings });
    }
  }, [serverSettings]);

  // Save mutation
  const saveMutation = useMutation({
    mutationFn: (data: AppSettings) => settingsApi.update(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user-settings'] });
      toast.success('Settings saved successfully');
    },
    onError: () => {
      toast.error('Failed to save settings');
    },
  });

  const handleSave = () => {
    saveMutation.mutate(settings);
  };

  const handleReset = () => {
    setSettings(defaultSettings);
    saveMutation.mutate(defaultSettings);
    toast.info('Settings reset to defaults');
  };

  return (
    <MainLayout
      title="Settings"
      subtitle="Configure your financial assumptions"
    >
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Age & Timeline */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4 flex items-center gap-2">
              <SettingsIcon size={20} /> Age & Timeline
            </h3>
            <div className="space-y-4">
              <Input
                label="Current Age"
                type="number"
                value={settings.currentAge}
                onChange={e => setSettings({ ...settings, currentAge: Number(e.target.value) })}
                helperText="Your current age in years"
              />
              <Input
                label="Retirement Age"
                type="number"
                value={settings.retirementAge}
                onChange={e => setSettings({ ...settings, retirementAge: Number(e.target.value) })}
                helperText="Age at which you plan to retire"
              />
              <Input
                label="Life Expectancy"
                type="number"
                value={settings.lifeExpectancy}
                onChange={e => setSettings({ ...settings, lifeExpectancy: Number(e.target.value) })}
                helperText="Expected lifespan for planning purposes"
              />
            </div>
          </CardContent>
        </Card>

        {/* Return Rates */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4">Expected Returns (%)</h3>
            <div className="space-y-4">
              <Input
                label="EPF Return"
                type="number"
                step="0.01"
                value={settings.epfReturn}
                onChange={e => setSettings({ ...settings, epfReturn: Number(e.target.value) })}
                helperText="Current: 8.15% (2024)"
              />
              <Input
                label="PPF Return"
                type="number"
                step="0.01"
                value={settings.ppfReturn}
                onChange={e => setSettings({ ...settings, ppfReturn: Number(e.target.value) })}
                helperText="Current: 7.1% (2024)"
              />
              <Input
                label="Equity MF Return"
                type="number"
                step="0.01"
                value={settings.mfEquityReturn}
                onChange={e => setSettings({ ...settings, mfEquityReturn: Number(e.target.value) })}
                helperText="Long-term equity expected return"
              />
              <Input
                label="Debt MF Return"
                type="number"
                step="0.01"
                value={settings.mfDebtReturn}
                onChange={e => setSettings({ ...settings, mfDebtReturn: Number(e.target.value) })}
                helperText="Debt fund expected return"
              />
              <Input
                label="FD Return"
                type="number"
                step="0.01"
                value={settings.fdReturn}
                onChange={e => setSettings({ ...settings, fdReturn: Number(e.target.value) })}
                helperText="Fixed deposit interest rate"
              />
              <Input
                label="SIP Step-up (%)"
                type="number"
                step="1"
                value={settings.sipStepup}
                onChange={e => setSettings({ ...settings, sipStepup: Number(e.target.value) })}
                helperText="Annual increase in SIP amount"
              />
            </div>
          </CardContent>
        </Card>

        {/* Inflation & Emergency */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4">Inflation & Safety</h3>
            <div className="space-y-4">
              <Input
                label="Inflation Rate (%)"
                type="number"
                step="0.1"
                value={settings.inflationRate}
                onChange={e => setSettings({ ...settings, inflationRate: Number(e.target.value) })}
                helperText="Expected long-term inflation"
              />
              <Input
                label="Emergency Fund (months)"
                type="number"
                value={settings.emergencyFundMonths}
                onChange={e => setSettings({ ...settings, emergencyFundMonths: Number(e.target.value) })}
                helperText="Recommended: 6-12 months of expenses"
              />
            </div>
          </CardContent>
        </Card>

        {/* Info Card */}
        <Card className="bg-primary-50 border-primary-200">
          <CardContent>
            <h3 className="text-lg font-semibold text-primary-800 mb-4">💡 Tips</h3>
            <ul className="space-y-3 text-sm text-primary-700">
              <li className="flex items-start gap-2">
                <span className="mt-1">•</span>
                <span>Use conservative estimates for better planning. Markets may not always perform as expected.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="mt-1">•</span>
                <span>Inflation of 6-7% is typical for India. Higher for education and healthcare.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="mt-1">•</span>
                <span>Review and update these assumptions annually based on economic conditions.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="mt-1">•</span>
                <span>Life expectancy should account for improving healthcare. Plan for 85-90 years.</span>
              </li>
            </ul>
          </CardContent>
        </Card>
      </div>

      {/* Action Buttons */}
      <div className="flex justify-end gap-4 mt-6">
        <Button variant="secondary" onClick={handleReset} isLoading={saveMutation.isPending}>
          <RotateCcw size={18} className="mr-2" /> Reset to Defaults
        </Button>
        <Button onClick={handleSave} isLoading={saveMutation.isPending}>
          <Save size={18} className="mr-2" /> Save Settings
        </Button>
      </div>
    </MainLayout>
  );
}

export default Settings;
