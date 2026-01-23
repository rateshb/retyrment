import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, Button, Modal, Input, Select, toast } from '../components/ui';
import { insuranceApi, Insurance as InsuranceType } from '../lib/api';
import { amountInWordsHelper, formatCurrency, formatDate } from '../lib/utils';
import { Plus, Pencil, Trash2, Shield, Heart, Umbrella } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';

const INSURANCE_TYPES = [
  { value: 'TERM_LIFE', label: 'Term Life Insurance' },
  { value: 'HEALTH', label: 'Health Insurance' },
  { value: 'ULIP', label: 'ULIP' },
  { value: 'ENDOWMENT', label: 'Endowment' },
  { value: 'MONEY_BACK', label: 'Money Back' },
  { value: 'ANNUITY', label: 'Annuity/Pension' },
  { value: 'VEHICLE', label: 'Vehicle Insurance' },
  { value: 'OTHER', label: 'Other' },
];

const PREMIUM_FREQUENCIES = [
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'QUARTERLY', label: 'Quarterly' },
  { value: 'HALF_YEARLY', label: 'Half Yearly' },
  { value: 'YEARLY', label: 'Yearly' },
  { value: 'SINGLE', label: 'Single Premium' },
];

export function Insurance() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<InsuranceType | null>(null);
  const [formData, setFormData] = useState<Partial<InsuranceType>>({});
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const { features } = useAuthStore();
  
  // Filter insurance types based on blocked types from backend
  const filteredInsuranceTypes = features?.blockedInsuranceTypes 
    ? INSURANCE_TYPES.filter(type => !features.blockedInsuranceTypes.includes(type.value))
    : INSURANCE_TYPES;

  const { data: insurances = [], isLoading } = useQuery({
    queryKey: ['insurance'],
    queryFn: insuranceApi.getAll,
  });

  const createMutation = useMutation({
    mutationFn: (data: InsuranceType) => insuranceApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['insurance'] });
      queryClient.invalidateQueries({ queryKey: ['insurance-recommendations'] });
      queryClient.invalidateQueries({ queryKey: ['retirement'] });
      toast.success('Insurance policy added successfully');
      closeModal();
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: InsuranceType }) => insuranceApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['insurance'] });
      queryClient.invalidateQueries({ queryKey: ['insurance-recommendations'] });
      queryClient.invalidateQueries({ queryKey: ['retirement'] });
      toast.success('Insurance policy updated successfully');
      closeModal();
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => insuranceApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['insurance'] });
      queryClient.invalidateQueries({ queryKey: ['insurance-recommendations'] });
      queryClient.invalidateQueries({ queryKey: ['retirement'] });
      toast.success('Insurance policy deleted successfully');
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const openAddModal = () => {
    setEditingItem(null);
    setFormData({ premiumFrequency: 'YEARLY', renewalMonth: new Date().getMonth() + 1 });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const openEditModal = (item: InsuranceType) => {
    setEditingItem(item);
    setFormData(item);
    setFormErrors({});
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setEditingItem(null);
    setFormData({});
    setFormErrors({});
  };

  const validateForm = () => {
    const errors: Record<string, string> = {};
    if (!formData.policyName) errors.policyName = 'Policy name is required';
    if (!formData.type) errors.type = 'Type is required';
    if (!formData.company) errors.company = 'Company is required';
    if (formData.type !== 'ANNUITY' && (!formData.sumAssured || Number(formData.sumAssured) <= 0)) {
      errors.sumAssured = 'Enter a valid amount';
    }
    if (!formData.annualPremium || Number(formData.annualPremium) <= 0) errors.annualPremium = 'Enter a valid premium';
    if (!formData.premiumFrequency) errors.premiumFrequency = 'Frequency is required';
    if (formData.type === 'HEALTH' && !formData.healthType) errors.healthType = 'Coverage type is required';
    if (!formData.renewalMonth) errors.renewalMonth = 'Renewal month is required';
    if (!formData.startDate) errors.startDate = 'Start date is required';
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) {
      toast.error('Please fix the highlighted fields');
      return;
    }
    const data = {
      ...formData,
      sumAssured: Number(formData.sumAssured) || 0,
      annualPremium: Number(formData.annualPremium) || 0,
      renewalMonth: Number(formData.renewalMonth) || undefined,
      isAnnuityPolicy: formData.type === 'ANNUITY' ? true : formData.isAnnuityPolicy,
    } as InsuranceType;
    if (editingItem?.id) {
      updateMutation.mutate({ id: editingItem.id, data });
    } else {
      createMutation.mutate(data);
    }
  };

  const handleDelete = (id: string) => {
    if (confirm('Are you sure you want to delete this policy?')) {
      deleteMutation.mutate(id);
    }
  };

  const moneyBackPayouts = formData.moneyBackPayouts || [];
  const addMoneyBackPayout = () => {
    setFormData({
      ...formData,
      moneyBackPayouts: [
        ...moneyBackPayouts,
        { policyYear: undefined, percentage: undefined, fixedAmount: undefined, includesBonus: false },
      ],
    });
  };
  const updateMoneyBackPayout = (index: number, updates: Partial<NonNullable<InsuranceType['moneyBackPayouts']>[number]>) => {
    const next = moneyBackPayouts.map((payout, i) => (i === index ? { ...payout, ...updates } : payout));
    setFormData({ ...formData, moneyBackPayouts: next });
  };
  const removeMoneyBackPayout = (index: number) => {
    const next = moneyBackPayouts.filter((_, i) => i !== index);
    setFormData({ ...formData, moneyBackPayouts: next });
  };

  // Calculate totals by type
  const healthCover = insurances
    .filter(i => i.type === 'HEALTH')
    .reduce((sum, i) => sum + (i.sumAssured || 0), 0);
  
  const lifeCover = insurances
    .filter(i => ['TERM_LIFE', 'ULIP', 'ENDOWMENT', 'MONEY_BACK'].includes(i.type))
    .reduce((sum, i) => sum + (i.sumAssured || 0), 0);

  const yearlyPremium = insurances.reduce((sum, i) => sum + (i.annualPremium || 0), 0);

  const postRetirePremium = insurances.reduce((sum, i) => {
    const premium = i.annualPremium || 0;
    if (i.type === 'HEALTH' && i.healthType !== 'GROUP') {
      return sum + premium;
    }
    if (i.type === 'TERM_LIFE' && i.continuesAfterRetirement !== false) {
      return sum + premium;
    }
    return sum;
  }, 0);

  const isTermLife = formData.type === 'TERM_LIFE';
  const isAnnuity = formData.type === 'ANNUITY';
  const isLifeSavings = ['ENDOWMENT', 'ULIP', 'MONEY_BACK'].includes(formData.type || '');
  const isMoneyBack = formData.type === 'MONEY_BACK';
  const showCoverageAmount = !isAnnuity;

  return (
    <MainLayout
      title="Insurance"
      subtitle="Manage your insurance policies"
      actions={
        <Button onClick={openAddModal}>
          <Plus size={18} className="mr-2" /> Add Policy
        </Button>
      }
    >
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <Card className="bg-success-50 border-success-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <Heart className="text-success-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-success-600">Health Cover</p>
              <p className="text-2xl font-bold text-success-700">{formatCurrency(healthCover, true)}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-primary-50 border-primary-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <Umbrella className="text-primary-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-primary-600">Life Cover</p>
              <p className="text-2xl font-bold text-primary-700">{formatCurrency(lifeCover, true)}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-warning-50 border-warning-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <Shield className="text-warning-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-warning-600">Yearly Premium</p>
              <p className="text-2xl font-bold text-warning-700">{formatCurrency(yearlyPremium, true)}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-emerald-50 border-emerald-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <Shield className="text-emerald-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-emerald-600">Post-Retirement Premium</p>
              <p className="text-2xl font-bold text-emerald-700">{formatCurrency(postRetirePremium, true)}</p>
            </div>
          </div>
        </Card>
      </div>

      {/* Insurance Table */}
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Policy Name</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Type</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Company</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase">Sum Assured</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase">Premium</th>
                <th className="px-6 py-4 text-center text-xs font-semibold text-slate-500 uppercase">Maturity</th>
                <th className="px-6 py-4 text-center text-xs font-semibold text-slate-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr><td colSpan={7} className="px-6 py-8 text-center text-slate-400">Loading...</td></tr>
              ) : insurances.length === 0 ? (
                <tr><td colSpan={7} className="px-6 py-8 text-center text-slate-400">No insurance policies yet. Click "Add Policy" to get started.</td></tr>
              ) : (
                insurances.map((ins) => (
                  <tr key={ins.id} className="hover:bg-slate-50">
                    <td className="px-6 py-4">
                      <div>
                        <p className="font-medium text-slate-800">{ins.policyName}</p>
                        {ins.policyNumber && <p className="text-xs text-slate-500">#{ins.policyNumber}</p>}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 text-xs rounded-full ${
                        ins.type === 'HEALTH' ? 'bg-success-100 text-success-700' :
                        ins.type === 'TERM_LIFE' ? 'bg-primary-100 text-primary-700' :
                        'bg-slate-100 text-slate-700'
                      }`}>
                        {INSURANCE_TYPES.find(t => t.value === ins.type)?.label || ins.type}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-slate-600">{ins.company}</td>
                    <td className="px-6 py-4 text-right text-success-600 font-medium">{formatCurrency(ins.sumAssured, true)}</td>
                    <td className="px-6 py-4 text-right text-slate-800">
                      {formatCurrency(ins.annualPremium, true)}
                      <span className="text-xs text-slate-500 ml-1">
                        /{PREMIUM_FREQUENCIES.find(f => f.value === ins.premiumFrequency)?.label || 'yr'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-center text-slate-600">{ins.maturityDate ? formatDate(ins.maturityDate) : '-'}</td>
                    <td className="px-6 py-4">
                      <div className="flex justify-center gap-2">
                        <button onClick={() => openEditModal(ins)} className="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg">
                          <Pencil size={16} />
                        </button>
                        <button onClick={() => handleDelete(ins.id!)} className="p-2 text-slate-400 hover:text-danger-600 hover:bg-danger-50 rounded-lg">
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

      {/* Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={editingItem ? 'Edit Policy' : 'Add Policy'}
        size="lg"
        footer={
          <>
            <Button variant="secondary" onClick={closeModal}>Cancel</Button>
            <Button onClick={handleSubmit} isLoading={createMutation.isPending || updateMutation.isPending}>
              {editingItem ? 'Save Changes' : 'Add Policy'}
            </Button>
          </>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Policy Name"
              value={formData.policyName || ''}
              onChange={e => {
                setFormData({ ...formData, policyName: e.target.value });
                if (formErrors.policyName) setFormErrors(prev => ({ ...prev, policyName: '' }));
              }}
              placeholder="e.g., HDFC Life Click2Protect"
              error={formErrors.policyName}
              required
            />
            <Input
              label="Policy Number"
              value={formData.policyNumber || ''}
              onChange={e => setFormData({ ...formData, policyNumber: e.target.value })}
              placeholder="e.g., POL123456"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Insurance Type"
              value={formData.type || ''}
              onChange={e => {
                const nextType = e.target.value;
                setFormData({
                  ...formData,
                  type: nextType,
                  healthType: nextType === 'HEALTH' ? (formData.healthType || 'PERSONAL') : formData.healthType,
                });
                if (formErrors.type) setFormErrors(prev => ({ ...prev, type: '' }));
              }}
              options={filteredInsuranceTypes}
              error={formErrors.type}
              required
            />
            <Input
              label="Company"
              value={formData.company || ''}
              onChange={e => {
                setFormData({ ...formData, company: e.target.value });
                if (formErrors.company) setFormErrors(prev => ({ ...prev, company: '' }));
              }}
              placeholder="e.g., HDFC Life"
              error={formErrors.company}
              required
            />
          </div>
          {formData.type === 'HEALTH' && (
            <div className="rounded-lg border border-primary-200 bg-primary-50/40 p-4">
              <div className="text-sm font-medium text-slate-700 mb-2">Coverage Type *</div>
              <div className="grid grid-cols-3 gap-3">
                {[
                  { value: 'PERSONAL', label: 'Personal' },
                  { value: 'FAMILY_FLOATER', label: 'Family' },
                  { value: 'GROUP', label: 'Group' },
                ].map(option => (
                  <label key={option.value} className="flex items-center gap-2 bg-white rounded-lg border border-slate-200 px-3 py-2 cursor-pointer hover:border-primary-300">
                    <input
                      type="radio"
                      name="healthType"
                      value={option.value}
                      checked={formData.healthType === option.value}
                      onChange={() => {
                        setFormData({ ...formData, healthType: option.value });
                        if (formErrors.healthType) setFormErrors(prev => ({ ...prev, healthType: '' }));
                      }}
                    />
                    <span className="text-sm text-slate-700">{option.label}</span>
                  </label>
                ))}
              </div>
              {formErrors.healthType && <p className="text-sm text-danger-500 mt-1">{formErrors.healthType}</p>}
              <p className="text-xs text-slate-500 mt-2">Group insurance ends at retirement. Personal/Family continues.</p>
            </div>
          )}
          <div className={`grid gap-4 ${showCoverageAmount ? 'grid-cols-2' : 'grid-cols-1'}`}>
            {showCoverageAmount && (
              <Input
                label={formData.type === 'HEALTH' ? 'Coverage Amount (₹)' : 'Sum Assured (₹)'}
                type="number"
                value={formData.sumAssured || ''}
                onChange={e => {
                  setFormData({ ...formData, sumAssured: Number(e.target.value) });
                  if (formErrors.sumAssured) setFormErrors(prev => ({ ...prev, sumAssured: '' }));
                }}
                placeholder="10000000"
                helperText={amountInWordsHelper(formData.sumAssured)}
                error={formErrors.sumAssured}
                required
              />
            )}
            <Input
              label="Annual Premium (₹)"
              type="number"
              value={formData.annualPremium || ''}
              onChange={e => {
                setFormData({ ...formData, annualPremium: Number(e.target.value) });
                if (formErrors.annualPremium) setFormErrors(prev => ({ ...prev, annualPremium: '' }));
              }}
              placeholder="25000"
              helperText={amountInWordsHelper(formData.annualPremium)}
              error={formErrors.annualPremium}
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Premium Frequency"
              value={formData.premiumFrequency || ''}
              onChange={e => {
                setFormData({ ...formData, premiumFrequency: e.target.value });
                if (formErrors.premiumFrequency) setFormErrors(prev => ({ ...prev, premiumFrequency: '' }));
              }}
              options={PREMIUM_FREQUENCIES}
              error={formErrors.premiumFrequency}
              required
            />
            <Select
              label="Renewal Month"
              value={formData.renewalMonth ? String(formData.renewalMonth) : ''}
              onChange={e => {
                setFormData({ ...formData, renewalMonth: Number(e.target.value) });
                if (formErrors.renewalMonth) setFormErrors(prev => ({ ...prev, renewalMonth: '' }));
              }}
              options={[
                { value: '1', label: 'Jan' },
                { value: '2', label: 'Feb' },
                { value: '3', label: 'Mar' },
                { value: '4', label: 'Apr' },
                { value: '5', label: 'May' },
                { value: '6', label: 'Jun' },
                { value: '7', label: 'Jul' },
                { value: '8', label: 'Aug' },
                { value: '9', label: 'Sep' },
                { value: '10', label: 'Oct' },
                { value: '11', label: 'Nov' },
                { value: '12', label: 'Dec' },
              ]}
              error={formErrors.renewalMonth}
              required
            />
          </div>
          {isTermLife && (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50/40 p-4">
              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Coverage Until Age"
                  type="number"
                  value={formData.coverageEndAge || ''}
                  onChange={e => setFormData({ ...formData, coverageEndAge: Number(e.target.value) })}
                  placeholder="65"
                />
                <div className="flex items-end">
                  <label className="flex items-center gap-2 text-sm text-emerald-700">
                    <input
                      type="checkbox"
                      checked={formData.continuesAfterRetirement !== false}
                      onChange={e => setFormData({ ...formData, continuesAfterRetirement: e.target.checked })}
                    />
                    Premium continues after retirement
                  </label>
                </div>
              </div>
            </div>
          )}

          {isLifeSavings && (
            <div className="rounded-lg border border-amber-200 bg-amber-50/40 p-4 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Policy Term (Years)"
                  type="number"
                  value={formData.policyTerm || ''}
                  onChange={e => setFormData({ ...formData, policyTerm: Number(e.target.value) })}
                  placeholder="20"
                />
                <Input
                  label="Expected Maturity (₹)"
                  type="number"
                  value={formData.maturityBenefit || ''}
                  onChange={e => setFormData({ ...formData, maturityBenefit: Number(e.target.value) })}
                  placeholder="1000000"
                />
              </div>
              <Input
                label="Maturity Date"
                type="date"
                value={formData.maturityDate?.split('T')[0] || ''}
                onChange={e => setFormData({ ...formData, maturityDate: e.target.value })}
              />
              <p className="text-xs text-amber-700">Maturity amount will be added to your corpus in that year.</p>
            </div>
          )}

          {isMoneyBack && (
            <div className="rounded-lg border border-pink-200 bg-pink-50/40 p-4 space-y-3">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-pink-800">Money-Back Payout Schedule</p>
                <Button variant="secondary" size="sm" type="button" onClick={addMoneyBackPayout}>+ Add Payout</Button>
              </div>
              <p className="text-xs text-pink-600">Define payouts at different policy years (percentage or fixed amount).</p>
              {moneyBackPayouts.length === 0 && (
                <p className="text-xs text-slate-500">No payouts added yet.</p>
              )}
              <div className="space-y-2">
                {moneyBackPayouts.map((payout, index) => (
                  <div key={index} className="grid grid-cols-5 gap-2 items-end">
                    <Input
                      label="Policy Year"
                      type="number"
                      value={payout.policyYear || ''}
                      onChange={e => updateMoneyBackPayout(index, { policyYear: Number(e.target.value) })}
                    />
                    <Input
                      label="% of Sum Assured"
                      type="number"
                      value={payout.percentage || ''}
                      onChange={e => updateMoneyBackPayout(index, { percentage: Number(e.target.value), fixedAmount: undefined })}
                    />
                    <Input
                      label="Fixed Amount"
                      type="number"
                      value={payout.fixedAmount || ''}
                      onChange={e => updateMoneyBackPayout(index, { fixedAmount: Number(e.target.value), percentage: undefined })}
                    />
                    <Input
                      label="Description"
                      value={payout.description || ''}
                      onChange={e => updateMoneyBackPayout(index, { description: e.target.value })}
                    />
                    <Button variant="secondary" size="sm" type="button" onClick={() => removeMoneyBackPayout(index)}>Remove</Button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {isAnnuity && (
            <div className="rounded-lg border border-teal-200 bg-teal-50/40 p-4 space-y-4">
              <p className="text-sm font-medium text-teal-800">Pension / Annuity Details</p>
              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Pay Premium for (Years)"
                  type="number"
                  value={formData.premiumPaymentYears || ''}
                  onChange={e => setFormData({ ...formData, premiumPaymentYears: Number(e.target.value) })}
                  placeholder="15"
                />
                <Input
                  label="Annuity Starts After (Years)"
                  type="number"
                  value={formData.annuityStartYear || ''}
                  onChange={e => setFormData({ ...formData, annuityStartYear: Number(e.target.value) })}
                  placeholder="15"
                />
                <Input
                  label="Monthly Annuity (₹)"
                  type="number"
                  value={formData.monthlyAnnuityAmount || ''}
                  onChange={e => setFormData({ ...formData, monthlyAnnuityAmount: Number(e.target.value) })}
                  placeholder="10000"
                />
                <Input
                  label="Annual Increase (%)"
                  type="number"
                  value={formData.annuityGrowthRate || ''}
                  onChange={e => setFormData({ ...formData, annuityGrowthRate: Number(e.target.value) })}
                  placeholder="3"
                  step="0.5"
                />
              </div>
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Start Date"
              type="date"
              value={formData.startDate?.split('T')[0] || ''}
              onChange={e => {
                setFormData({ ...formData, startDate: e.target.value });
                if (formErrors.startDate) setFormErrors(prev => ({ ...prev, startDate: '' }));
              }}
              error={formErrors.startDate}
              required
            />
            {!isLifeSavings && (
              <Input
                label="Maturity Date"
                type="date"
                value={formData.maturityDate?.split('T')[0] || ''}
                onChange={e => setFormData({ ...formData, maturityDate: e.target.value })}
              />
            )}
          </div>
        </form>
      </Modal>
    </MainLayout>
  );
}

export default Insurance;
