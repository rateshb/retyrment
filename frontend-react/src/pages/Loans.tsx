import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, Button, Modal, Input, Select, toast } from '../components/ui';
import { loansApi, Loan } from '../lib/api';
import { amountInWordsHelper, formatCurrency, getCurrencySymbol } from '../lib/utils';
import { Plus, Pencil, Trash2, Building2, TrendingDown, Calendar } from 'lucide-react';

const LOAN_TYPES = [
  { value: 'HOME', label: 'Home Loan' },
  { value: 'VEHICLE', label: 'Vehicle Loan' },
  { value: 'PERSONAL', label: 'Personal Loan' },
  { value: 'EDUCATION', label: 'Education Loan' },
  { value: 'CREDIT_CARD', label: 'Credit Card' },
  { value: 'OTHER', label: 'Other' },
];

export function Loans() {
  const queryClient = useQueryClient();
  const currencySymbol = getCurrencySymbol();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<Loan | null>(null);
  const [formData, setFormData] = useState<Partial<Loan>>({});
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  const { data: loans = [], isLoading } = useQuery({
    queryKey: ['loans'],
    queryFn: loansApi.getAll,
  });

  const createMutation = useMutation({
    mutationFn: (data: Loan) => loansApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      toast.success('Loan added successfully');
      closeModal();
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Loan }) => loansApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      toast.success('Loan updated successfully');
      closeModal();
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => loansApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      toast.success('Loan deleted successfully');
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const openAddModal = () => {
    setEditingItem(null);
    setFormData({ type: 'HOME', emiDay: 1 });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const openEditModal = (item: Loan) => {
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
    if (!formData.name) errors.name = 'Loan name is required';
    if (!formData.type) errors.type = 'Loan type is required';
    if (!formData.originalAmount || Number(formData.originalAmount) <= 0) errors.originalAmount = 'Enter a valid amount';
    if (!formData.outstandingAmount || Number(formData.outstandingAmount) < 0) errors.outstandingAmount = 'Enter a valid amount';
    if (!formData.emi || Number(formData.emi) <= 0) errors.emi = 'Enter a valid EMI';
    if (!formData.emiDay || Number(formData.emiDay) < 1 || Number(formData.emiDay) > 31) errors.emiDay = 'Enter EMI debit day (1-31)';
    if (!formData.interestRate || Number(formData.interestRate) <= 0) errors.interestRate = 'Enter a valid rate';
    if (!formData.startDate) errors.startDate = 'Start date is required';
    if (!formData.tenureMonths && !formData.remainingMonths) {
      errors.tenureMonths = 'Enter tenure or remaining months';
      errors.remainingMonths = 'Enter tenure or remaining months';
    }
    if (formData.originalAmount && formData.outstandingAmount && Number(formData.outstandingAmount) > Number(formData.originalAmount)) {
      errors.outstandingAmount = 'Outstanding cannot exceed original amount';
    }
    if (formData.tenureMonths && formData.remainingMonths && Number(formData.remainingMonths) > Number(formData.tenureMonths)) {
      errors.remainingMonths = 'Remaining months cannot exceed tenure';
    }
    if (formData.startDate && formData.endDate && new Date(formData.endDate) < new Date(formData.startDate)) {
      errors.endDate = 'End date must be after start date';
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
      originalAmount: Number(formData.originalAmount) || 0,
      outstandingAmount: Number(formData.outstandingAmount) || 0,
      emi: Number(formData.emi) || 0,
      emiDay: Number(formData.emiDay) || undefined,
      interestRate: Number(formData.interestRate) || 0,
      tenureMonths: formData.tenureMonths ? Number(formData.tenureMonths) : undefined,
      remainingMonths: formData.remainingMonths ? Number(formData.remainingMonths) : undefined,
    } as Loan;
    if (editingItem?.id) {
      updateMutation.mutate({ id: editingItem.id, data });
    } else {
      createMutation.mutate(data);
    }
  };

  const handleDelete = (id: string) => {
    if (confirm('Are you sure you want to delete this loan?')) {
      deleteMutation.mutate(id);
    }
  };

  // Calculate totals
  const totalOutstanding = loans.reduce((sum, loan) => sum + (loan.outstandingAmount || 0), 0);
  const totalEmi = loans.reduce((sum, loan) => sum + (loan.emi || 0), 0);
  const loanCount = loans.length;

  return (
    <MainLayout
      title="Loans"
      subtitle="Track your liabilities"
      actions={
        <Button onClick={openAddModal}>
          <Plus size={18} className="mr-2" /> Add Loan
        </Button>
      }
    >
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card className="bg-danger-50 border-danger-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <Building2 className="text-danger-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-danger-600">Total Outstanding</p>
              <p className="text-2xl font-bold text-danger-700">{formatCurrency(totalOutstanding, true)}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-warning-50 border-warning-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <TrendingDown className="text-warning-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-warning-600">Monthly EMI</p>
              <p className="text-2xl font-bold text-warning-700">{formatCurrency(totalEmi, true)}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-primary-50 border-primary-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <Calendar className="text-primary-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-primary-600">Active Loans</p>
              <p className="text-2xl font-bold text-primary-700">{loanCount}</p>
            </div>
          </div>
        </Card>
      </div>

      {/* Loans Table */}
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Loan</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Type</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase">Outstanding</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase">EMI</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase">Interest Rate</th>
                <th className="px-6 py-4 text-center text-xs font-semibold text-slate-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr><td colSpan={6} className="px-6 py-8 text-center text-slate-400">Loading...</td></tr>
              ) : loans.length === 0 ? (
                <tr><td colSpan={6} className="px-6 py-8 text-center text-slate-400">No loans yet. Click "Add Loan" to track your liabilities.</td></tr>
              ) : (
                loans.map((loan) => (
                  <tr key={loan.id} className="hover:bg-slate-50">
                    <td className="px-6 py-4 font-medium text-slate-800">{loan.name}</td>
                    <td className="px-6 py-4 text-slate-600">
                      {LOAN_TYPES.find(t => t.value === loan.type)?.label || loan.type}
                    </td>
                    <td className="px-6 py-4 text-right text-danger-600 font-medium">{formatCurrency(loan.outstandingAmount, true)}</td>
                    <td className="px-6 py-4 text-right text-slate-800">{formatCurrency(loan.emi, true)}</td>
                    <td className="px-6 py-4 text-right text-slate-600">{loan.interestRate}%</td>
                    <td className="px-6 py-4">
                      <div className="flex justify-center gap-2">
                        <button onClick={() => openEditModal(loan)} className="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg">
                          <Pencil size={16} />
                        </button>
                        <button onClick={() => handleDelete(loan.id!)} className="p-2 text-slate-400 hover:text-danger-600 hover:bg-danger-50 rounded-lg">
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
        title={editingItem ? 'Edit Loan' : 'Add Loan'}
        footer={
          <>
            <Button variant="secondary" onClick={closeModal}>Cancel</Button>
            <Button onClick={handleSubmit} isLoading={createMutation.isPending || updateMutation.isPending}>
              {editingItem ? 'Save Changes' : 'Add Loan'}
            </Button>
          </>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Loan Name"
            value={formData.name || ''}
            onChange={e => {
              setFormData({ ...formData, name: e.target.value });
              if (formErrors.name) setFormErrors(prev => ({ ...prev, name: '' }));
            }}
            placeholder="e.g., HDFC Home Loan"
            error={formErrors.name}
            required
          />
          <Select
            label="Loan Type"
            value={formData.type || ''}
            onChange={e => {
              setFormData({ ...formData, type: e.target.value as Loan['type'] });
              if (formErrors.type) setFormErrors(prev => ({ ...prev, type: '' }));
            }}
            options={LOAN_TYPES}
            error={formErrors.type}
            required
          />
          <div className="grid grid-cols-2 gap-4">
            <Input
              label={`Original Amount (${currencySymbol})`}
              type="number"
              value={formData.originalAmount || ''}
              onChange={e => {
                setFormData({ ...formData, originalAmount: Number(e.target.value) });
                if (formErrors.originalAmount) setFormErrors(prev => ({ ...prev, originalAmount: '' }));
              }}
              placeholder="5000000"
              helperText={amountInWordsHelper(formData.originalAmount)}
              error={formErrors.originalAmount}
              required
            />
            <Input
              label={`Outstanding Amount (${currencySymbol})`}
              type="number"
              value={formData.outstandingAmount || ''}
              onChange={e => {
                setFormData({ ...formData, outstandingAmount: Number(e.target.value) });
                if (formErrors.outstandingAmount) setFormErrors(prev => ({ ...prev, outstandingAmount: '' }));
              }}
              placeholder="4500000"
              helperText={amountInWordsHelper(formData.outstandingAmount)}
              error={formErrors.outstandingAmount}
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input
              label={`Monthly EMI (${currencySymbol})`}
              type="number"
              value={formData.emi || ''}
              onChange={e => {
                setFormData({ ...formData, emi: Number(e.target.value) });
                if (formErrors.emi) setFormErrors(prev => ({ ...prev, emi: '' }));
              }}
              placeholder="45000"
              helperText={amountInWordsHelper(formData.emi)}
              error={formErrors.emi}
              required
            />
            <Input
              label="EMI Debit Day (1-31)"
              type="number"
              value={formData.emiDay || ''}
              onChange={e => {
                setFormData({ ...formData, emiDay: Number(e.target.value) });
                if (formErrors.emiDay) setFormErrors(prev => ({ ...prev, emiDay: '' }));
              }}
              placeholder="5"
              error={formErrors.emiDay}
              required
            />
            <Input
              label="Interest Rate (%)"
              type="number"
              value={formData.interestRate || ''}
              onChange={e => {
                setFormData({ ...formData, interestRate: Number(e.target.value) });
                if (formErrors.interestRate) setFormErrors(prev => ({ ...prev, interestRate: '' }));
              }}
              placeholder="8.5"
              step="0.01"
              error={formErrors.interestRate}
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Tenure (Months)"
              type="number"
              value={formData.tenureMonths || ''}
              onChange={e => {
                setFormData({ ...formData, tenureMonths: Number(e.target.value) });
                if (formErrors.tenureMonths) setFormErrors(prev => ({ ...prev, tenureMonths: '' }));
                if (formErrors.remainingMonths) setFormErrors(prev => ({ ...prev, remainingMonths: '' }));
              }}
              placeholder="240"
              error={formErrors.tenureMonths}
            />
            <Input
              label="Remaining Months"
              type="number"
              value={formData.remainingMonths || ''}
              onChange={e => {
                setFormData({ ...formData, remainingMonths: Number(e.target.value) });
                if (formErrors.remainingMonths) setFormErrors(prev => ({ ...prev, remainingMonths: '' }));
              }}
              placeholder="180"
              error={formErrors.remainingMonths}
            />
          </div>
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
            <Input
              label="End Date"
              type="date"
              value={formData.endDate?.split('T')[0] || ''}
              onChange={e => {
                setFormData({ ...formData, endDate: e.target.value });
                if (formErrors.endDate) setFormErrors(prev => ({ ...prev, endDate: '' }));
              }}
              error={formErrors.endDate}
            />
          </div>
        </form>
      </Modal>
    </MainLayout>
  );
}

export default Loans;
