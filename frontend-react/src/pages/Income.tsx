import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, Button, Modal, Input, toast } from '../components/ui';
import { incomeApi, Income as IncomeType } from '../lib/api';
import { amountInWordsHelper, formatCurrency, getCurrencySymbol } from '../lib/utils';
import { Plus, Pencil, Trash2, Wallet, TrendingUp, Calendar } from 'lucide-react';

export function Income() {
  const queryClient = useQueryClient();
  const currencySymbol = getCurrencySymbol();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<IncomeType | null>(null);
  const [formData, setFormData] = useState<Partial<IncomeType>>({});
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  const { data: incomes = [], isLoading } = useQuery({
    queryKey: ['income'],
    queryFn: incomeApi.getAll,
  });

  const createMutation = useMutation({
    mutationFn: (data: IncomeType) => incomeApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['income'] });
      toast.success('Income added successfully');
      closeModal();
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: IncomeType }) => incomeApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['income'] });
      toast.success('Income updated successfully');
      closeModal();
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => incomeApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['income'] });
      toast.success('Income deleted successfully');
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const openAddModal = () => {
    setEditingItem(null);
    setFormData({ isActive: true, annualIncrement: 7 });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const openEditModal = (item: IncomeType) => {
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
    if (!formData.source) errors.source = 'Source is required';
    if (!formData.monthlyAmount || Number(formData.monthlyAmount) <= 0) errors.monthlyAmount = 'Enter a valid amount';
    if (formData.annualIncrement !== undefined && Number(formData.annualIncrement) < 0) errors.annualIncrement = 'Enter a valid increment';
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
      monthlyAmount: Number(formData.monthlyAmount) || 0,
      annualIncrement: Number(formData.annualIncrement) || 7,
    } as IncomeType;
    if (editingItem?.id) {
      updateMutation.mutate({ id: editingItem.id, data });
    } else {
      createMutation.mutate(data);
    }
  };

  const handleDelete = (id: string) => {
    if (confirm('Are you sure you want to delete this income source?')) {
      deleteMutation.mutate(id);
    }
  };

  // Calculate totals
  const monthlyTotal = incomes.reduce((sum, inc) => sum + (inc.monthlyAmount || 0), 0);
  const yearlyTotal = monthlyTotal * 12;
  const activeCount = incomes.filter(i => i.isActive !== false).length;

  return (
    <MainLayout
      title="Income"
      subtitle="Track your income sources"
      actions={
        <Button onClick={openAddModal}>
          <Plus size={18} className="mr-2" /> Add Income
        </Button>
      }
    >
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card className="bg-success-50 border-success-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <Wallet className="text-success-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-success-600">Monthly Income</p>
              <p className="text-2xl font-bold text-success-700">{formatCurrency(monthlyTotal, true)}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-primary-50 border-primary-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <TrendingUp className="text-primary-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-primary-600">Yearly Income</p>
              <p className="text-2xl font-bold text-primary-700">{formatCurrency(yearlyTotal, true)}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-warning-50 border-warning-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <Calendar className="text-warning-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-warning-600">Active Sources</p>
              <p className="text-2xl font-bold text-warning-700">{activeCount}</p>
            </div>
          </div>
        </Card>
      </div>

      {/* Income Table */}
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Source</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase">Monthly Amount</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase">Annual Increment</th>
                <th className="px-6 py-4 text-center text-xs font-semibold text-slate-500 uppercase">Status</th>
                <th className="px-6 py-4 text-center text-xs font-semibold text-slate-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr><td colSpan={5} className="px-6 py-8 text-center text-slate-400">Loading...</td></tr>
              ) : incomes.length === 0 ? (
                <tr><td colSpan={5} className="px-6 py-8 text-center text-slate-400">No income sources yet. Click "Add Income" to get started.</td></tr>
              ) : (
                incomes.map((inc) => (
                  <tr key={inc.id} className="hover:bg-slate-50">
                    <td className="px-6 py-4 font-medium text-slate-800">{inc.source}</td>
                    <td className="px-6 py-4 text-right text-success-600 font-medium">{formatCurrency(inc.monthlyAmount, true)}</td>
                    <td className="px-6 py-4 text-right text-slate-600">{inc.annualIncrement || 0}%</td>
                    <td className="px-6 py-4 text-center">
                      <span className={`px-2 py-1 text-xs rounded-full ${inc.isActive !== false ? 'bg-success-100 text-success-700' : 'bg-slate-100 text-slate-600'}`}>
                        {inc.isActive !== false ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex justify-center gap-2">
                        <button onClick={() => openEditModal(inc)} className="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg">
                          <Pencil size={16} />
                        </button>
                        <button onClick={() => handleDelete(inc.id!)} className="p-2 text-slate-400 hover:text-danger-600 hover:bg-danger-50 rounded-lg">
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
        title={editingItem ? 'Edit Income' : 'Add Income'}
        footer={
          <>
            <Button variant="secondary" onClick={closeModal}>Cancel</Button>
            <Button onClick={handleSubmit} isLoading={createMutation.isPending || updateMutation.isPending}>
              {editingItem ? 'Save Changes' : 'Add Income'}
            </Button>
          </>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Income Source"
            value={formData.source || ''}
            onChange={e => {
              setFormData({ ...formData, source: e.target.value });
              if (formErrors.source) setFormErrors(prev => ({ ...prev, source: '' }));
            }}
            placeholder="e.g., Salary - ABC Company"
            error={formErrors.source}
            required
          />
          <div className="grid grid-cols-2 gap-4">
            <Input
              label={`Monthly Amount (${currencySymbol})`}
              type="number"
              value={formData.monthlyAmount || ''}
              onChange={e => {
                setFormData({ ...formData, monthlyAmount: Number(e.target.value) });
                if (formErrors.monthlyAmount) setFormErrors(prev => ({ ...prev, monthlyAmount: '' }));
              }}
              placeholder="50000"
              helperText={amountInWordsHelper(formData.monthlyAmount)}
              error={formErrors.monthlyAmount}
              required
            />
            <Input
              label="Annual Increment (%)"
              type="number"
              value={formData.annualIncrement ?? 7}
              onChange={e => {
                setFormData({ ...formData, annualIncrement: Number(e.target.value) });
                if (formErrors.annualIncrement) setFormErrors(prev => ({ ...prev, annualIncrement: '' }));
              }}
              placeholder="7"
              step="0.1"
              error={formErrors.annualIncrement}
            />
          </div>
          <Input
            label="Start Date"
            type="date"
            value={formData.startDate?.split('T')[0] || ''}
            onChange={e => setFormData({ ...formData, startDate: e.target.value })}
          />
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="isActive"
              checked={formData.isActive !== false}
              onChange={e => setFormData({ ...formData, isActive: e.target.checked })}
              className="w-4 h-4 text-primary-600 rounded"
            />
            <label htmlFor="isActive" className="text-sm text-slate-700">This income source is active</label>
          </div>
        </form>
      </Modal>
    </MainLayout>
  );
}

export default Income;
