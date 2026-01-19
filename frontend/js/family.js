// Retyrment - Family Members Page
// Manages family member data for insurance recommendations and financial planning

const RELATIONSHIPS = {
    'SELF': { label: '👤 Self', description: 'You (the primary user)' },
    'SPOUSE': { label: '💑 Spouse', description: 'Husband/Wife' },
    'CHILD': { label: '👶 Child', description: 'Son/Daughter' },
    'PARENT': { label: '👨‍👩‍👦 Parent', description: 'Father/Mother' },
    'PARENT_IN_LAW': { label: '👴 Parent-in-Law', description: 'Father/Mother-in-law' },
    'SIBLING': { label: '👫 Sibling', description: 'Brother/Sister' },
    'OTHER': { label: '👥 Other', description: 'Other dependent' }
};

const GENDERS = {
    'MALE': 'Male',
    'FEMALE': 'Female',
    'OTHER': 'Other'
};

const EDUCATION_LEVELS = {
    'PRE_SCHOOL': 'Pre-School',
    'PRIMARY': 'Primary (Class 1-5)',
    'MIDDLE': 'Middle (Class 6-8)',
    'SECONDARY': 'Secondary (Class 9-10)',
    'HIGHER_SECONDARY': 'Higher Secondary (Class 11-12)',
    'UNDERGRADUATE': 'Undergraduate',
    'POSTGRADUATE': 'Postgraduate',
    'WORKING': 'Working',
    'NOT_APPLICABLE': 'N/A'
};

// Note: Don't redeclare currentEditId - it's in common.js
// But we need our own for this page's custom modal
let familyCurrentEditId = null;

document.addEventListener('DOMContentLoaded', () => {
    loadData();
});

async function loadData() {
    try {
        const [members, summary] = await Promise.all([
            api.get('/family'),
            api.get('/family/summary')
        ]);
        renderTable(members);
        renderSummary(summary);
        document.getElementById('entry-count').textContent = `${members.length} members`;
    } catch (error) {
        console.error('Error loading family data:', error);
        document.getElementById('data-table').innerHTML = `
            <tr><td colspan="6" class="px-6 py-8 text-center text-danger-400">
                Error loading data: ${error.message}
            </td></tr>`;
    }
}

function renderSummary(summary) {
    document.getElementById('total-members').textContent = summary.totalMembers || 0;
    document.getElementById('total-dependents').textContent = summary.dependents || 0;
    document.getElementById('total-health-cover').textContent = formatCurrency(summary.totalHealthCover || 0);
    document.getElementById('total-life-cover').textContent = formatCurrency(summary.totalLifeCover || 0);
}

function renderTable(members) {
    const tbody = document.getElementById('data-table');
    if (!members || members.length === 0) {
        tbody.innerHTML = `
            <tr><td colspan="6" class="px-6 py-8 text-center text-slate-500">
                No family members added yet. Click "Add Member" to start.
            </td></tr>`;
        return;
    }

    tbody.innerHTML = members.map(member => {
        const rel = RELATIONSHIPS[member.relationship] || RELATIONSHIPS.OTHER;
        const age = member.dateOfBirth ? calculateAge(member.dateOfBirth) : '-';
        const badges = [];
        
        if (member.isDependent) badges.push('<span class="badge badge-warning">Dependent</span>');
        if (member.isEarning) badges.push('<span class="badge badge-success">Earning</span>');
        if (member.hasPreExistingConditions) badges.push('<span class="badge badge-danger">PEC</span>');
        
        return `
        <tr class="hover:bg-slate-50 border-b border-slate-100">
            <td class="px-6 py-4">
                <div class="flex items-center gap-3">
                    <span class="text-2xl">${rel.label.split(' ')[0]}</span>
                    <div>
                        <div class="font-medium text-slate-800">${member.name || 'Unnamed'}</div>
                        <div class="text-xs text-slate-500">${rel.label.substring(3)}</div>
                    </div>
                </div>
            </td>
            <td class="px-6 py-4">${age}</td>
            <td class="px-6 py-4">${GENDERS[member.gender] || '-'}</td>
            <td class="px-6 py-4">
                <div class="flex flex-wrap gap-1">${badges.join('') || '-'}</div>
            </td>
            <td class="px-6 py-4 text-xs">
                ${member.existingHealthCover ? `Health: ${formatCurrency(member.existingHealthCover)}` : ''}
                ${member.existingLifeCover ? `<br>Life: ${formatCurrency(member.existingLifeCover)}` : ''}
                ${!member.existingHealthCover && !member.existingLifeCover ? '-' : ''}
            </td>
            <td class="px-6 py-4 text-right">
                <button onclick="editMember('${member.id}')" class="text-primary-600 hover:text-primary-800 mr-3">Edit</button>
                <button onclick="deleteMember('${member.id}')" class="text-slate-400 hover:text-danger-500">Delete</button>
            </td>
        </tr>`;
    }).join('');
}

