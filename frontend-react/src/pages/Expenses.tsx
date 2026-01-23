import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, Button, Modal, Input, Select, toast } from '../components/ui';
import { expensesApi, Expense } from '../lib/api';
import { amountInWordsHelper, formatCurrency } from '../lib/utils';
import { Plus, Pencil, Trash2, ShoppingCart, TrendingUp, AlertCircle } from 'lucide-react';

const EXPENSE_CATEGORIES = [
  { value: 'RENT', label: 'Rent/Housing' },
  { value: 'SOCIETY_CHARGES', label: 'Society Charges' },
  { value: 'UTILITIES', label: 'Utilities' },
  { value: 'GROCERIES', label: 'Groceries' },
  { value: 'TRANSPORT', label: 'Transportation' },
  { value: 'HEALTHCARE', label: 'Healthcare' },
  { value: 'INSURANCE_PREMIUM', label: 'Insurance Premiums' },
  { value: 'MAINTENANCE', label: 'Maintenance' },
  { value: 'ENTERTAINMENT', label: 'Entertainment' },
  { value: 'SUBSCRIPTIONS', label: 'Subscriptions' },
  { value: 'SHOPPING', label: 'Shopping' },
  { value: 'DINING', label: 'Dining Out' },
  { value: 'TRAVEL', label: 'Travel' },
  { value: 'CHILDCARE', label: 'Childcare' },
  { value: 'DAYCARE', label: 'Daycare' },
  { value: 'SCHOOL_FEE', label: 'School Fee' },
  { value: 'TUITION', label: 'Tuition' },
  { value: 'COLLEGE_FEE', label: 'College Fee' },
  { value: 'COACHING', label: 'Coaching' },
  { value: 'BOOKS_SUPPLIES', label: 'Books & Supplies' },
  { value: 'HOSTEL', label: 'Hostel' },
  { value: 'ELDERLY_CARE', label: 'Elderly Care' },
  { value: 'OTHER', label: 'Other' },
];

const FREQUENCY_OPTIONS = [
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'QUARTERLY', label: 'Quarterly' },
  { value: 'HALF_YEARLY', label: 'Half Yearly' },
  { value: 'YEARLY', label: 'Yearly' },
  { value: 'ONE_TIME', label: 'One Time' },
];

