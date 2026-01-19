// Retyrment - Goals Page
// Supports both one-time and recurring goals with customizable intervals

// Note: currentEditId is declared in common.js

document.addEventListener('DOMContentLoaded', () => { loadData(); });

async function loadData() {
    try {
        const data = await api.goals.getAll();
        renderTable(data);
        document.getElementById('entry-count').textContent = `${data.length} entries`;
    } catch (error) {
        document.getElementById('data-table').innerHTML = `<tr><td colspan="7" class="px-6 py-8 text-center text-danger-400">Error loading data</td></tr>`;
    }
}

function renderTable(data) {
    const tbody = document.getElementById('data-table');
    if (data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="px-6 py-8 text-center text-gray-500">No goals yet.</td></tr>`;
        return;
    }

    const priorityColors = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' };

    tbody.innerHTML = data.map(item => {
        // Format recurring info
        let recurringInfo = 'No';
        if (item.isRecurring) {
            const interval = item.recurrenceInterval || 1;
            const endYear = item.recurrenceEndYear;
            if (interval === 1) {
                recurringInfo = `Every year${endYear ? ` till ${endYear}` : ''}`;
            } else {
                recurringInfo = `Every ${interval} years${endYear ? ` till ${endYear}` : ''}`;
            }
        }
        
        return `
        <tr class="hover:bg-dark-600">
            <td class="px-6 py-4">
                <div class="flex items-center gap-2">
                    <span class="text-2xl">${item.icon || '🎯'}</span>
                    <div>
                        <span class="font-medium">${item.name || 'Goal'}</span>
                        ${item.description ? `<p class="text-xs text-slate-500">${item.description}</p>` : ''}
                    </div>
                </div>
            </td>
            <td class="px-6 py-4 font-mono text-accent-400">${formatCurrency(item.targetAmount)}</td>
            <td class="px-6 py-4">${item.targetYear || '-'}</td>
            <td class="px-6 py-4">
                <span class="badge badge-${priorityColors[item.priority] || 'info'}">${item.priority || 'MEDIUM'}</span>
            </td>
            <td class="px-6 py-4">
                ${item.isRecurring 
                    ? `<span class="text-success-400 text-xs">${recurringInfo}</span>` 
                    : '<span class="text-slate-500">One-time</span>'}
            </td>
            <td class="px-6 py-4 text-right">
                <button onclick="editItem('${item.id}')" class="text-gray-400 hover:text-accent-400 mr-3">Edit</button>
                <button onclick="deleteItem('${item.id}')" class="text-gray-400 hover:text-danger-400">Delete</button>
            </td>
        </tr>`;
    }).join('');
}

function openAddModal() {
    currentEditId = null;
    showGoalForm();
}

async function editItem(id) {
    try {
        const data = await api.get(`/goals/${id}`);
        currentEditId = id;
        showGoalForm(data);
    } catch (error) {
        showToast('Error loading goal', 'error');
    }
}

async function deleteItem(id) {
    await confirmDelete('/goals', id, loadData);
}

function showGoalForm(data = {}) {
    const currentYear = new Date().getFullYear();
    const isRecurring = data.isRecurring || false;
    
    const modalHtml = `
    <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" id="modal-backdrop" onclick="closeModal()">
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 max-h-[90vh] overflow-hidden" onclick="event.stopPropagation()">
            <div class="p-6 border-b border-slate-200 bg-gradient-to-r from-primary-50 to-white">
                <h2 class="text-xl font-semibold text-slate-800">${currentEditId ? 'Edit' : 'Add'} Financial Goal</h2>
                <p class="text-sm text-slate-500">Define your financial targets</p>
            </div>
            
            <form onsubmit="handleSubmit(event)" class="p-6 space-y-4 max-h-[60vh] overflow-y-auto">
                <!-- Basic Info -->
                <div class="grid grid-cols-4 gap-4">
                    <div class="col-span-1">
                        <label class="block text-sm font-medium text-slate-700 mb-1">Icon</label>
                        <input type="text" name="icon" value="${data.icon || '🎯'}" maxlength="2"
                               class="w-full px-3 py-2 border border-slate-300 rounded-lg text-center text-2xl">
                    </div>
                    <div class="col-span-3">
                        <label class="block text-sm font-medium text-slate-700 mb-1">Goal Name *</label>
                        <input type="text" name="name" value="${data.name || ''}" required
                               placeholder="e.g., Child's Education, New Car"
                               class="w-full px-3 py-2 border border-slate-300 rounded-lg">
                    </div>
                </div>
                
                <div>
                    <label class="block text-sm font-medium text-slate-700 mb-1">Description</label>
                    <input type="text" name="description" value="${data.description || ''}"
                           placeholder="Optional details about this goal"
                           class="w-full px-3 py-2 border border-slate-300 rounded-lg">
                </div>
                
                <div class="grid grid-cols-2 gap-4">
                    <div>
                        <label class="block text-sm font-medium text-slate-700 mb-1">Target Amount (₹) *</label>
                        <input type="number" name="targetAmount" value="${data.targetAmount || ''}" required min="0"
                               placeholder="e.g., 500000"
                               class="w-full px-3 py-2 border border-slate-300 rounded-lg">
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-slate-700 mb-1">Target Year *</label>
                        <input type="number" name="targetYear" value="${data.targetYear || currentYear + 5}" required 
                               min="${currentYear}" max="2100"
                               class="w-full px-3 py-2 border border-slate-300 rounded-lg">
                    </div>
                </div>
                
                <div>
                    <label class="block text-sm font-medium text-slate-700 mb-1">Priority</label>
                    <select name="priority" class="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white">
                        <option value="HIGH" ${data.priority === 'HIGH' ? 'selected' : ''}>🔴 High - Must achieve</option>
                        <option value="MEDIUM" ${data.priority === 'MEDIUM' || !data.priority ? 'selected' : ''}>🟡 Medium - Important</option>
                        <option value="LOW" ${data.priority === 'LOW' ? 'selected' : ''}>🟢 Low - Nice to have</option>
                    </select>
                </div>
                
                <!-- Recurring Goal Section -->
                <div class="p-4 bg-blue-50 rounded-lg border border-blue-200">
                    <label class="flex items-center gap-2 cursor-pointer mb-3">
                        <input type="checkbox" name="isRecurring" ${isRecurring ? 'checked' : ''}
                               onchange="toggleRecurringFields(this.checked)"
                               class="w-4 h-4 text-blue-600 rounded">
                        <span class="font-medium text-blue-800">Recurring Goal</span>
                    </label>
                    <p class="text-xs text-blue-600 mb-3">
                        Check this if the goal repeats (e.g., annual vacation, car replacement every 5 years)
                    </p>
                    
                    <div id="recurring-fields" class="${isRecurring ? '' : 'hidden'} space-y-3">
                        <div class="grid grid-cols-2 gap-4">
                            <div>
                                <label class="block text-sm font-medium text-blue-800 mb-1">Repeat Every</label>
                                <div class="flex items-center gap-2">
                                    <input type="number" name="recurrenceInterval" 
                                           value="${data.recurrenceInterval || 1}" min="1" max="20"
                                           class="w-20 px-3 py-2 border border-blue-300 rounded-lg bg-white">
                                    <span class="text-sm text-blue-700">year(s)</span>
                                </div>
                            </div>
                            <div>
                                <label class="block text-sm font-medium text-blue-800 mb-1">Until Year</label>
                                <input type="number" name="recurrenceEndYear" 
                                       value="${data.recurrenceEndYear || ''}"
                                       min="${currentYear}" max="2100"
                                       placeholder="Leave empty for retirement"
                                       class="w-full px-3 py-2 border border-blue-300 rounded-lg bg-white">
                            </div>
                        </div>
                        
                        <div class="flex items-center gap-4">
                            <label class="flex items-center gap-2 cursor-pointer">
                                <input type="checkbox" name="adjustForInflation" 
                                       ${data.adjustForInflation !== false ? 'checked' : ''}
                                       class="w-4 h-4 text-blue-600 rounded">
                                <span class="text-sm text-blue-700">Adjust for inflation</span>
                            </label>
                            <div class="flex items-center gap-1">
                                <input type="number" name="customInflationRate" 
                                       value="${data.customInflationRate || ''}"
                                       step="0.5" min="0" max="20"
                                       placeholder="6"
                                       class="w-16 px-2 py-1 border border-blue-300 rounded text-sm bg-white">
                                <span class="text-xs text-blue-600">% (leave empty for default)</span>
                            </div>
                        </div>
                        
                        <div class="text-xs text-blue-600 bg-blue-100 p-2 rounded">
                            💡 Example: A ₹2L vacation goal recurring every year with 6% inflation becomes ₹2.12L next year, ₹2.25L after that, etc.
                        </div>
                    </div>
                </div>
            </form>
            
            <div class="p-4 border-t border-slate-200 bg-slate-50 flex justify-between">
                <button onclick="closeModal()" class="px-4 py-2 text-slate-600 hover:text-slate-800">Cancel</button>
                <button onclick="submitForm()" class="px-6 py-2 bg-primary-600 hover:bg-primary-700 text-white rounded-lg font-medium">
                    ${currentEditId ? 'Save Changes' : 'Add Goal'}
                </button>
            </div>
        </div>
    </div>`;
    
    closeModal();
    document.body.insertAdjacentHTML('beforeend', modalHtml);
}

function toggleRecurringFields(show) {
    document.getElementById('recurring-fields').classList.toggle('hidden', !show);
}

function closeModal() {
    const modal = document.getElementById('modal-backdrop');
    if (modal) modal.remove();
}

function submitForm() {
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
        if (key === 'isRecurring' || key === 'adjustForInflation') {
            data[key] = true;
            return;
        }
        
        // Handle numbers
        if (['targetAmount', 'targetYear', 'recurrenceInterval', 'recurrenceEndYear', 'customInflationRate'].includes(key)) {
            data[key] = parseFloat(value);
        } else {
            data[key] = value;
        }
    });
    
    // Handle unchecked checkboxes
    if (!data.isRecurring) data.isRecurring = false;
    if (data.isRecurring && !data.adjustForInflation) data.adjustForInflation = false;
    
    // If not recurring, clear recurring fields
    if (!data.isRecurring) {
        delete data.recurrenceInterval;
        delete data.recurrenceEndYear;
        delete data.adjustForInflation;
        delete data.customInflationRate;
    }
    
    try {
        if (currentEditId) {
            await api.goals.update(currentEditId, data);
            showToast('Goal updated!');
        } else {
            await api.goals.create(data);
            showToast('Goal added!');
        }
        closeModal();
        loadData();
    } catch (error) {
        showToast('Error: ' + error.message, 'error');
    }
}

// Utility function (should be in common.js)
function formatCurrency(amount) {
    if (!amount || isNaN(amount)) return '₹0';
    if (amount >= 10000000) return `₹${(amount / 10000000).toFixed(2)} Cr`;
    if (amount >= 100000) return `₹${(amount / 100000).toFixed(2)} L`;
    return `₹${Math.round(amount).toLocaleString('en-IN')}`;
}
