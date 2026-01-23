import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MainLayout } from '../components/Layout';
import { Card, Button, Modal, Input, Select, toast } from '../components/ui';
import { familyApi, FamilyMember } from '../lib/api';
import { formatCurrency } from '../lib/utils';
import { Plus, Pencil, Trash2, Users, Heart, Calendar } from 'lucide-react';

const RELATIONSHIP_OPTIONS = [
  { value: 'SELF', label: 'Self' },
  { value: 'SPOUSE', label: 'Spouse' },
  { value: 'CHILD', label: 'Child' },
  { value: 'PARENT', label: 'Parent' },
  { value: 'SIBLING', label: 'Sibling' },
  { value: 'OTHER', label: 'Other' },
];

const GENDER_OPTIONS = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
];

export function Family() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<FamilyMember | null>(null);
  const [formData, setFormData] = useState<Partial<FamilyMember>>({});

  const { data: members = [], isLoading } = useQuery({
    queryKey: ['family'],
    queryFn: familyApi.getAll,
  });

  const createMutation = useMutation({
    mutationFn: (data: FamilyMember) => familyApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['family'] });
      toast.success('Family member added successfully');
      closeModal();
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: FamilyMember }) => familyApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['family'] });
      toast.success('Family member updated successfully');
      closeModal();
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => familyApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['family'] });
      toast.success('Family member removed successfully');
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const openAddModal = () => {
    setEditingItem(null);
    setFormData({ isDependent: true });
    setIsModalOpen(true);
  };

  const openEditModal = (item: FamilyMember) => {
    setEditingItem(item);
    setFormData(item);
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setEditingItem(null);
    setFormData({});
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const data = {
      ...formData,
      existingHealthCover: Number(formData.existingHealthCover) || 0,
      existingLifeCover: Number(formData.existingLifeCover) || 0,
    } as FamilyMember;
    if (editingItem?.id) {
      updateMutation.mutate({ id: editingItem.id, data });
    } else {
      createMutation.mutate(data);
    }
  };

  const handleDelete = (id: string) => {
    if (confirm('Are you sure you want to remove this family member?')) {
      deleteMutation.mutate(id);
    }
  };

  const calculateAge = (dob: string) => {
    const birth = new Date(dob);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const m = today.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
    return age;
  };

  // Group by relationship
  const dependents = members.filter(m => m.isDependent);
  const earningMembers = members.filter(m => m.isEarning);
  const totalHealthCover = members.reduce((sum, m) => sum + (m.existingHealthCover || 0), 0);

  return (
    <MainLayout
      title="Family Members"
      subtitle="Manage family information for insurance planning"
      actions={
        <Button onClick={openAddModal}>
          <Plus size={18} className="mr-2" /> Add Member
        </Button>
      }
    >
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card className="bg-primary-50 border-primary-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <Users className="text-primary-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-primary-600">Total Members</p>
              <p className="text-2xl font-bold text-primary-700">{members.length}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-warning-50 border-warning-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <Calendar className="text-warning-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-warning-600">Dependents</p>
              <p className="text-2xl font-bold text-warning-700">{dependents.length}</p>
            </div>
          </div>
        </Card>
        <Card className="bg-success-50 border-success-200">
          <div className="p-4 flex items-center gap-4">
            <div className="p-3 rounded-xl bg-white shadow-sm">
              <Heart className="text-success-500" size={24} />
            </div>
            <div>
              <p className="text-sm text-success-600">Health Cover</p>
              <p className="text-2xl font-bold text-success-700">{formatCurrency(totalHealthCover)}</p>
            </div>
          </div>
        </Card>
      </div>

      {/* Family Members Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {isLoading ? (
          <div className="col-span-full text-center py-12 text-slate-400">Loading...</div>
        ) : members.length === 0 ? (
          <div className="col-span-full text-center py-12 text-slate-400">
            No family members yet. Click "Add Member" to get started.
          </div>
        ) : (
          members.map((member) => {
            const age = member.dateOfBirth ? calculateAge(member.dateOfBirth) : null;

            return (
              <Card key={member.id} className="overflow-hidden">
                <div className="p-5">
                  <div className="flex justify-between items-start mb-4">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-full bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white font-bold text-lg">
                        {member.name.charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <h3 className="font-semibold text-slate-800">{member.name}</h3>
                        <p className="text-sm text-slate-500">
                          {RELATIONSHIP_OPTIONS.find(r => r.value === member.relationship)?.label || member.relationship}
                          {age !== null && ` • ${age} years`}
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-2 mb-4">
                    <div className="flex flex-wrap gap-2">
                      {member.isEarning && (
                        <span className="px-2 py-1 text-xs rounded-full bg-success-100 text-success-700">Earning</span>
                      )}
                      {member.isDependent && (
                        <span className="px-2 py-1 text-xs rounded-full bg-warning-100 text-warning-700">Dependent</span>
                      )}
                      {member.hasPreExistingConditions && (
                        <span className="px-2 py-1 text-xs rounded-full bg-danger-100 text-danger-700">Pre-existing Conditions</span>
                      )}
                    </div>

                    {(member.existingHealthCover || 0) > 0 && (
                      <div className="flex justify-between text-sm">
                        <span className="text-slate-500">Health Cover</span>
                        <span className="font-medium text-success-600">{formatCurrency(member.existingHealthCover || 0)}</span>
                      </div>
                    )}
                    {(member.existingLifeCover || 0) > 0 && (
                      <div className="flex justify-between text-sm">
                        <span className="text-slate-500">Life Cover</span>
                        <span className="font-medium text-primary-600">{formatCurrency(member.existingLifeCover || 0)}</span>
                      </div>
                    )}
                  </div>

                  <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                    <button onClick={() => openEditModal(member)} className="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg">
                      <Pencil size={16} />
                    </button>
                    <button onClick={() => handleDelete(member.id!)} className="p-2 text-slate-400 hover:text-danger-600 hover:bg-danger-50 rounded-lg">
                      <Trash2 size={16} />
                    </button>
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
        title={editingItem ? 'Edit Family Member' : 'Add Family Member'}
        footer={
          <>
            <Button variant="secondary" onClick={closeModal}>Cancel</Button>
            <Button onClick={handleSubmit} isLoading={createMutation.isPending || updateMutation.isPending}>
              {editingItem ? 'Save Changes' : 'Add Member'}
            </Button>
          </>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Name"
            value={formData.name || ''}
            onChange={e => setFormData({ ...formData, name: e.target.value })}
            placeholder="e.g., John Doe"
            required
          />
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Relationship"
              value={formData.relationship || ''}
              onChange={e => setFormData({ ...formData, relationship: e.target.value })}
              options={RELATIONSHIP_OPTIONS}
              required
            />
            <Select
              label="Gender"
              value={formData.gender || ''}
              onChange={e => setFormData({ ...formData, gender: e.target.value })}
              options={GENDER_OPTIONS}
            />
          </div>
          <Input
            label="Date of Birth"
            type="date"
            value={formData.dateOfBirth?.split('T')[0] || ''}
            onChange={e => setFormData({ ...formData, dateOfBirth: e.target.value })}
            required
          />

          <div className="space-y-3 p-4 bg-slate-50 rounded-lg">
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="isEarning"
                checked={formData.isEarning || false}
                onChange={e => setFormData({ ...formData, isEarning: e.target.checked })}
                className="w-4 h-4 text-primary-600 rounded"
              />
              <label htmlFor="isEarning" className="text-sm text-slate-700">Is earning member</label>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="isDependent"
                checked={formData.isDependent || false}
                onChange={e => setFormData({ ...formData, isDependent: e.target.checked })}
                className="w-4 h-4 text-primary-600 rounded"
              />
              <label htmlFor="isDependent" className="text-sm text-slate-700">Is dependent</label>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="hasPreExistingConditions"
                checked={formData.hasPreExistingConditions || false}
                onChange={e => setFormData({ ...formData, hasPreExistingConditions: e.target.checked })}
                className="w-4 h-4 text-primary-600 rounded"
              />
              <label htmlFor="hasPreExistingConditions" className="text-sm text-slate-700">Has pre-existing health conditions</label>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Existing Health Cover"
              type="number"
              value={formData.existingHealthCover || ''}
              onChange={e => setFormData({ ...formData, existingHealthCover: Number(e.target.value) })}
              placeholder="500000"
            />
            <Input
              label="Existing Life Cover"
              type="number"
              value={formData.existingLifeCover || ''}
              onChange={e => setFormData({ ...formData, existingLifeCover: Number(e.target.value) })}
              placeholder="1000000"
            />
          </div>
        </form>
      </Modal>
    </MainLayout>
  );
}

export default Family;