export function Expenses() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<Expense | null>(null);
  const [formData, setFormData] = useState<Partial<Expense>>({});
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  const { data: expenses = [], isLoading } = useQuery({
    queryKey: ['expenses'],
    queryFn: expensesApi.getAll,
  });

  const createMutation = useMutation({
    mutationFn: (data: Expense) => expensesApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] });
      toast.success('Expense added successfully');
      closeModal();
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Expense }) => expensesApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] });
      toast.success('Expense updated successfully');
      closeModal();
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => expensesApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] });
      toast.success('Expense deleted successfully');
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const openAddModal = () => {
    setEditingItem(null);
    setFormData({ isEssential: true, frequency: 'MONTHLY' });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const openEditModal = (item: Expense) => {
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
    if (!formData.name) errors.name = 'Name is required';
    if (!formData.category) errors.category = 'Category is required';
    if (!formData.amount || Number(formData.amount) <= 0) errors.amount = 'Enter a valid amount';
    if (!formData.frequency) errors.frequency = 'Frequency is required';
    if (formData.endAge !== undefined && Number(formData.endAge) < 0) errors.endAge = 'Enter a valid age';
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
      amount: Number(formData.amount) || 0,
      endAge: formData.endAge ? Number(formData.endAge) : undefined,
      annualIncreasePercent: formData.annualIncreasePercent ? Number(formData.annualIncreasePercent) : undefined,
      // Clear time-bound fields if not time-bound
      ...(formData.isTimeBound ? {} : {
        startDate: undefined,
        endDate: undefined,
        dependentName: undefined,
        dependentDob: undefined,
        endAge: undefined,
        annualIncreasePercent: undefined,
      }),
    } as Expense;
    if (editingItem?.id) {
      updateMutation.mutate({ id: editingItem.id, data });
    } else {
      createMutation.mutate(data);
    }
  };

  const handleDelete = (id: string) => {
    if (confirm('Are you sure you want to delete this expense?')) {
      deleteMutation.mutate(id);
    }
  };

  // Calculate totals
  const monthlyTotal = expenses.reduce((sum, exp) => {
    const amount = exp.amount || 0;
    switch (exp.frequency) {
      case 'MONTHLY': return sum + amount;
      case 'QUARTERLY': return sum + amount / 3;
      case 'YEARLY': return sum + amount / 12;
      default: return sum + amount;
    }
  }, 0);

  const essentialExpenses = expenses.filter(e => e.isEssential).reduce((sum, exp) => {
    const amount = exp.amount || 0;
    switch (exp.frequency) {
      case 'MONTHLY': return sum + amount;
      case 'QUARTERLY': return sum + amount / 3;
      case 'YEARLY': return sum + amount / 12;
      default: return sum + amount;
    }
  }, 0);

  const discretionaryExpenses = monthlyTotal - essentialExpenses;

  return (
    <MainLayout
      title="Expenses"
      subtitle="Track your monthly expenses"
      actions={
        <Button onClick={openAddModal}>
          <Plus size={18} className="mr-2" /> Add Expense
        </Button>
      }
    >
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card className="bg-danger-50 border-danger-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <ShoppingCart className="text-danger-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-danger-600">Monthly Expenses</p>
              <p className="text-2xl font-bold text-danger-700">{formatCurrency(monthlyTotal, true)}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-warning-50 border-warning-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <AlertCircle className="text-warning-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-warning-600">Essential</p>
              <p className="text-2xl font-bold text-warning-700">{formatCurrency(essentialExpenses, true)}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-primary-50 border-primary-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <TrendingUp className="text-primary-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-primary-600">Discretionary</p>
              <p className="text-2xl font-bold text-primary-700">{formatCurrency(discretionaryExpenses, true)}</p>
            </div>
          </div>
        </Card>
      </div>

      {/* Expenses Table */}
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Expense</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Category</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase">Amount</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-slate-500 uppercase">Frequency</th>
                <th className="px-6 py-4 text-center text-xs font-semibold text-slate-500 uppercase">Essential</th>
                <th className="px-6 py-4 text-center text-xs font-semibold text-slate-500 uppercase">Time-Bound</th>
                <th className="px-6 py-4 text-center text-xs font-semibold text-slate-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr><td colSpan={7} className="px-6 py-8 text-center text-slate-400">Loading...</td></tr>
              ) : expenses.length === 0 ? (
                <tr><td colSpan={7} className="px-6 py-8 text-center text-slate-400">No expenses yet. Click "Add Expense" to get started.</td></tr>
              ) : (
                expenses.map((exp) => (
                  <tr key={exp.id} className={`hover:bg-slate-50 ${exp.isTimeBound ? 'border-l-4 border-purple-400' : ''}`}>
                    <td className="px-6 py-4 font-medium text-slate-800">{exp.name}</td>
                    <td className="px-6 py-4 text-slate-600">
                      {EXPENSE_CATEGORIES.find(c => c.value === exp.category)?.label || exp.category}
                    </td>
                    <td className="px-6 py-4 text-right text-danger-600 font-medium">{formatCurrency(exp.amount, true)}</td>
                    <td className="px-6 py-4 text-slate-600">
                      {FREQUENCY_OPTIONS.find(f => f.value === exp.frequency)?.label || exp.frequency}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className={`px-2 py-1 text-xs rounded-full ${exp.isEssential ? 'bg-warning-100 text-warning-700' : 'bg-slate-100 text-slate-600'}`}>
                        {exp.isEssential ? 'Yes' : 'No'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-center">
                      {exp.isTimeBound ? (
                        <div>
                          <span className="px-2 py-0.5 text-xs rounded-full bg-purple-100 text-purple-700">Time-Bound</span>
                          <div className="text-xs text-purple-600 mt-1">
                            {exp.endDate ? (
                              new Date(exp.endDate).toLocaleDateString()
                            ) : exp.endAge && exp.dependentDob ? (
                              (() => {
                                const dob = new Date(exp.dependentDob);
                                const age = Math.floor((Date.now() - dob.getTime()) / (365.25 * 24 * 60 * 60 * 1000));
                                const yearsLeft = exp.endAge - age;
                                const endYear = new Date().getFullYear() + yearsLeft;
                                return `~${endYear} (${exp.dependentName || 'Dep.'} turns ${exp.endAge})`;
                              })()
                            ) : exp.endAge ? (
                              `Ends at age ${exp.endAge}`
                            ) : '-'}
                          </div>
                        </div>
                      ) : (
                        <span className="text-slate-400">-</span>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex justify-center gap-2">
                        <button onClick={() => openEditModal(exp)} className="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg">
                          <Pencil size={16} />
                        </button>
                        <button onClick={() => handleDelete(exp.id!)} className="p-2 text-slate-400 hover:text-danger-600 hover:bg-danger-50 rounded-lg">
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
        title={editingItem ? 'Edit Expense' : 'Add Expense'}
        footer={
          <>
            <Button variant="secondary" onClick={closeModal}>Cancel</Button>
            <Button onClick={handleSubmit} isLoading={createMutation.isPending || updateMutation.isPending}>
              {editingItem ? 'Save Changes' : 'Add Expense'}
            </Button>
          </>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Expense Name"
            value={formData.name || ''}
            onChange={e => {
              setFormData({ ...formData, name: e.target.value });
              if (formErrors.name) setFormErrors(prev => ({ ...prev, name: '' }));
            }}
            placeholder="e.g., Monthly Rent"
            error={formErrors.name}
            required
          />
          <Select
            label="Category"
            value={formData.category || ''}
            onChange={e => {
              setFormData({ ...formData, category: e.target.value });
              if (formErrors.category) setFormErrors(prev => ({ ...prev, category: '' }));
            }}
            options={EXPENSE_CATEGORIES}
            error={formErrors.category}
            required
          />
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Amount"
              type="number"
              value={formData.amount || ''}
              onChange={e => {
                setFormData({ ...formData, amount: Number(e.target.value) });
                if (formErrors.amount) setFormErrors(prev => ({ ...prev, amount: '' }));
              }}
              placeholder="25000"
              helperText={amountInWordsHelper(formData.amount)}
              error={formErrors.amount}
              required
            />
            <Select
              label="Frequency"
              value={formData.frequency || ''}
              onChange={e => {
                setFormData({ ...formData, frequency: e.target.value });
                if (formErrors.frequency) setFormErrors(prev => ({ ...prev, frequency: '' }));
              }}
              options={FREQUENCY_OPTIONS}
              error={formErrors.frequency}
              required
            />
          </div>
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="isFixed"
              checked={formData.isFixed || false}
              onChange={e => setFormData({ ...formData, isFixed: e.target.checked })}
              className="w-4 h-4 text-primary-600 rounded"
            />
            <label htmlFor="isFixed" className="text-sm text-slate-700">This is a fixed expense</label>
          </div>
          
          {/* Time-Bound Expense Checkbox */}
          <div className="p-3 bg-purple-50 rounded-lg border border-purple-200">
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="isTimeBound"
                checked={formData.isTimeBound || false}
                onChange={e => setFormData({ ...formData, isTimeBound: e.target.checked })}
                className="w-4 h-4 text-purple-600 rounded border-purple-300"
              />
              <label htmlFor="isTimeBound" className="text-sm text-purple-800 font-medium">
                This expense has an end date (e.g., school fees until child graduates)
              </label>
            </div>
            
            {/* Time-Bound Fields - shown only when isTimeBound is checked */}
            {formData.isTimeBound && (
              <div className="mt-4 space-y-4 pt-3 border-t border-purple-200">
                <div className="grid grid-cols-2 gap-4">
                  <Input
                    label="Start Date"
                    type="date"
                    value={formData.startDate || ''}
                    onChange={e => setFormData({ ...formData, startDate: e.target.value })}
                    helperText="When did/will this expense start?"
                  />
                  <Input
                    label="End Date"
                    type="date"
                    value={formData.endDate || ''}
                    onChange={e => setFormData({ ...formData, endDate: e.target.value })}
                    helperText="Specific end date (optional)"
                  />
                </div>
                
                <div className="p-3 bg-white rounded-lg border border-purple-100">
                  <p className="text-xs text-purple-600 font-medium mb-2">OR link to a dependent's age:</p>
                  <div className="grid grid-cols-3 gap-3">
                    <Input
                      label="Dependent Name"
                      value={formData.dependentName || ''}
                      onChange={e => setFormData({ ...formData, dependentName: e.target.value })}
                      placeholder="e.g., Child's name"
                    />
                    <Input
                      label="Dependent DOB"
                      type="date"
                      value={formData.dependentDob || ''}
                      onChange={e => setFormData({ ...formData, dependentDob: e.target.value })}
                    />
                    <Input
                      label="End at Age"
                      type="number"
                      value={formData.endAge || ''}
                      onChange={e => setFormData({ ...formData, endAge: Number(e.target.value) || undefined })}
                      placeholder="e.g., 18"
                      helperText="Expense ends when dependent reaches this age"
                    />
                  </div>
                  {formData.dependentDob && formData.endAge && (
                    <div className="mt-2 text-xs text-purple-600">
                      {(() => {
                        const dob = new Date(formData.dependentDob);
                        const age = Math.floor((Date.now() - dob.getTime()) / (365.25 * 24 * 60 * 60 * 1000));
                        const yearsLeft = (formData.endAge || 0) - age;
                        const endYear = new Date().getFullYear() + yearsLeft;
                        return `Current age: ${age} years. Expense ends in ${yearsLeft} years (${endYear})`;
                      })()}
                    </div>
                  )}
                </div>
                
                <Input
                  label="Annual Increase %"
                  type="number"
                  step="0.1"
                  value={formData.annualIncreasePercent || ''}
                  onChange={e => setFormData({ ...formData, annualIncreasePercent: Number(e.target.value) || undefined })}
                  placeholder="e.g., 10"
                  helperText="Expected annual increase (like school fee hike)"
                />
              </div>
            )}
          </div>
        </form>
      </Modal>
    </MainLayout>
  );
}

export default Expenses;
