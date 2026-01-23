import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, Button, Modal, Input, Select, toast } from '../components/ui';
import { investmentsApi, Investment } from '../lib/api';
import { amountInWordsHelper, formatCurrency } from '../lib/utils';
import { Plus, Pencil, Trash2, Shield } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';

const INVESTMENT_TYPES = [
  { value: 'MUTUAL_FUND', label: 'Mutual Fund', icon: '📈' },
  { value: 'STOCK', label: 'Stocks', icon: '📊' },
  { value: 'FD', label: 'Fixed Deposit', icon: '🏦' },
  { value: 'RD', label: 'Recurring Deposit', icon: '📅' },
  { value: 'PPF', label: 'PPF', icon: '🏛️' },
  { value: 'EPF', label: 'EPF', icon: '👔' },
  { value: 'NPS', label: 'NPS', icon: '🧓' },
  { value: 'REAL_ESTATE', label: 'Real Estate', icon: '🏠' },
  { value: 'GOLD', label: 'Gold', icon: '🥇' },
  { value: 'CRYPTO', label: 'Crypto', icon: '₿' },
  { value: 'CASH', label: 'Cash/Savings', icon: '💵' },
  { value: 'OTHER', label: 'Other', icon: '📦' },
];

// Field configurations by investment type
const TYPE_FIELDS: Record<string, {
  showSip: boolean;
  showSipDay: boolean;
  showRdDay: boolean;
  showMaturityDate: boolean;
  showEmergencyFund: boolean;
  showYearlyContribution: boolean;
  showInterestRate: boolean;
  sipLabel: string;
  dateLabel: string;
  investedLabel: string;
  currentLabel: string;
  defaultReturn: number;
}> = {
  MUTUAL_FUND: {
    showSip: true, showSipDay: true, showRdDay: false, showMaturityDate: false, showEmergencyFund: false,
    showYearlyContribution: false, showInterestRate: false,
    sipLabel: 'Monthly SIP Amount (₹)', dateLabel: 'First Investment Date',
    investedLabel: 'Total Invested (₹)', currentLabel: 'Current NAV Value (₹)', defaultReturn: 12
  },
  STOCK: {
    showSip: false, showSipDay: false, showRdDay: false, showMaturityDate: false, showEmergencyFund: false,
    showYearlyContribution: false, showInterestRate: false,
    sipLabel: '', dateLabel: 'Purchase Date',
    investedLabel: 'Total Purchase Cost (₹)', currentLabel: 'Current Market Value (₹)', defaultReturn: 15
  },
  FD: {
    showSip: false, showSipDay: false, showRdDay: false, showMaturityDate: true, showEmergencyFund: true,
    showYearlyContribution: false, showInterestRate: true,
    sipLabel: '', dateLabel: 'Deposit Date',
    investedLabel: 'Principal Amount (₹)', currentLabel: 'Maturity Value (₹)', defaultReturn: 7
  },
  RD: {
    showSip: true, showSipDay: false, showRdDay: true, showMaturityDate: true, showEmergencyFund: true,
    showYearlyContribution: false, showInterestRate: true,
    sipLabel: 'Monthly Deposit (₹)', dateLabel: 'Start Date',
    investedLabel: 'Total Deposited (₹)', currentLabel: 'Current Value (₹)', defaultReturn: 6.5
  },
  PPF: {
    showSip: false, showSipDay: false, showRdDay: false, showMaturityDate: false, showEmergencyFund: false,
    showYearlyContribution: true, showInterestRate: false,
    sipLabel: '', dateLabel: 'Account Opening Date',
    investedLabel: 'Total Contribution (₹)', currentLabel: 'Current Balance (₹)', defaultReturn: 7.1
  },
  EPF: {
    showSip: true, showSipDay: false, showRdDay: false, showMaturityDate: false, showEmergencyFund: false,
    showYearlyContribution: false, showInterestRate: false,
    sipLabel: 'Monthly Contribution (Employee + Employer) (₹)', dateLabel: 'Employment Start Date',
    investedLabel: 'Contribution Till Date (₹)', currentLabel: 'Current EPF Balance (₹)', defaultReturn: 8.15
  },
  NPS: {
    showSip: true, showSipDay: true, showRdDay: false, showMaturityDate: false, showEmergencyFund: false,
    showYearlyContribution: false, showInterestRate: false,
    sipLabel: 'Monthly Contribution (₹)', dateLabel: 'Account Opening Date',
    investedLabel: 'Total Contribution (₹)', currentLabel: 'Current NAV Value (₹)', defaultReturn: 10
  },
  REAL_ESTATE: {
    showSip: false, showSipDay: false, showRdDay: false, showMaturityDate: false, showEmergencyFund: false,
    showYearlyContribution: false, showInterestRate: false,
    sipLabel: '', dateLabel: 'Purchase Date',
    investedLabel: 'Purchase Price (₹)', currentLabel: 'Current Market Value (₹)', defaultReturn: 8
  },
  GOLD: {
    showSip: false, showSipDay: false, showRdDay: false, showMaturityDate: false, showEmergencyFund: false,
    showYearlyContribution: false, showInterestRate: false,
    sipLabel: '', dateLabel: 'Purchase Date',
    investedLabel: 'Purchase Cost (₹)', currentLabel: 'Current Market Value (₹)', defaultReturn: 8
  },
  CRYPTO: {
    showSip: false, showSipDay: false, showRdDay: false, showMaturityDate: false, showEmergencyFund: false,
    showYearlyContribution: false, showInterestRate: false,
    sipLabel: '', dateLabel: 'First Purchase Date',
    investedLabel: 'Total Invested (₹)', currentLabel: 'Current Value (₹)', defaultReturn: 20
  },
  CASH: {
    showSip: false, showSipDay: false, showRdDay: false, showMaturityDate: false, showEmergencyFund: false,
    showYearlyContribution: false, showInterestRate: true,
    sipLabel: '', dateLabel: '',
    investedLabel: '', currentLabel: 'Current Balance (₹)', defaultReturn: 3
  },
  OTHER: {
    showSip: true, showSipDay: false, showRdDay: false, showMaturityDate: false, showEmergencyFund: false,
    showYearlyContribution: false, showInterestRate: false,
    sipLabel: 'Monthly Contribution (₹)', dateLabel: 'Investment Date',
    investedLabel: 'Invested Amount (₹)', currentLabel: 'Current Value (₹)', defaultReturn: 10
  },
};

