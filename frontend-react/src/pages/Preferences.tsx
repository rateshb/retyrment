import { useState } from 'react';
import { MainLayout } from '../components/Layout';
import { Card, CardContent, Button, toast } from '../components/ui';
import { Palette, Bell, Eye, Save } from 'lucide-react';

interface UserPreferences {
  theme: 'light' | 'dark' | 'system';
  currency: string;
  dateFormat: string;
  numberFormat: string;
  showEmoji: boolean;
  compactNumbers: boolean;
  notifications: {
    email: boolean;
    browser: boolean;
    renewalReminders: boolean;
    goalAlerts: boolean;
  };
  dashboard: {
    showNetWorth: boolean;
    showRecommendations: boolean;
    showUpcomingEvents: boolean;
    showGoalProgress: boolean;
  };
}

const defaultPreferences: UserPreferences = {
  theme: 'light',
  currency: 'INR',
  dateFormat: 'DD/MM/YYYY',
  numberFormat: 'Indian',
  showEmoji: true,
  compactNumbers: true,
  notifications: {
    email: true,
    browser: false,
    renewalReminders: true,
    goalAlerts: true,
  },
  dashboard: {
    showNetWorth: true,
    showRecommendations: true,
    showUpcomingEvents: true,
    showGoalProgress: true,
  },
};

export function Preferences() {
  const [prefs, setPrefs] = useState<UserPreferences>(defaultPreferences);

  const handleSave = () => {
    localStorage.setItem('retyrment_preferences', JSON.stringify(prefs));
    toast.success('Preferences saved successfully');
  };

  return (
    <MainLayout
      title="Preferences"
      subtitle="Customize your experience"
    >
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Display Settings */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4 flex items-center gap-2">
              <Palette size={20} /> Display
            </h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">Theme</label>
                <div className="flex gap-2">
                  {['light', 'dark', 'system'].map((theme) => (
                    <button
                      key={theme}
                      onClick={() => setPrefs({ ...prefs, theme: theme as any })}
                      className={`px-4 py-2 rounded-lg border capitalize transition-colors ${
                        prefs.theme === theme
                          ? 'border-primary-500 bg-primary-50 text-primary-700'
                          : 'border-slate-200 hover:bg-slate-50'
                      }`}
                    >
                      {theme}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">Currency</label>
                <select
                  value={prefs.currency}
                  onChange={e => setPrefs({ ...prefs, currency: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                  <option value="INR">₹ Indian Rupee (INR)</option>
                  <option value="USD">$ US Dollar (USD)</option>
                  <option value="EUR">€ Euro (EUR)</option>
                  <option value="GBP">£ British Pound (GBP)</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">Number Format</label>
                <select
                  value={prefs.numberFormat}
                  onChange={e => setPrefs({ ...prefs, numberFormat: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                  <option value="Indian">Indian (1,00,000)</option>
                  <option value="International">International (100,000)</option>
                </select>
              </div>

              <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
                <div>
                  <p className="font-medium text-slate-700">Compact Numbers</p>
                  <p className="text-sm text-slate-500">Show 10L instead of 10,00,000</p>
                </div>
                <input
                  type="checkbox"
                  checked={prefs.compactNumbers}
                  onChange={e => setPrefs({ ...prefs, compactNumbers: e.target.checked })}
                  className="w-5 h-5 text-primary-600 rounded"
                />
              </div>

              <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
                <div>
                  <p className="font-medium text-slate-700">Show Emoji</p>
                  <p className="text-sm text-slate-500">Display emoji icons in the interface</p>
                </div>
                <input
                  type="checkbox"
                  checked={prefs.showEmoji}
                  onChange={e => setPrefs({ ...prefs, showEmoji: e.target.checked })}
                  className="w-5 h-5 text-primary-600 rounded"
                />
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Notifications */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4 flex items-center gap-2">
              <Bell size={20} /> Notifications
            </h3>
            <div className="space-y-3">
              {[
                { key: 'email', label: 'Email Notifications', desc: 'Receive updates via email' },
                { key: 'browser', label: 'Browser Notifications', desc: 'Desktop push notifications' },
                { key: 'renewalReminders', label: 'Renewal Reminders', desc: 'Insurance & policy renewals' },
                { key: 'goalAlerts', label: 'Goal Alerts', desc: 'Progress updates on goals' },
              ].map(({ key, label, desc }) => (
                <div key={key} className="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
                  <div>
                    <p className="font-medium text-slate-700">{label}</p>
                    <p className="text-sm text-slate-500">{desc}</p>
                  </div>
                  <input
                    type="checkbox"
                    checked={prefs.notifications[key as keyof typeof prefs.notifications]}
                    onChange={e => setPrefs({
                      ...prefs,
                      notifications: { ...prefs.notifications, [key]: e.target.checked }
                    })}
                    className="w-5 h-5 text-primary-600 rounded"
                  />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Dashboard Widgets */}
        <Card className="lg:col-span-2">
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4 flex items-center gap-2">
              <Eye size={20} /> Dashboard Widgets
            </h3>
            <p className="text-slate-500 mb-4">Choose which widgets to display on your dashboard</p>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              {[
                { key: 'showNetWorth', label: 'Net Worth', icon: '💰' },
                { key: 'showRecommendations', label: 'Recommendations', icon: '💡' },
                { key: 'showUpcomingEvents', label: 'Upcoming Events', icon: '📅' },
                { key: 'showGoalProgress', label: 'Goal Progress', icon: '🎯' },
              ].map(({ key, label, icon }) => (
                <label
                  key={key}
                  className={`flex items-center gap-3 p-4 rounded-lg border cursor-pointer transition-colors ${
                    prefs.dashboard[key as keyof typeof prefs.dashboard]
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={prefs.dashboard[key as keyof typeof prefs.dashboard]}
                    onChange={e => setPrefs({
                      ...prefs,
                      dashboard: { ...prefs.dashboard, [key]: e.target.checked }
                    })}
                    className="w-4 h-4 text-primary-600 rounded"
                  />
                  <span className="text-xl">{icon}</span>
                  <span className="text-sm font-medium text-slate-700">{label}</span>
                </label>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Save Button */}
      <div className="flex justify-end mt-6">
        <Button onClick={handleSave}>
          <Save size={18} className="mr-2" /> Save Preferences
        </Button>
      </div>
    </MainLayout>
  );
}

export default Preferences;