function calculateAge(dateOfBirth) {
    if (!dateOfBirth) return null;
    const today = new Date();
    const birth = new Date(dateOfBirth);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
        age--;
    }
    return age;
}

function openAddMemberModal() {
    familyCurrentEditId = null;
    showMemberForm();
}

async function editMember(id) {
    try {
        const member = await api.get(`/family/${id}`);
        familyCurrentEditId = id;
        showMemberForm(member);
    } catch (error) {
        showToast('Error loading member', 'error');
    }
}

async function deleteMember(id) {
    if (!confirm('Are you sure you want to delete this family member?')) return;
    try {
        await api.delete(`/family/${id}`);
        showToast('Member deleted');
        loadData();
    } catch (error) {
        showToast('Error: ' + error.message, 'error');
    }
}

function showMemberForm(data = {}) {
    const isChild = data.relationship === 'CHILD';
    const isParent = data.relationship === 'PARENT' || data.relationship === 'PARENT_IN_LAW';
    
    const modalHtml = `
    <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" id="modal-backdrop" onclick="closeFamilyModal()">
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-2xl mx-4 max-h-[90vh] overflow-hidden" onclick="event.stopPropagation()">
            <div class="p-6 border-b border-slate-200 bg-gradient-to-r from-primary-50 to-white">
                <h2 class="text-xl font-semibold text-slate-800">${familyCurrentEditId ? 'Edit' : 'Add'} Family Member</h2>
                <p class="text-sm text-slate-500">Enter details for insurance and financial planning</p>
            </div>
            
            <form id="family-member-form" onsubmit="handleSubmit(event)" class="p-6 space-y-4 max-h-[60vh] overflow-y-auto">
                <!-- Basic Info -->
                <div class="grid grid-cols-2 gap-4">
                    <div class="col-span-2 sm:col-span-1">
                        <label class="block text-sm font-medium text-slate-700 mb-1">Name *</label>
                        <input type="text" name="name" value="${data.name || ''}" required
                               placeholder="Full name"
                               class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500">
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-slate-700 mb-1">Relationship *</label>
                        <select name="relationship" required onchange="updateFormForRelationship(this.value)"
                                class="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white">
                            ${Object.entries(RELATIONSHIPS).map(([key, val]) => 
                                `<option value="${key}" ${data.relationship === key ? 'selected' : ''}>${val.label}</option>`
                            ).join('')}
                        </select>
                    </div>
                </div>
                
                <div class="grid grid-cols-2 gap-4">
                    <div>
                        <label class="block text-sm font-medium text-slate-700 mb-1">Date of Birth</label>
                        <input type="date" name="dateOfBirth" value="${data.dateOfBirth || ''}"
                               class="w-full px-3 py-2 border border-slate-300 rounded-lg">
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-slate-700 mb-1">Gender</label>
                        <select name="gender" class="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white">
                            ${Object.entries(GENDERS).map(([key, val]) => 
                                `<option value="${key}" ${data.gender === key ? 'selected' : ''}>${val}</option>`
                            ).join('')}
                        </select>
                    </div>
                </div>
                
                <!-- Financial Status -->
                <div class="p-4 bg-blue-50 rounded-lg border border-blue-200">
                    <p class="text-sm font-medium text-blue-800 mb-3">💰 Financial Status</p>
                    <div class="grid grid-cols-2 gap-4">
                        <label class="flex items-center gap-2 cursor-pointer">
                            <input type="checkbox" name="isDependent" ${data.isDependent ? 'checked' : ''}
                                   class="w-4 h-4 text-blue-600 rounded">
                            <span class="text-sm text-slate-700">Financially Dependent</span>
                        </label>
                        <label class="flex items-center gap-2 cursor-pointer">
                            <input type="checkbox" name="isEarning" ${data.isEarning ? 'checked' : ''}
                                   onchange="toggleIncomeField(this.checked)"
                                   class="w-4 h-4 text-blue-600 rounded">
                            <span class="text-sm text-slate-700">Currently Earning</span>
                        </label>
                    </div>
                    <div id="income-fields" class="${data.isEarning ? '' : 'hidden'} mt-3">
                        <label class="block text-sm font-medium text-slate-700 mb-1">Monthly Income (₹)</label>
                        <input type="number" name="monthlyIncome" value="${data.monthlyIncome || ''}"
                               placeholder="e.g., 50000"
                               class="w-full px-3 py-2 border border-slate-300 rounded-lg">
                    </div>
                </div>
                
                <!-- Health Information -->
                <div class="p-4 bg-red-50 rounded-lg border border-red-200">
                    <p class="text-sm font-medium text-red-800 mb-3">🏥 Health Information (for insurance calculation)</p>
                    <div class="grid grid-cols-2 gap-4 mb-3">
                        <label class="flex items-center gap-2 cursor-pointer">
                            <input type="checkbox" name="hasPreExistingConditions" 
                                   ${data.hasPreExistingConditions ? 'checked' : ''}
                                   onchange="togglePECField(this.checked)"
                                   class="w-4 h-4 text-red-600 rounded">
                            <span class="text-sm text-slate-700">Pre-existing Conditions</span>
                        </label>
                        <label class="flex items-center gap-2 cursor-pointer">
                            <input type="checkbox" name="isSmoker" ${data.isSmoker ? 'checked' : ''}
                                   class="w-4 h-4 text-red-600 rounded">
                            <span class="text-sm text-slate-700">Smoker</span>
                        </label>
                    </div>
                    <div id="pec-fields" class="${data.hasPreExistingConditions ? '' : 'hidden'}">
                        <label class="block text-sm font-medium text-slate-700 mb-1">Conditions (comma-separated)</label>
                        <input type="text" name="preExistingConditions" value="${data.preExistingConditions || ''}"
                               placeholder="e.g., Diabetes, Hypertension"
                               class="w-full px-3 py-2 border border-slate-300 rounded-lg">
                    </div>
                </div>
                
                <!-- Existing Insurance Coverage -->
                <div class="p-4 bg-green-50 rounded-lg border border-green-200">
                    <p class="text-sm font-medium text-green-800 mb-3">🛡️ Existing Insurance Coverage</p>
                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="block text-sm font-medium text-slate-700 mb-1">Health Cover (₹)</label>
                            <input type="number" name="existingHealthCover" value="${data.existingHealthCover || ''}"
                                   placeholder="Sum insured"
                                   class="w-full px-3 py-2 border border-slate-300 rounded-lg">
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-slate-700 mb-1">Life Cover (₹)</label>
                            <input type="number" name="existingLifeCover" value="${data.existingLifeCover || ''}"
                                   placeholder="Sum assured"
                                   class="w-full px-3 py-2 border border-slate-300 rounded-lg">
                        </div>
                    </div>
                </div>
                
                <!-- Child-specific fields -->
                <div id="child-fields" class="${isChild ? '' : 'hidden'} p-4 bg-purple-50 rounded-lg border border-purple-200">
                    <p class="text-sm font-medium text-purple-800 mb-3">📚 Education (for children)</p>
                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="block text-sm font-medium text-slate-700 mb-1">Current Education Level</label>
                            <select name="currentEducation" class="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white">
                                ${Object.entries(EDUCATION_LEVELS).map(([key, val]) => 
                                    `<option value="${key}" ${data.currentEducation === key ? 'selected' : ''}>${val}</option>`
                                ).join('')}
                            </select>
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-slate-700 mb-1">Dependency End Age</label>
                            <input type="number" name="dependencyEndAge" value="${data.dependencyEndAge || 25}" min="18" max="30"
                                   class="w-full px-3 py-2 border border-slate-300 rounded-lg">
                        </div>
                    </div>
                </div>
                
                <!-- Parent-specific fields -->
                <div id="parent-fields" class="${isParent ? '' : 'hidden'} p-4 bg-amber-50 rounded-lg border border-amber-200">
                    <p class="text-sm font-medium text-amber-800 mb-3">👴 Senior Information (for parents)</p>
                    <div class="grid grid-cols-2 gap-4">
                        <label class="flex items-center gap-2 cursor-pointer">
                            <input type="checkbox" name="livesWithUser" ${data.livesWithUser ? 'checked' : ''}
                                   class="w-4 h-4 text-amber-600 rounded">
                            <span class="text-sm text-slate-700">Lives with you</span>
                        </label>
                        <label class="flex items-center gap-2 cursor-pointer">
                            <input type="checkbox" name="hasSeparateHealthPolicy" ${data.hasSeparateHealthPolicy ? 'checked' : ''}
                                   class="w-4 h-4 text-amber-600 rounded">
                            <span class="text-sm text-slate-700">Has separate health policy</span>
                        </label>
                    </div>
                </div>
            </form>
            
            <div class="p-4 border-t border-slate-200 bg-slate-50 flex justify-between">
                <button type="button" onclick="closeFamilyModal()" class="px-4 py-2 text-slate-600 hover:text-slate-800">Cancel</button>
                <button type="submit" form="family-member-form" class="px-6 py-2 bg-primary-600 hover:bg-primary-700 text-white rounded-lg font-medium">
                    ${familyCurrentEditId ? 'Save Changes' : 'Add Member'}
                </button>
            </div>
        </div>
    </div>`;
    
    closeFamilyModal();
    document.body.insertAdjacentHTML('beforeend', modalHtml);
}