export function Investments() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<Investment | null>(null);
  const [formData, setFormData] = useState<Partial<Investment>>({});
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const { features } = useAuthStore();
  
  // Filter investment types based on allowed types from backend
  const filteredInvestmentTypes = features?.allowedInvestmentTypes 
    ? INVESTMENT_TYPES.filter(type => features.allowedInvestmentTypes.includes(type.value))
    : INVESTMENT_TYPES;

  const selectedType = formData.type || 'MUTUAL_FUND';
  const typeConfig = TYPE_FIELDS[selectedType] || TYPE_FIELDS.OTHER;

  const { data: investments = [], isLoading } = useQuery({
    queryKey: ['investments'],
    queryFn: investmentsApi.getAll,
  });

  const createMutation = useMutation({
    mutationFn: (data: Investment) => investmentsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['investments'] });
      toast.success('Investment added successfully');
      closeModal();
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to add investment');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Investment }) => 
      investmentsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['investments'] });
      toast.success('Investment updated successfully');
      closeModal();
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to update investment');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => investmentsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['investments'] });
      toast.success('Investment deleted successfully');
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Failed to delete investment');
    },
  });

  // Set default expected return when type changes
  useEffect(() => {
    if (!editingItem && formData.type) {
      const config = TYPE_FIELDS[formData.type] || TYPE_FIELDS.OTHER;
      setFormData(prev => ({ 
        ...prev, 
        expectedReturn: prev.expectedReturn || config.defaultReturn 
      }));
    }
  }, [formData.type, editingItem]);

  const openAddModal = () => {
    setEditingItem(null);
    setFormData({ type: 'MUTUAL_FUND', expectedReturn: 12 });
    setFormErrors({});
    setIsModalOpen(true);
  };


  const openEditModal = (item: Investment) => {
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
    const config = TYPE_FIELDS[formData.type || 'MUTUAL_FUND'] || TYPE_FIELDS.OTHER;
    if (!formData.type) errors.type = 'Type is required';
    if (!formData.name) errors.name = 'Name is required';
    if (config.investedLabel && (!formData.investedAmount || Number(formData.investedAmount) <= 0)) {
      errors.investedAmount = 'Enter a valid amount';
    }
    if (!formData.currentValue || Number(formData.currentValue) <= 0) errors.currentValue = 'Enter a valid amount';
    if (config.showSip) {
      if (formData.monthlySip !== undefined && Number(formData.monthlySip) < 0) {
        errors.monthlySip = 'Enter a valid amount';
      } else if (formData.type && formData.type !== 'MUTUAL_FUND' && (!formData.monthlySip || Number(formData.monthlySip) <= 0)) {
        errors.monthlySip = 'Enter a valid amount';
      }
    }
    if (config.showSipDay && formData.sipDay && (formData.sipDay < 1 || formData.sipDay > 28)) {
      errors.sipDay = 'Day must be 1-28';
    }
    if (config.showRdDay && formData.rdDay && (formData.rdDay < 1 || formData.rdDay > 28)) {
      errors.rdDay = 'Day must be 1-28';
    }
    if (config.showYearlyContribution && (!formData.yearlyContribution || Number(formData.yearlyContribution) <= 0)) {
      errors.yearlyContribution = 'Enter a valid amount';
    }
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
      investedAmount: Number(formData.investedAmount) || 0,
      currentValue: Number(formData.currentValue) || 0,
      monthlySip: formData.monthlySip ? Number(formData.monthlySip) : undefined,
      sipDay: formData.sipDay ? Number(formData.sipDay) : undefined,
      rdDay: formData.rdDay ? Number(formData.rdDay) : undefined,
      yearlyContribution: formData.yearlyContribution ? Number(formData.yearlyContribution) : undefined,
      expectedReturn: Number(formData.expectedReturn) || 0,
      interestRate: formData.interestRate ? Number(formData.interestRate) : undefined,
    } as Investment;

    if (editingItem?.id) {
      updateMutation.mutate({ id: editingItem.id, data });
    } else {
      createMutation.mutate(data);
    }
  };

  const handleDelete = (id: string) => {
    if (confirm('Are you sure you want to delete this investment?')) {
      deleteMutation.mutate(id);
    }
  };

  const toggleEmergencyFund = async (item: Investment) => {
    // Only allow for FD and RD
    if (item.type !== 'FD' && item.type !== 'RD') {
      toast.error('Emergency fund tagging is only available for FD and RD');
      return;
    }
    const updated = { ...item, isEmergencyFund: !item.isEmergencyFund };
    updateMutation.mutate({ id: item.id!, data: updated });
  };

  const totalInvested = investments.reduce((sum, inv) => sum + (inv.investedAmount || 0), 0);
  const totalCurrent = investments.reduce((sum, inv) => sum + (inv.currentValue || 0), 0);
  const totalGain = totalCurrent - totalInvested;

  // Can this type have emergency fund tagging?
  const canTagEmergency = (type: string) => type === 'FD' || type === 'RD';

  return (
    <MainLayout
      title="Investments"
      subtitle="Track your investment portfolio"
      actions={
        <Button onClick={openAddModal}>
          <Plus size={18} className="mr-2" />
          Add Investment
        </Button>
      }
    >
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card className="bg-primary-50 border-primary-200">
          <div className="p-4">
            <p className="text-sm text-primary-600">Total Invested</p>
            <p className="text-2xl font-bold text-primary-700">{formatCurrency(totalInvested, true)}</p>
          </div>
        </Card>
        <Card className="bg-success-50 border-success-200">
          <div className="p-4">
            <p className="text-sm text-success-600">Current Value</p>
            <p className="text-2xl font-bold text-success-700">{formatCurrency(totalCurrent, true)}</p>
          </div>
        </Card>
        <Card className={totalGain >= 0 ? 'bg-success-50 border-success-200' : 'bg-danger-50 border-danger-200'}>
          <div className="p-4">
            <p className={`text-sm ${totalGain >= 0 ? 'text-success-600' : 'text-danger-600'}`}>
              Total {totalGain >= 0 ? 'Gain' : 'Loss'}
            </p>
            <p className={`text-2xl font-bold ${totalGain >= 0 ? 'text-success-700' : 'text-danger-700'}`}>
              {formatCurrency(Math.abs(totalGain), true)} ({totalInvested > 0 ? ((totalGain / totalInvested) * 100).toFixed(1) : 0}%)
            </p>
          </div>
        </Card>
      </div>

      {/* Investments Table */}
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Name</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase">Invested</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase">Current</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase">Gain/Loss</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase">SIP/Monthly</th>
                <th className="px-6 py-4 text-center text-xs font-semibold text-slate-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-slate-400">
                    Loading...
                  </td>
                </tr>
              ) : investments.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-slate-400">
                    No investments yet. Click "Add Investment" to get started.
                  </td>
                </tr>
              ) : (
                investments.map((inv) => {
                  const gain = (inv.currentValue || 0) - (inv.investedAmount || 0);
                  const gainPercent = inv.investedAmount ? (gain / inv.investedAmount) * 100 : 0;
                  const typeInfo = INVESTMENT_TYPES.find(t => t.value === inv.type);
                  
                  return (
                    <tr key={inv.id} className="hover:bg-slate-50">
                      <td className="px-6 py-4">
                        <div className="flex items-start gap-2">
                          <span className="text-lg mt-0.5">{typeInfo?.icon}</span>
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-medium text-slate-800">{inv.name}</span>
                              {inv.isEmergencyFund && (
                                <span className="text-xs px-2 py-0.5 bg-amber-100 text-amber-700 rounded-full flex items-center gap-1">
                                  <Shield size={12} /> Emergency
                                </span>
                              )}
                            </div>
                            <div className="text-xs text-slate-500">{typeInfo?.label || inv.type}</div>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-right text-slate-800">
                        {formatCurrency(inv.investedAmount || 0, true)}
                      </td>
                      <td className="px-6 py-4 text-right text-slate-800">
                        {formatCurrency(inv.currentValue || 0, true)}
                      </td>
                      <td className={`px-6 py-4 text-right font-medium ${gain >= 0 ? 'text-success-600' : 'text-danger-600'}`}>
                        {gain >= 0 ? '+' : ''}{formatCurrency(gain, true)} ({gainPercent.toFixed(1)}%)
                      </td>
                      <td className="px-6 py-4 text-right text-slate-600">
                        {inv.monthlySip ? formatCurrency(inv.monthlySip, true) : '-'}
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex justify-center gap-2">
                          {canTagEmergency(inv.type) && (
                            <button
                              onClick={() => toggleEmergencyFund(inv)}
                              className={`p-2 rounded-lg transition-colors ${
                                inv.isEmergencyFund 
                                  ? 'bg-amber-100 text-amber-600 hover:bg-amber-200' 
                                  : 'text-slate-400 hover:bg-slate-100 hover:text-slate-600'
                              }`}
                              title={inv.isEmergencyFund ? 'Remove from Emergency Fund' : 'Tag as Emergency Fund'}
                            >
                              <Shield size={16} />
                            </button>
                          )}
                          <button
                            onClick={() => openEditModal(inv)}
                            className="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                          >
                            <Pencil size={16} />
                          </button>
                          <button
                            onClick={() => handleDelete(inv.id!)}
                            className="p-2 text-slate-400 hover:text-danger-600 hover:bg-danger-50 rounded-lg transition-colors"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Add/Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={editingItem ? 'Edit Investment' : 'Add Investment'}
        footer={
          <>
            <Button variant="secondary" onClick={closeModal}>Cancel</Button>
            <Button onClick={handleSubmit} isLoading={createMutation.isPending || updateMutation.isPending}>
              {editingItem ? 'Save Changes' : 'Add Investment'}
            </Button>
          </>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <Select
            label="Investment Type"
            value={formData.type || ''}
            onChange={e => {
              setFormData({ ...formData, type: e.target.value });
              if (formErrors.type) setFormErrors(prev => ({ ...prev, type: '' }));
            }}
            options={filteredInvestmentTypes.map(t => ({ value: t.value, label: `${t.icon} ${t.label}` }))}
            error={formErrors.type}
            required
          />

          <Input
            label="Name"
            value={formData.name || ''}
            onChange={e => {
              setFormData({ ...formData, name: e.target.value });
              if (formErrors.name) setFormErrors(prev => ({ ...prev, name: '' }));
            }}
            placeholder={selectedType === 'MUTUAL_FUND' ? 'e.g., Axis Bluechip Fund' : 
                        selectedType === 'FD' ? 'e.g., SBI Fixed Deposit' : 'Enter name'}
            error={formErrors.name}
            required
          />

          <div className="grid grid-cols-2 gap-4">
            {typeConfig.investedLabel && (
              <Input
                label={typeConfig.investedLabel}
                type="number"
                value={formData.investedAmount || ''}
                onChange={e => {
                  setFormData({ ...formData, investedAmount: Number(e.target.value) });
                  if (formErrors.investedAmount) setFormErrors(prev => ({ ...prev, investedAmount: '' }));
                }}
                placeholder="100000"
                helperText={amountInWordsHelper(formData.investedAmount)}
                error={formErrors.investedAmount}
                required
              />
            )}
            <Input
              label={typeConfig.currentLabel}
              type="number"
              value={formData.currentValue || ''}
              onChange={e => {
                setFormData({ ...formData, currentValue: Number(e.target.value) });
                if (formErrors.currentValue) setFormErrors(prev => ({ ...prev, currentValue: '' }));
              }}
              placeholder="120000"
              helperText={amountInWordsHelper(formData.currentValue)}
              error={formErrors.currentValue}
              required
            />
          </div>

          {/* SIP fields */}
          {typeConfig.showSip && (
            <div className="grid grid-cols-2 gap-4">
              <Input
                label={typeConfig.sipLabel}
                type="number"
                value={formData.monthlySip || ''}
                onChange={e => {
                  setFormData({ ...formData, monthlySip: Number(e.target.value) });
                  if (formErrors.monthlySip) setFormErrors(prev => ({ ...prev, monthlySip: '' }));
                }}
                placeholder="5000"
                helperText={amountInWordsHelper(formData.monthlySip) || (selectedType === 'MUTUAL_FUND' ? 'Leave empty for lumpsum' : undefined)}
                error={formErrors.monthlySip}
              />
              {typeConfig.showSipDay && (
                <Input
                  label="SIP Debit Day"
                  type="number"
                  min={1}
                  max={28}
                  value={formData.sipDay || ''}
                  onChange={e => {
                    setFormData({ ...formData, sipDay: Number(e.target.value) });
                    if (formErrors.sipDay) setFormErrors(prev => ({ ...prev, sipDay: '' }));
                  }}
                  placeholder="5"
                  helperText="Day of month (1-28)"
                  error={formErrors.sipDay}
                />
              )}
              {typeConfig.showRdDay && (
                <Input
                  label="RD Debit Day"
                  type="number"
                  min={1}
                  max={28}
                  value={formData.rdDay || ''}
                  onChange={e => {
                    setFormData({ ...formData, rdDay: Number(e.target.value) });
                    if (formErrors.rdDay) setFormErrors(prev => ({ ...prev, rdDay: '' }));
                  }}
                  placeholder="1"
                  helperText="Day of month (1-28)"
                  error={formErrors.rdDay}
                />
              )}
            </div>
          )}

          {/* PPF yearly contribution */}
          {typeConfig.showYearlyContribution && (
            <Input
              label="Yearly Contribution (₹)"
              type="number"
              value={formData.yearlyContribution || ''}
              onChange={e => {
                setFormData({ ...formData, yearlyContribution: Number(e.target.value) });
                if (formErrors.yearlyContribution) setFormErrors(prev => ({ ...prev, yearlyContribution: '' }));
              }}
              placeholder="150000"
              helperText={amountInWordsHelper(formData.yearlyContribution) || 'Max ₹1,50,000 per year'}
              error={formErrors.yearlyContribution}
            />
          )}

          {/* Expected return / Interest rate */}
          <div className="grid grid-cols-2 gap-4">
            <Input
              label={typeConfig.showInterestRate ? 'Interest Rate (%)' : 'Expected Annual Return (%)'}
              type="number"
              step="0.1"
              value={formData.expectedReturn || ''}
              onChange={e => setFormData({ ...formData, expectedReturn: Number(e.target.value) })}
              placeholder={String(typeConfig.defaultReturn)}
            />
            {typeConfig.dateLabel && (
              <Input
                label={typeConfig.dateLabel}
                type="date"
                value={formData.purchaseDate?.split('T')[0] || ''}
                onChange={e => setFormData({ ...formData, purchaseDate: e.target.value })}
              />
            )}
          </div>

          {/* Maturity date for FD/RD */}
          {typeConfig.showMaturityDate && (
            <Input
              label="Maturity Date"
              type="date"
              value={formData.maturityDate?.split('T')[0] || ''}
              onChange={e => setFormData({ ...formData, maturityDate: e.target.value })}
            />
          )}

          {/* Emergency fund checkbox for FD/RD */}
          {typeConfig.showEmergencyFund && (
            <div className="flex items-center gap-2 p-3 bg-amber-50 border border-amber-200 rounded-lg">
              <input
                type="checkbox"
                id="isEmergencyFund"
                checked={formData.isEmergencyFund || false}
                onChange={e => setFormData({ ...formData, isEmergencyFund: e.target.checked })}
                className="w-4 h-4 text-amber-600 rounded"
              />
              <label htmlFor="isEmergencyFund" className="text-sm text-amber-800">
                <Shield size={14} className="inline mr-1" />
                Tag as Emergency Fund (excluded from retirement corpus)
              </label>
            </div>
          )}
        </form>
      </Modal>
    </MainLayout>
  );
}

export default Investments;
