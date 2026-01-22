import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, CardContent, Button, toast } from '../components/ui';
import { api } from '../lib/api';
import { useAuthStore } from '../stores/authStore';
import { User, Mail, Shield, Calendar, Trash2, AlertTriangle } from 'lucide-react';

export function Account() {
  const { user, features } = useAuthStore();
  const queryClient = useQueryClient();
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteConfirmation, setDeleteConfirmation] = useState('');

  const { data: dataSummary } = useQuery({
    queryKey: ['user-data-summary'],
    queryFn: api.userData.getSummary,
  });

  const deleteMutation = useMutation({
    mutationFn: () => api.userData.deleteAll('DELETE_ALL_DATA'),
    onSuccess: () => {
      toast.success('All data deleted successfully');
      setShowDeleteModal(false);
      setDeleteConfirmation('');
      queryClient.invalidateQueries();
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const handleDelete = () => {
    if (deleteConfirmation === 'DELETE_ALL_DATA') {
      deleteMutation.mutate();
    }
  };

  return (
    <MainLayout
      title="My Account"
      subtitle="Manage your account settings"
    >
      {/* Profile Card */}
      <Card className="mb-6">
        <CardContent>
          <div className="flex items-center gap-6">
            <div className="w-20 h-20 rounded-full bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white text-3xl font-bold">
              {user?.name?.charAt(0) || user?.email?.charAt(0) || 'U'}
            </div>
            <div>
              <h2 className="text-xl font-semibold text-slate-800">{user?.name || 'User'}</h2>
              <p className="text-slate-500">{user?.email}</p>
              <div className="flex items-center gap-2 mt-2">
                <span className={`px-3 py-1 text-sm rounded-full font-medium ${
                  user?.role === 'ADMIN' ? 'bg-purple-100 text-purple-700' :
                  user?.role === 'PRO' ? 'bg-amber-100 text-amber-700' :
                  'bg-slate-100 text-slate-700'
                }`}>
                  {user?.role || 'FREE'}
                </span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Account Info */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4">Account Information</h3>
            <div className="space-y-4">
              <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
                <User className="text-slate-400" size={20} />
                <div>
                  <p className="text-sm text-slate-500">Full Name</p>
                  <p className="font-medium text-slate-800">{user?.name || 'Not set'}</p>
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
                <Mail className="text-slate-400" size={20} />
                <div>
                  <p className="text-sm text-slate-500">Email</p>
                  <p className="font-medium text-slate-800">{user?.email}</p>
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
                <Shield className="text-slate-400" size={20} />
                <div>
                  <p className="text-sm text-slate-500">Role</p>
                  <p className="font-medium text-slate-800">{user?.role || 'FREE'}</p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Data Summary */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4">Your Data</h3>
            <div className="grid grid-cols-2 gap-3">
              {[
                { label: 'Income Sources', value: dataSummary?.income || 0 },
                { label: 'Investments', value: dataSummary?.investments || 0 },
                { label: 'Loans', value: dataSummary?.loans || 0 },
                { label: 'Insurance Policies', value: dataSummary?.insurance || 0 },
                { label: 'Expenses', value: dataSummary?.expenses || 0 },
                { label: 'Goals', value: dataSummary?.goals || 0 },
                { label: 'Family Members', value: dataSummary?.familyMembers || 0 },
                { label: 'Total Records', value: dataSummary?.total || 0, highlight: true },
              ].map(({ label, value, highlight }) => (
                <div 
                  key={label} 
                  className={`p-3 rounded-lg ${highlight ? 'bg-primary-50 border border-primary-200' : 'bg-slate-50'}`}
                >
                  <p className="text-xs text-slate-500">{label}</p>
                  <p className={`text-xl font-bold ${highlight ? 'text-primary-700' : 'text-slate-800'}`}>{value}</p>
                </div>
              ))}
            </div>
            <p className="text-xs text-slate-500 mt-3">
              Note: Total records can include preferences and retirement strategies, which are not listed above.
            </p>
          </CardContent>
        </Card>

        {/* Feature Access */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-slate-800 mb-4">Feature Access</h3>
            <div className="space-y-2">
              {features && Object.entries(features)
                .filter(([key]) => key.endsWith('Page') || key.endsWith('Tab'))
                .slice(0, 10)
                .map(([key, value]) => (
                  <div key={key} className="flex justify-between items-center p-2 hover:bg-slate-50 rounded">
                    <span className="text-sm text-slate-600 capitalize">
                      {key.replace(/([A-Z])/g, ' $1').trim()}
                    </span>
                    <span className={`text-xs px-2 py-1 rounded ${value ? 'bg-success-100 text-success-700' : 'bg-slate-100 text-slate-500'}`}>
                      {value ? 'Enabled' : 'Disabled'}
                    </span>
                  </div>
                ))}
            </div>
          </CardContent>
        </Card>

        {/* Danger Zone */}
        <Card className="border-danger-200">
          <CardContent>
            <h3 className="text-lg font-semibold text-danger-700 mb-4 flex items-center gap-2">
              <AlertTriangle size={20} /> Danger Zone
            </h3>
            <p className="text-sm text-slate-600 mb-4">
              Permanently delete all your financial data. This action cannot be undone.
            </p>
            <Button 
              variant="danger" 
              onClick={() => setShowDeleteModal(true)}
              className="w-full"
            >
              <Trash2 size={18} className="mr-2" /> Delete All Data
            </Button>
          </CardContent>
        </Card>
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full mx-4 max-h-[85vh] overflow-hidden flex flex-col">
            <div className="text-center mb-4">
              <div className="text-4xl mb-2">⚠️</div>
              <h2 className="text-xl font-bold text-danger-700 mb-1">Delete All Data</h2>
              <p className="text-sm text-slate-600">This action cannot be undone!</p>
            </div>

            <div className="px-6 pb-4 overflow-y-auto">
              <div className="mb-4 p-3 bg-danger-50 rounded-lg text-sm">
                <p className="text-danger-700 font-medium">This will permanently delete:</p>
                <div className="mt-2 grid grid-cols-2 gap-2 text-danger-700">
                  <div>Income: <strong>{dataSummary?.income || 0}</strong></div>
                  <div>Investments: <strong>{dataSummary?.investments || 0}</strong></div>
                  <div>Loans: <strong>{dataSummary?.loans || 0}</strong></div>
                  <div>Insurance: <strong>{dataSummary?.insurance || 0}</strong></div>
                  <div>Expenses: <strong>{dataSummary?.expenses || 0}</strong></div>
                  <div>Goals: <strong>{dataSummary?.goals || 0}</strong></div>
                  <div>Family: <strong>{dataSummary?.familyMembers || 0}</strong></div>
                  <div>Prefs/Strategy: <strong>Yes</strong></div>
                </div>
                <p className="text-danger-800 font-bold mt-2">
                  Total: {dataSummary?.total || 0} records will be deleted
                </p>
              </div>

              <div className="mb-4 p-3 bg-success-50 rounded-lg text-sm">
                <p className="text-success-700 font-medium">✓ What will be kept:</p>
                <ul className="mt-2 space-y-1 text-success-600">
                  <li>• Your account login access</li>
                  <li>• Your email and name</li>
                  <li>• Your subscription status</li>
                </ul>
              </div>

              <div className="mb-4">
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Type DELETE_ALL_DATA to confirm
                </label>
                <input
                  type="text"
                  value={deleteConfirmation}
                  onChange={e => setDeleteConfirmation(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-danger-500"
                  placeholder="DELETE_ALL_DATA"
                />
              </div>
            </div>

            <div className="flex gap-3 px-6 pb-6">
              <Button 
                variant="secondary" 
                onClick={() => { setShowDeleteModal(false); setDeleteConfirmation(''); }}
                className="flex-1"
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                onClick={handleDelete}
                disabled={deleteConfirmation !== 'DELETE_ALL_DATA'}
                isLoading={deleteMutation.isPending}
                className="flex-1"
              >
                Delete All
              </Button>
            </div>
          </div>
        </div>
      )}
    </MainLayout>
  );
}

export default Account;