function updateFormForRelationship(relationship) {
    const childFields = document.getElementById('child-fields');
    const parentFields = document.getElementById('parent-fields');
    
    childFields.classList.toggle('hidden', relationship !== 'CHILD');
    parentFields.classList.toggle('hidden', relationship !== 'PARENT' && relationship !== 'PARENT_IN_LAW');
    
    // Auto-set dependent status
    const isDependentCheckbox = document.querySelector('[name="isDependent"]');
    if (relationship === 'CHILD' || relationship === 'PARENT' || relationship === 'PARENT_IN_LAW') {
        isDependentCheckbox.checked = true;
    }
}

function toggleIncomeField(show) {
    document.getElementById('income-fields').classList.toggle('hidden', !show);
}

function togglePECField(show) {
    document.getElementById('pec-fields').classList.toggle('hidden', !show);
}

function closeFamilyModal() {
    const modal = document.getElementById('modal-backdrop');
    if (modal) modal.remove();
}

function submitFamilyForm() {
    const form = document.querySelector('#modal-backdrop form');
    if (form) {
        form.dispatchEvent(new Event('submit', { cancelable: true }));
    }
}

async function handleSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const data = {};
    
    formData.forEach((value, key) => {
        if (value === '' || value === null) return;
        
        // Handle checkboxes
        if (['isDependent', 'isEarning', 'hasPreExistingConditions', 'isSmoker', 
             'livesWithUser', 'hasSeparateHealthPolicy'].includes(key)) {
            data[key] = true;
            return;
        }
        
        // Handle numbers
        if (['monthlyIncome', 'existingHealthCover', 'existingLifeCover', 'dependencyEndAge'].includes(key)) {
            data[key] = parseFloat(value);
        } else {
            data[key] = value;
        }
    });
    
    // Handle unchecked checkboxes
    ['isDependent', 'isEarning', 'hasPreExistingConditions', 'isSmoker', 
     'livesWithUser', 'hasSeparateHealthPolicy'].forEach(key => {
        if (!data[key]) data[key] = false;
    });
    
    try {
        if (familyCurrentEditId) {
            await api.put(`/family/${familyCurrentEditId}`, data);
            showToast('Member updated!');
        } else {
            await api.post('/family', data);
            showToast('Member added!');
        }
        closeFamilyModal();
        loadData();
    } catch (error) {
        showToast('Error: ' + error.message, 'error');
    }
}

// Utility functions are provided by common.js (formatCurrency, showToast)
