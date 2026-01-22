import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, CardContent, Button, Modal, toast } from '../components/ui';
import { useAuthStore } from '../stores/authStore';
import { api } from '../lib/api';
import { Users, Search, Shield, Clock, Check, AlertTriangle, Crown, Trash2 } from 'lucide-react';

// Feature configuration for the modal
const PAGE_FEATURES = [
  { key: 'incomePage', label: 'Income Page', default: true },
  { key: 'investmentPage', label: 'Investment Page', default: true },
  { key: 'loanPage', label: 'Loan Page', default: true },
  { key: 'insurancePage', label: 'Insurance Page', default: true },
  { key: 'expensePage', label: 'Expense Page', default: true },
  { key: 'goalsPage', label: 'Goals Page', default: true },
  { key: 'familyPage', label: 'Family Page', default: true },
  { key: 'calendarPage', label: 'Calendar Page', default: false },
  { key: 'retirementPage', label: 'Retirement Page', default: true },
  { key: 'insuranceRecommendationsPage', label: 'Insurance Recommendations', default: false },
  { key: 'reportsPage', label: 'Reports Page', default: false },
  { key: 'preferencesPage', label: 'Preferences Page', default: false },
  { key: 'settingsPage', label: 'Settings Page', default: true },
  { key: 'accountPage', label: 'My Account Page', default: true },
  { key: 'adminPanel', label: 'Admin Panel', default: false },
];

const RETIREMENT_FEATURES = [
  { key: 'retirementStrategyPlannerTab', label: 'Strategy Planner Tab' },
  { key: 'retirementWithdrawalStrategyTab', label: 'Withdrawal Strategy Tab' },
];

const SIMULATION_FEATURES = [
  { key: 'simulationPage', label: 'Simulation Page Visible' },
  { key: 'canRunSimulation', label: 'Can Run Simulations' },
];

const REPORT_FEATURES = [
  { key: 'canExportPdf', label: 'Export PDF' },
  { key: 'canExportExcel', label: 'Export Excel' },
  { key: 'canExportJson', label: 'Export JSON' },
  { key: 'canImportData', label: 'Import Data' },
];

const ALL_INVESTMENT_TYPES = ['MUTUAL_FUND', 'PPF', 'EPF', 'FD', 'RD', 'REAL_ESTATE', 'STOCK', 'NPS', 'GOLD', 'CRYPTO', 'CASH'];
const ALL_INSURANCE_TYPES = ['VEHICLE', 'PENSION', 'LIFE_SAVINGS', 'HEALTH', 'TERM_LIFE', 'ULIP', 'ENDOWMENT', 'MONEY_BACK'];

interface AdminUser {
  id: string;
  email: string;
  name: string;
  role: string;
  createdAt: string;
  lastLoginAt?: string;
  picture?: string;
  trial?: { active: boolean; daysRemaining: number };
  roleInfo?: { temporary: boolean; expired: boolean; daysRemaining?: number };
}

