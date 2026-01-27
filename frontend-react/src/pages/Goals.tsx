import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, Button, Modal, Input, Select, toast } from '../components/ui';
import { goalsApi, Goal } from '../lib/api';
import { amountInWordsHelper, formatCurrency } from '../lib/utils';
import { Plus, Pencil, Trash2, Target, TrendingUp, AlertTriangle, CheckCircle } from 'lucide-react';

const PRIORITY_OPTIONS = [
  { value: 'HIGH', label: 'High Priority' },
  { value: 'MEDIUM', label: 'Medium Priority' },
  { value: 'LOW', label: 'Low Priority' },
];

export function Goals() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<Goal | null>(null);
  const [formData, setFormData] = useState<Partial<Goal>>({});
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  const { data: goals = [], isLoading } = useQuery({
    queryKey: ['goals'],
    queryFn: goalsApi.getAll,
  });

  const createMutation = useMutation({
    mutationFn: (data: Goal) => goalsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['goals'] });
      toast.success('Goal added successfully');
      closeModal();
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Goal }) => goalsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['goals'] });
      toast.success('Goal updated successfully');
      closeModal();
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => goalsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['goals'] });
      toast.success('Goal deleted successfully');
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const openAddModal = () => {
    setEditingItem(null);
    setFormData({ priority: 'MEDIUM', targetYear: new Date().getFullYear() + 5 });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const openEditModal = (item: Goal) => {
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
    const currentYear = new Date().getFullYear();
    if (!formData.name) errors.name = 'Goal name is required';
    if (!formData.targetAmount || Number(formData.targetAmount) <= 0) errors.targetAmount = 'Enter a valid target';
    if (formData.currentSavings !== undefined && Number(formData.currentSavings) < 0) errors.currentSavings = 'Enter a valid amount';
    if (!formData.targetYear || Number(formData.targetYear) < currentYear) errors.targetYear = 'Enter a valid year';
    if (!formData.priority) errors.priority = 'Priority is required';
    if (formData.isRecurring) {
      if (!formData.recurrenceInterval || Number(formData.recurrenceInterval) <= 0) errors.recurrenceInterval = 'Enter a valid interval';
      if (formData.recurrenceEndYear && Number(formData.recurrenceEndYear) < Number(formData.targetYear || currentYear)) {
        errors.recurrenceEndYear = 'End year must be after target year';
      }
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
      targetAmount: Number(formData.targetAmount) || 0,
      currentSavings: Number(formData.currentSavings) || 0,
      targetYear: Number(formData.targetYear) || new Date().getFullYear() + 5,
      recurrenceInterval: formData.isRecurring ? Number(formData.recurrenceInterval) || 1 : undefined,
      recurrenceEndYear: formData.isRecurring ? Number(formData.recurrenceEndYear) : undefined,
    } as Goal;
    if (editingItem?.id) {
      updateMutation.mutate({ id: editingItem.id, data });
    } else {
      createMutation.mutate(data);
    }
  };

  const handleDelete = (id: string) => {
    if (confirm('Are you sure you want to delete this goal?')) {
      deleteMutation.mutate(id);
    }
  };

  // Calculate totals
  const totalTarget = goals.reduce((sum, g) => sum + (g.targetAmount || 0), 0);
  const totalSaved = goals.reduce((sum, g) => sum + (g.currentSavings || 0), 0);
  const totalGap = totalTarget - totalSaved;
  const overallProgress = totalTarget > 0 ? (totalSaved / totalTarget) * 100 : 0;

  const getProgressColor = (progress: number) => {
    if (progress >= 75) return 'bg-success-500';
    if (progress >= 50) return 'bg-warning-500';
    return 'bg-danger-500';
  };

  return (
    <MainLayout
      title="Goals"
      subtitle="Track your financial goals"
      actions={
        <Button onClick={openAddModal}>
          <Plus size={18} className="mr-2" /> Add Goal
        </Button>
      }
    >
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <Card className="bg-primary-50 border-primary-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <Target className="text-primary-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-primary-600">Total Target</p>
              <p className="text-2xl font-bold text-primary-700">{formatCurrency(totalTarget, true)}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-success-50 border-success-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <TrendingUp className="text-success-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-success-600">Total Saved</p>
              <p className="text-2xl font-bold text-success-700">{formatCurrency(totalSaved, true)}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-danger-50 border-danger-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <AlertTriangle className="text-danger-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-danger-600">Gap Amount</p>
              <p className="text-2xl font-bold text-danger-700">{formatCurrency(totalGap, true)}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-warning-50 border-warning-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <CheckCircle className="text-warning-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-warning-600">Overall Progress</p>
              <p className="text-2xl font-bold text-warning-700">{overallProgress.toFixed(0)}%</p>
            </div>
          </div>
        </Card>
      </div>

      {/* Goals Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {isLoading ? (
          <div className="col-span-full text-center py-12 text-slate-400">Loading...</div>
        ) : goals.length === 0 ? (
          <div className="col-span-full text-center py-12 text-slate-400">
            No goals yet. Click "Add Goal" to get started.
          </div>
        ) : (
          goals.map((goal) => {
            const progress = goal.targetAmount > 0 ? ((goal.currentSavings || 0) / goal.targetAmount) * 100 : 0;
            const gap = goal.targetAmount - (goal.currentSavings || 0);
            const yearsLeft = goal.targetYear - new Date().getFullYear();

            return (
              <Card key={goal.id} className="overflow-hidden">
                <div className="p-5">
                  <div className="flex justify-between items-start mb-3">
                    <div>
                      <h3 className="font-semibold text-slate-800">{goal.name}</h3>
                      {goal.description && <p className="text-sm text-slate-500 mt-1">{goal.description}</p>}
                    </div>
                    <span className={`px-2 py-1 text-xs rounded-full ${
                      goal.priority === 'HIGH' ? 'bg-danger-100 text-danger-700' :
                      goal.priority === 'MEDIUM' ? 'bg-warning-100 text-warning-700' :
                      'bg-slate-100 text-slate-700'
                    }`}>
                      {goal.priority}
                    </span>
                  </div>

                  <div className="space-y-2 mb-4">
                    <div className="flex justify-between text-sm">
                      <span className="text-slate-500">Target</span>
                      <span className="font-medium text-slate-800">{formatCurrency(goal.targetAmount, true)}</span>
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-slate-500">Saved</span>
                      <span className="font-medium text-success-600">{formatCurrency(goal.currentSavings || 0, true)}</span>
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-slate-500">Gap</span>
                      <span className="font-medium text-danger-600">{formatCurrency(gap, true)}</span>
                    </div>
                  </div>

                  {/* Progress Bar */}
                  <div className="mb-4">
                    <div className="flex justify-between text-xs text-slate-500 mb-1">
                      <span>{progress.toFixed(0)}% complete</span>
                      <span>{yearsLeft > 0 ? `${yearsLeft} years left` : 'Due!'}</span>
                    </div>
                    <div className="h-2 bg-slate-200 rounded-full overflow-hidden">
                      <div 
                        className={`h-full ${getProgressColor(progress)} transition-all`}
                        style={{ width: `${Math.min(progress, 100)}%` }}
                      />
                    </div>
                  </div>

                  {/* Recurring Badge */}
                  {goal.isRecurring && (
                    <div className="mb-3 px-2 py-1 bg-primary-50 rounded text-xs text-primary-700">
                      🔄 Recurring every {goal.recurrenceInterval} year(s) until {goal.recurrenceEndYear}
                    </div>
                  )}

                  <div className="flex justify-between items-center pt-3 border-t border-slate-100">
                    <span className="text-xs text-slate-500">Target: {goal.targetYear}</span>
                    <div className="flex gap-2">
                      <button onClick={() => openEditModal(goal)} className="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg">
                        <Pencil size={16} />
                      </button>
                      <button onClick={() => handleDelete(goal.id!)} className="p-2 text-slate-400 hover:text-danger-600 hover:bg-danger-50 rounded-lg">
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </div>
                </div>
              </Card>
            );
          })
        )}
      </div>

      {/* Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={closeModal}
        title={editingItem ? 'Edit Goal' : 'Add Goal'}
        footer={
          <>
            <Button variant="secondary" onClick={closeModal}>Cancel</Button>
            <Button onClick={handleSubmit} isLoading={createMutation.isPending || updateMutation.isPending}>
              {editingItem ? 'Save Changes' : 'Add Goal'}
            </Button>
          </>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Goal Name"
            value={formData.name || ''}
            onChange={e => {
              setFormData({ ...formData, name: e.target.value });
              if (formErrors.name) setFormErrors(prev => ({ ...prev, name: '' }));
            }}
            placeholder="e.g., Children's Education"
            error={formErrors.name}
            required
          />
          <Input
            label="Description (optional)"
            value={formData.description || ''}
            onChange={e => setFormData({ ...formData, description: e.target.value })}
            placeholder="e.g., College fees for both children"
          />
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Target Amount"
              type="number"
              value={formData.targetAmount || ''}
              onChange={e => {
                setFormData({ ...formData, targetAmount: Number(e.target.value) });
                if (formErrors.targetAmount) setFormErrors(prev => ({ ...prev, targetAmount: '' }));
              }}
              placeholder="2500000"
              helperText={amountInWordsHelper(formData.targetAmount)}
              error={formErrors.targetAmount}
              required
            />
            <Input
              label="Current Savings"
              type="number"
              value={formData.currentSavings ?? ''}
              onChange={e => {
                setFormData({ ...formData, currentSavings: Number(e.target.value) });
                if (formErrors.currentSavings) setFormErrors(prev => ({ ...prev, currentSavings: '' }));
              }}
              placeholder="500000"
              helperText={amountInWordsHelper(formData.currentSavings)}
              error={formErrors.currentSavings}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Target Year"
              type="number"
              value={formData.targetYear || ''}
              onChange={e => {
                setFormData({ ...formData, targetYear: Number(e.target.value) });
                if (formErrors.targetYear) setFormErrors(prev => ({ ...prev, targetYear: '' }));
              }}
              placeholder="2030"
              error={formErrors.targetYear}
              required
            />
            <Select
              label="Priority"
              value={formData.priority || ''}
              onChange={e => {
                setFormData({ ...formData, priority: e.target.value as 'HIGH' | 'MEDIUM' | 'LOW' });
                if (formErrors.priority) setFormErrors(prev => ({ ...prev, priority: '' }));
              }}
              options={PRIORITY_OPTIONS}
              error={formErrors.priority}
              required
            />
          </div>

          {/* Recurring Goal Options */}
          <div className="p-4 bg-slate-50 rounded-lg space-y-3">
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="isRecurring"
                checked={formData.isRecurring || false}
                onChange={e => setFormData({ ...formData, isRecurring: e.target.checked })}
                className="w-4 h-4 text-primary-600 rounded"
              />
              <label htmlFor="isRecurring" className="text-sm font-medium text-slate-700">This is a recurring goal</label>
            </div>

            {formData.isRecurring && (
              <div className="grid grid-cols-2 gap-4 pt-2">
                <Input
                  label="Repeat Every (years)"
                  type="number"
                  value={formData.recurrenceInterval || ''}
                  onChange={e => {
                    setFormData({ ...formData, recurrenceInterval: Number(e.target.value) });
                    if (formErrors.recurrenceInterval) setFormErrors(prev => ({ ...prev, recurrenceInterval: '' }));
                  }}
                  placeholder="1"
                  min={1}
                  error={formErrors.recurrenceInterval}
                />
                <Input
                  label="Until Year"
                  type="number"
                  value={formData.recurrenceEndYear || ''}
                  onChange={e => {
                    setFormData({ ...formData, recurrenceEndYear: Number(e.target.value) });
                    if (formErrors.recurrenceEndYear) setFormErrors(prev => ({ ...prev, recurrenceEndYear: '' }));
                  }}
                  placeholder="2035"
                  error={formErrors.recurrenceEndYear}
                />
              </div>
            )}
          </div>
        </form>
      </Modal>
    </MainLayout>
  );
}

export default Goals;