export function Admin() {
  const { user, fetchFeatures } = useAuthStore();
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  
  // Modal states
  const [roleModalOpen, setRoleModalOpen] = useState(false);
  const [featureModalOpen, setFeatureModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  
  // Role modal state
  const [selectedRole, setSelectedRole] = useState('FREE');
  const [selectedDuration, setSelectedDuration] = useState<number | null>(null);
  const [roleReason, setRoleReason] = useState('');
  
  // Feature modal state
  const [featureAccess, setFeatureAccess] = useState<Record<string, any>>({});

  // Fetch users
  const { data: usersData, isLoading, refetch } = useQuery({
    queryKey: ['admin-users'],
    queryFn: api.admin.getUsers,
    enabled: user?.role === 'ADMIN',
  });

  // Mutations
  const updateRoleMutation = useMutation({
    mutationFn: ({ userId, data }: { userId: string; data: any }) => 
      api.admin.updateRole(userId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      toast.success('Role updated successfully');
      setRoleModalOpen(false);
    },
    onError: (error: Error) => toast.error(error.message || 'Failed to update role'),
  });

  const updateFeaturesMutation = useMutation({
    mutationFn: ({ userId, features }: { userId: string; features: any }) =>
      api.admin.updateUserFeatures(userId, features),
    onSuccess: async (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      toast.success('Feature access updated successfully');
      setFeatureModalOpen(false);
      
      // If updating current user's features or any admin user, refresh features immediately
      if (user && (user.id === variables.userId || user.role === 'ADMIN')) {
        // Clear localStorage cache
        localStorage.removeItem('retyrment_features');
        // Fetch fresh features from server
        await fetchFeatures();
        toast.info('Feature access refreshed. Some pages may require reload to reflect changes.');
      }
    },
    onError: (error: Error) => toast.error(error.message || 'Failed to update features'),
  });

  const deleteUserMutation = useMutation({
    mutationFn: (userId: string) => api.admin.deleteUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      toast.success('User deleted successfully');
    },
    onError: (error: Error) => toast.error(error.message || 'Failed to delete user'),
  });

  const users: AdminUser[] = usersData?.users || [];
  const stats = usersData?.stats || {};

  const filteredUsers = searchTerm 
    ? users.filter(u => 
        u.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
        u.name?.toLowerCase().includes(searchTerm.toLowerCase())
      )
    : users;

  // Open role modal
  const openRoleModal = (adminUser: AdminUser) => {
    setSelectedUser(adminUser);
    setSelectedRole(adminUser.role);
    setSelectedDuration(null);
    setRoleReason('');
    setRoleModalOpen(true);
  };

  // Open feature modal
  const openFeatureModal = async (adminUser: AdminUser) => {
    setSelectedUser(adminUser);
    try {
      const response = await api.admin.getFeatureAccess(adminUser.id);
      const features = response.featureAccess || response || {};
      setFeatureAccess(features);
      setFeatureModalOpen(true);
    } catch (error) {
      toast.error('Failed to load feature access');
    }
  };

  // Save role
  const handleSaveRole = () => {
    if (!selectedUser) return;
    
    // For PRO role with "Permanent" duration, send a long duration (10 years = 3650 days)
    // Backend requires duration for PRO roles
    let durationToSend = selectedDuration;
    if (selectedRole === 'PRO' && selectedDuration === null) {
      durationToSend = 3650; // 10 years for permanent PRO
    }
    
    updateRoleMutation.mutate({
      userId: selectedUser.id,
      data: {
        role: selectedRole,
        durationDays: durationToSend || undefined,
        reason: roleReason || undefined,
      },
    });
  };

  // Save features
  const handleSaveFeatures = () => {
    if (!selectedUser) return;
    updateFeaturesMutation.mutate({
      userId: selectedUser.id,
      features: featureAccess,
    });
  };

  // Delete user
  const handleDeleteUser = (adminUser: AdminUser) => {
    if (!confirm(`Are you sure you want to delete user ${adminUser.email}? This cannot be undone.`)) {
      return;
    }
    deleteUserMutation.mutate(adminUser.id);
  };

  // Toggle feature
  const toggleFeature = (key: string) => {
    setFeatureAccess(prev => ({ ...prev, [key]: !prev[key] }));
  };

  // Toggle investment type
  const toggleInvestmentType = (type: string) => {
    const current = featureAccess.allowedInvestmentTypes || [];
    const updated = current.includes(type)
      ? current.filter((t: string) => t !== type)
      : [...current, type];
    setFeatureAccess(prev => ({ ...prev, allowedInvestmentTypes: updated }));
  };

  // Toggle insurance type
  const toggleInsuranceType = (type: string) => {
    const current = featureAccess.blockedInsuranceTypes || [];
    const updated = current.includes(type)
      ? current.filter((t: string) => t !== type)
      : [...current, type];
    setFeatureAccess(prev => ({ ...prev, blockedInsuranceTypes: updated }));
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString();
  };

  // Access check
  if (user?.role !== 'ADMIN') {
    return (
      <MainLayout title="Admin Panel" subtitle="Access Denied">
        <Card className="bg-danger-50 border-danger-200">
          <CardContent className="text-center py-12">
            <Shield className="mx-auto text-danger-500 mb-4" size={48} />
            <h3 className="text-lg font-semibold text-danger-700 mb-2">Access Denied</h3>
            <p className="text-danger-600">You don't have permission to access the admin panel.</p>
          </CardContent>
        </Card>
      </MainLayout>
    );
  }

  return (
    <MainLayout
      title="Admin Panel"
      subtitle="Manage users and feature access"
    >
      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <Card>
          <CardContent className="flex items-center gap-4">
            <Users className="text-slate-500" size={24} />
            <div>
              <p className="text-sm text-slate-500">Total Users</p>
              <p className="text-2xl font-bold text-slate-800">{usersData?.total || 0}</p>
            </div>
          </CardContent>
        </Card>
        <Card className="bg-slate-50">
          <CardContent className="flex items-center gap-4">
            <Check className="text-slate-500" size={24} />
            <div>
              <p className="text-sm text-slate-500">Free Users</p>
              <p className="text-2xl font-bold text-slate-700">{stats.free || 0}</p>
            </div>
          </CardContent>
        </Card>
        <Card className="bg-amber-50 border-amber-200">
          <CardContent className="flex items-center gap-4">
            <Crown className="text-amber-500" size={24} />
            <div>
              <p className="text-sm text-amber-600">Pro Users</p>
              <p className="text-2xl font-bold text-amber-700">{stats.pro || 0}</p>
            </div>
          </CardContent>
        </Card>
        <Card className="bg-purple-50 border-purple-200">
          <CardContent className="flex items-center gap-4">
            <Shield className="text-purple-500" size={24} />
            <div>
              <p className="text-sm text-purple-600">Admins</p>
              <p className="text-2xl font-bold text-purple-700">{stats.admin || 0}</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Additional Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card className="bg-green-50 border-green-200">
          <CardContent className="flex items-center gap-3">
            <Users className="text-green-500" size={24} />
            <div>
              <p className="text-xl font-bold text-green-600">{users.length || 0}</p>
              <p className="text-xs text-green-600">Total Users</p>
            </div>
          </CardContent>
        </Card>
        <Card className="bg-blue-50 border-blue-200">
          <CardContent className="flex items-center gap-3">
            <Clock className="text-blue-500" size={24} />
            <div>
              <p className="text-xl font-bold text-blue-600">{stats.temporaryRoles || 0}</p>
              <p className="text-xs text-blue-600">Temporary Roles</p>
            </div>
          </CardContent>
        </Card>
        <Card className="bg-orange-50 border-orange-200">
          <CardContent className="flex items-center gap-3">
            <AlertTriangle className="text-orange-500" size={24} />
            <div>
              <p className="text-xl font-bold text-orange-600">{stats.expiringIn7Days || 0}</p>
              <p className="text-xs text-orange-600">Expiring in 7 Days</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Search */}
      <Card className="mb-6">
        <CardContent>
          <div className="flex items-center gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
              <input
                type="text"
                placeholder="Search users by email or name..."
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
            <Button onClick={() => refetch()}>Refresh</Button>
          </div>
        </CardContent>
      </Card>

      {/* Users Table */}
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">User</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Email</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Role</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Created</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Last Login</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-slate-400">Loading...</td>
                </tr>
              ) : filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-slate-400">No users found</td>
                </tr>
              ) : (
                filteredUsers.map((adminUser) => (
                  <tr key={adminUser.id} className="hover:bg-slate-50">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        {adminUser.picture ? (
                          <img src={adminUser.picture} alt="" className="w-8 h-8 rounded-full" />
                        ) : (
                          <div className="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center">
                            <Users size={16} className="text-slate-500" />
                          </div>
                        )}
                        <span className="font-medium text-slate-800">{adminUser.name || 'Unknown'}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-slate-600">{adminUser.email}</td>
                    <td className="px-6 py-4">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className={`px-2 py-1 text-xs rounded-full font-medium ${
                          adminUser.role === 'ADMIN' ? 'bg-purple-100 text-purple-700' :
                          adminUser.role === 'PRO' ? 'bg-amber-100 text-amber-700' :
                          'bg-slate-100 text-slate-600'
                        }`}>
                          {adminUser.role === 'ADMIN' ? '👑 ' : adminUser.role === 'PRO' ? '⭐ ' : ''}{adminUser.role}
                        </span>
                        {adminUser.roleInfo?.temporary && !adminUser.roleInfo.expired && (
                          <span className="px-2 py-0.5 text-xs bg-blue-100 text-blue-700 rounded-full">
                            ⏰ {adminUser.roleInfo.daysRemaining}d left
                          </span>
                        )}
                        {adminUser.roleInfo?.expired && (
                          <span className="px-2 py-0.5 text-xs bg-red-100 text-red-700 rounded-full">
                            ⚠️ Expired
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-500">{formatDate(adminUser.createdAt)}</td>
                    <td className="px-6 py-4 text-sm text-slate-500">{formatDate(adminUser.lastLoginAt)}</td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => openRoleModal(adminUser)}
                          className="text-primary-500 hover:text-primary-700 font-medium text-sm"
                        >
                          Change Role
                        </button>
                        <button
                          onClick={() => openFeatureModal(adminUser)}
                          className="text-blue-500 hover:text-blue-700 font-medium text-sm"
                        >
                          Features
                        </button>
                        <button
                          onClick={() => handleDeleteUser(adminUser)}
                          className="text-slate-400 hover:text-danger-500"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Role Change Modal */}
      <Modal
        isOpen={roleModalOpen}
        onClose={() => setRoleModalOpen(false)}
        title={`Change Role for ${selectedUser?.email}`}
        footer={
          <>
            <Button variant="secondary" onClick={() => setRoleModalOpen(false)}>Cancel</Button>
            <Button onClick={handleSaveRole} isLoading={updateRoleMutation.isPending}>Save Changes</Button>
          </>
        }
      >
        <div className="space-y-4">
          {/* Info Banner */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
            <p className="text-sm text-blue-700">
              <strong>💡 Tip:</strong> To grant a "trial", select <strong>PRO</strong> role with a duration (e.g., 7 days). After expiry, the user will revert to FREE.
            </p>
          </div>

          {/* Role Selection */}
          <div className="space-y-3">
            {[
              { value: 'FREE', label: 'Free', desc: 'Basic features - data entry, projections', color: 'slate' },
              { value: 'PRO', label: '⭐ Pro', desc: 'Premium - recommendations, reports, retirement planning', color: 'amber' },
              { value: 'ADMIN', label: '👑 Admin', desc: 'Full access + user management', color: 'purple' },
            ].map(role => (
              <label 
                key={role.value}
                className={`flex items-center gap-3 p-3 border rounded-lg cursor-pointer hover:bg-${role.color}-50 ${
                  selectedRole === role.value ? `border-${role.color}-500 bg-${role.color}-50` : 'border-slate-200'
                }`}
              >
                <input
                  type="radio"
                  name="role"
                  value={role.value}
                  checked={selectedRole === role.value}
                  onChange={() => setSelectedRole(role.value)}
                  className="w-4 h-4"
                />
                <div>
                  <div className={`font-medium text-${role.color}-700`}>{role.label}</div>
                  <div className="text-xs text-slate-500">{role.desc}</div>
                </div>
              </label>
            ))}
          </div>

          {/* Duration Selection */}
          <div className="border-t pt-4">
            <div className="text-sm font-medium text-slate-700 mb-3">Duration</div>
            <div className="grid grid-cols-4 gap-2 mb-3">
              {[
                { value: null, label: 'Permanent' },
                { value: 7, label: '7 Days' },
                { value: 30, label: '30 Days' },
                { value: 90, label: '90 Days' },
              ].map(opt => (
                <button
                  key={opt.label}
                  type="button"
                  onClick={() => setSelectedDuration(opt.value)}
                  className={`px-3 py-2 text-sm border rounded-lg ${
                    selectedDuration === opt.value 
                      ? 'border-primary-500 bg-primary-50 text-primary-700' 
                      : 'border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {/* Reason */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">Reason (optional)</label>
            <input
              type="text"
              value={roleReason}
              onChange={e => setRoleReason(e.target.value)}
              placeholder="e.g., License purchase, Trial extension..."
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
        </div>
      </Modal>

      {/* Feature Access Modal - Tree Structure */}
      <Modal
        isOpen={featureModalOpen}
        onClose={() => setFeatureModalOpen(false)}
        title={`Manage Features for ${selectedUser?.email}`}
        size="xl"
        footer={
          <>
            <Button variant="secondary" onClick={() => setFeatureModalOpen(false)}>Cancel</Button>
            <Button onClick={handleSaveFeatures} isLoading={updateFeaturesMutation.isPending}>Save Changes</Button>
          </>
        }
      >
        <div className="space-y-4 max-h-[70vh] overflow-y-auto">
          {/* Info Banner */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
            <p className="text-sm text-blue-700">
              <strong>Note:</strong> Disabling a parent feature will automatically restrict access to all its child features.
            </p>
          </div>

          {/* Core Pages */}
          <div className="space-y-2">
            <h4 className="font-semibold text-slate-800 flex items-center gap-2">
              <span className="text-primary-500">📄</span> Core Pages
            </h4>
            <div className="ml-6 space-y-1">
              {PAGE_FEATURES.filter(f => ['incomePage', 'investmentPage', 'loanPage', 'insurancePage', 'expensePage', 'goalsPage', 'familyPage'].includes(f.key)).map(feature => (
                <label key={feature.key} className="flex items-center gap-2 p-2 border border-slate-200 rounded-lg cursor-pointer hover:bg-slate-50 bg-white">
                  <input
                    type="checkbox"
                    checked={featureAccess[feature.key] ?? feature.default}
                    onChange={() => toggleFeature(feature.key)}
                    className="w-4 h-4 text-primary-600 rounded flex-shrink-0"
                  />
                  <span className="text-sm text-slate-700">{feature.label}</span>
                  {!feature.default && <span className="ml-auto text-xs bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full">PRO</span>}
                </label>
              ))}
            </div>
          </div>

          {/* Investment Page - Sub-features */}
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <h4 className="font-semibold text-slate-800 flex items-center gap-2">
                <span className="text-green-500">📊</span> Investment Page
              </h4>
              <span className={`text-xs px-2 py-0.5 rounded-full ${featureAccess['investmentPage'] ?? true ? 'bg-success-100 text-success-700' : 'bg-slate-100 text-slate-500'}`}>
                {featureAccess['investmentPage'] ?? true ? '✓ Enabled' : '✗ Disabled'}
              </span>
            </div>
            <div className="ml-8 border-l-2 border-slate-200 pl-4 space-y-1">
              <p className="text-xs text-slate-500 mb-2">
                {!(featureAccess['investmentPage'] ?? true) && '⚠️ '}Allowed investment types (uncheck to block)
              </p>
              <div className="grid grid-cols-3 gap-1.5">
                {ALL_INVESTMENT_TYPES.map(type => (
                  <label 
                    key={type} 
                    className={`flex items-center gap-2 p-1.5 border rounded cursor-pointer text-xs ${
                      !(featureAccess['investmentPage'] ?? true) 
                        ? 'border-slate-200 bg-slate-50 opacity-50 cursor-not-allowed' 
                        : 'border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={(featureAccess.allowedInvestmentTypes || []).includes(type)}
                      onChange={() => toggleInvestmentType(type)}
                      disabled={!(featureAccess['investmentPage'] ?? true)}
                      className="w-3.5 h-3.5 text-green-600 rounded flex-shrink-0"
                    />
                    <span className="text-slate-700">{type.replace('_', ' ')}</span>
                  </label>
                ))}
              </div>
            </div>
          </div>

          {/* Insurance Page - Sub-features */}
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <h4 className="font-semibold text-slate-800 flex items-center gap-2">
                <span className="text-blue-500">🛡️</span> Insurance Page
              </h4>
              <span className={`text-xs px-2 py-0.5 rounded-full ${featureAccess['insurancePage'] ?? true ? 'bg-success-100 text-success-700' : 'bg-slate-100 text-slate-500'}`}>
                {featureAccess['insurancePage'] ?? true ? '✓ Enabled' : '✗ Disabled'}
              </span>
            </div>
            <div className="ml-8 border-l-2 border-slate-200 pl-4 space-y-1">
              <p className="text-xs text-slate-500 mb-2">
                {!(featureAccess['insurancePage'] ?? true) && '⚠️ '}Blocked insurance types (check to block)
              </p>
              <div className="grid grid-cols-3 gap-1.5">
                {ALL_INSURANCE_TYPES.map(type => (
                  <label 
                    key={type} 
                    className={`flex items-center gap-2 p-1.5 border rounded cursor-pointer text-xs ${
                      !(featureAccess['insurancePage'] ?? true) 
                        ? 'border-slate-200 bg-slate-50 opacity-50 cursor-not-allowed' 
                        : 'border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={(featureAccess.blockedInsuranceTypes || []).includes(type)}
                      onChange={() => toggleInsuranceType(type)}
                      disabled={!(featureAccess['insurancePage'] ?? true)}
                      className="w-3.5 h-3.5 text-danger-600 rounded flex-shrink-0"
                    />
                    <span className="text-slate-700">{type.replace('_', ' ')}</span>
                  </label>
                ))}
              </div>
            </div>
          </div>

          {/* Retirement Page - Sub-features */}
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <h4 className="font-semibold text-slate-800 flex items-center gap-2">
                <span className="text-purple-500">🏖️</span> Retirement Page
              </h4>
              <span className={`text-xs px-2 py-0.5 rounded-full ${featureAccess['retirementPage'] ?? true ? 'bg-success-100 text-success-700' : 'bg-slate-100 text-slate-500'}`}>
                {featureAccess['retirementPage'] ?? true ? '✓ Enabled' : '✗ Disabled'}
              </span>
            </div>
            <div className="ml-8 border-l-2 border-slate-200 pl-4 space-y-1">
              <p className="text-xs text-slate-500 mb-2">
                {!(featureAccess['retirementPage'] ?? true) && '⚠️ '}Available tabs (requires Retirement Page enabled)
              </p>
              {RETIREMENT_FEATURES.map(feature => (
                <label 
                  key={feature.key} 
                  className={`flex items-center gap-2 p-2 border rounded cursor-pointer ${
                    !(featureAccess['retirementPage'] ?? true) 
                      ? 'border-slate-200 bg-slate-50 opacity-50 cursor-not-allowed' 
                      : 'border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={featureAccess[feature.key] ?? false}
                    onChange={() => toggleFeature(feature.key)}
                    disabled={!(featureAccess['retirementPage'] ?? true)}
                    className="w-4 h-4 text-purple-600 rounded flex-shrink-0"
                  />
                  <span className="text-sm text-slate-700">{feature.label}</span>
                  <span className="ml-auto text-xs bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full">PRO</span>
                </label>
              ))}
            </div>
          </div>

          {/* Reports Page - Sub-features */}
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <h4 className="font-semibold text-slate-800 flex items-center gap-2">
                <span className="text-orange-500">📑</span> Reports Page
              </h4>
              <span className={`text-xs px-2 py-0.5 rounded-full ${featureAccess['reportsPage'] ?? false ? 'bg-success-100 text-success-700' : 'bg-slate-100 text-slate-500'}`}>
                {featureAccess['reportsPage'] ?? false ? '✓ Enabled' : '✗ Disabled'}
              </span>
              <span className="ml-auto text-xs bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full">PRO</span>
            </div>
            <div className="ml-8 border-l-2 border-slate-200 pl-4 space-y-1">
              <p className="text-xs text-slate-500 mb-2">
                {!(featureAccess['reportsPage'] ?? false) && '⚠️ '}Export features (requires Reports Page enabled)
              </p>
              {REPORT_FEATURES.map(feature => (
                <label 
                  key={feature.key} 
                  className={`flex items-center gap-2 p-2 border rounded cursor-pointer ${
                    !(featureAccess['reportsPage'] ?? false) 
                      ? 'border-slate-200 bg-slate-50 opacity-50 cursor-not-allowed' 
                      : 'border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={featureAccess[feature.key] ?? false}
                    onChange={() => toggleFeature(feature.key)}
                    disabled={!(featureAccess['reportsPage'] ?? false)}
                    className="w-4 h-4 text-orange-600 rounded flex-shrink-0"
                  />
                  <span className="text-sm text-slate-700">{feature.label}</span>
                  <span className="ml-auto text-xs bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full">PRO</span>
                </label>
              ))}
            </div>
          </div>

          {/* Simulation Features */}
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <h4 className="font-semibold text-slate-800 flex items-center gap-2">
                <span className="text-teal-500">🎯</span> Simulation
              </h4>
              <span className={`text-xs px-2 py-0.5 rounded-full ${featureAccess['simulationPage'] ?? false ? 'bg-success-100 text-success-700' : 'bg-slate-100 text-slate-500'}`}>
                {featureAccess['simulationPage'] ?? false ? '✓ Enabled' : '✗ Disabled'}
              </span>
              <span className="ml-auto text-xs bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full">PRO</span>
            </div>
            <div className="ml-8 border-l-2 border-slate-200 pl-4 space-y-1">
              <p className="text-xs text-slate-500 mb-2">
                {!(featureAccess['simulationPage'] ?? false) && '⚠️ '}Simulation capabilities
              </p>
              {SIMULATION_FEATURES.map(feature => (
                <label 
                  key={feature.key} 
                  className={`flex items-center gap-2 p-2 border rounded cursor-pointer ${
                    feature.key !== 'simulationPage' && !(featureAccess['simulationPage'] ?? false)
                      ? 'border-slate-200 bg-slate-50 opacity-50 cursor-not-allowed' 
                      : 'border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={featureAccess[feature.key] ?? false}
                    onChange={() => toggleFeature(feature.key)}
                    disabled={feature.key !== 'simulationPage' && !(featureAccess['simulationPage'] ?? false)}
                    className="w-4 h-4 text-teal-600 rounded flex-shrink-0"
                  />
                  <span className="text-sm text-slate-700">{feature.label}</span>
                  <span className="ml-auto text-xs bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full">PRO</span>
                </label>
              ))}
            </div>
          </div>

          {/* Other Premium Pages */}
          <div className="space-y-2">
            <h4 className="font-semibold text-slate-800 flex items-center gap-2">
              <span className="text-amber-500">⭐</span> Other Premium Pages
            </h4>
            <div className="ml-6 space-y-1">
              {PAGE_FEATURES.filter(f => ['calendarPage', 'insuranceRecommendationsPage', 'preferencesPage', 'settingsPage', 'accountPage'].includes(f.key)).map(feature => (
                <label key={feature.key} className="flex items-center gap-2 p-2 border border-slate-200 rounded-lg cursor-pointer hover:bg-slate-50 bg-white">
                  <input
                    type="checkbox"
                    checked={featureAccess[feature.key] ?? feature.default}
                    onChange={() => toggleFeature(feature.key)}
                    className="w-4 h-4 text-amber-600 rounded flex-shrink-0"
                  />
                  <span className="text-sm text-slate-700">{feature.label}</span>
                  {!feature.default && <span className="ml-auto text-xs bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full">PRO</span>}
                </label>
              ))}
            </div>
          </div>

          {/* Admin Access */}
          <div className="space-y-2 border-t pt-4">
            <h4 className="font-semibold text-slate-800 flex items-center gap-2">
              <span className="text-red-500">👑</span> Administrative
            </h4>
            <div className="ml-6 space-y-1">
              {PAGE_FEATURES.filter(f => f.key === 'adminPanel').map(feature => (
                <label key={feature.key} className="flex items-center gap-2 p-2 border border-red-200 bg-red-50 rounded-lg cursor-pointer hover:bg-red-100">
                  <input
                    type="checkbox"
                    checked={featureAccess[feature.key] ?? feature.default}
                    onChange={() => toggleFeature(feature.key)}
                    className="w-4 h-4 text-red-600 rounded flex-shrink-0"
                  />
                  <span className="text-sm text-red-700 font-medium">{feature.label}</span>
                  <span className="ml-auto text-xs bg-red-200 text-red-800 px-2 py-0.5 rounded-full font-medium">ADMIN ONLY</span>
                </label>
              ))}
            </div>
          </div>
        </div>
      </Modal>
    </MainLayout>
  );
}

export default Admin;
