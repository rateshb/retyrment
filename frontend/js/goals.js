// Retyrment - Goals Page

const goalFields = [
    { name: 'icon', label: 'Icon (Emoji)', type: 'text', maxlength: 2, placeholder: '🎯', hint: 'Optional. Pick an emoji to represent this goal.' },
    { name: 'name', label: 'Goal Name', type: 'text', required: true, placeholder: 'e.g., New Car', hint: 'Required. Goal name. Cannot be empty.' },
    { name: 'targetAmount', label: 'Target Amount (₹)', type: 'number', required: true, min: 0, hint: 'Required. Amount needed to achieve this goal. Must be 0 or greater.' },
    { name: 'targetYear', label: 'Target Year', type: 'number', required: true, value: new Date().getFullYear() + 5, min: 2000, hint: 'Required. Year by which you want to achieve this. Must be 2000 or later.' },
    { name: 'priority', label: 'Priority', type: 'select', options: ['HIGH', 'MEDIUM', 'LOW'], hint: 'Required. How important is this goal?' },
    { name: 'isRecurring', label: 'Recurring Goal', type: 'checkbox', hint: 'Check if this is a yearly recurring goal (e.g., vacation).' }
];

document.addEventListener('DOMContentLoaded', () => { loadData(); });

async function loadData() {
    try {
        const data = await api.goals.getAll();
        renderTable(data);
        document.getElementById('entry-count').textContent = `${data.length} entries`;
    } catch (error) {
        document.getElementById('data-table').innerHTML = `<tr><td colspan="6" class="px-6 py-8 text-center text-danger-400">Error loading data</td></tr>`;
    }
}

function renderTable(data) {
    const tbody = document.getElementById('data-table');
    if (data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="px-6 py-8 text-center text-gray-500">No goals yet.</td></tr>`;
        return;
    }

    const priorityColors = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' };

    tbody.innerHTML = data.map(item => `
        <tr class="hover:bg-dark-600">
            <td class="px-6 py-4">
                <div class="flex items-center gap-2">
                    <span class="text-2xl">${item.icon || '🎯'}</span>
                    <span class="font-medium">${item.name || 'Goal'}</span>
                </div>
            </td>
            <td class="px-6 py-4 font-mono text-accent-400">${formatCurrency(item.targetAmount)}</td>
            <td class="px-6 py-4">${item.targetYear || '-'}</td>
            <td class="px-6 py-4">
                <span class="badge badge-${priorityColors[item.priority] || 'info'}">${item.priority || 'MEDIUM'}</span>
            </td>
            <td class="px-6 py-4">${item.isRecurring ? '<span class="text-success-400">Yes</span>' : 'No'}</td>
            <td class="px-6 py-4 text-right">
                <button onclick="editItem('${item.id}')" class="text-gray-400 hover:text-accent-400 mr-3">Edit</button>
                <button onclick="deleteItem('${item.id}')" class="text-gray-400 hover:text-danger-400">Delete</button>
            </td>
        </tr>
    `).join('');
}

function openAddModal() { currentEditId = null; openModal('Add Goal', goalFields); }
async function editItem(id) { await openEditModal('Goal', '/goals', id, goalFields); }
async function deleteItem(id) { await confirmDelete('/goals', id, loadData); }

async function handleSubmit(event) {
    event.preventDefault();
    const data = getFormData(event.target);
    try {
        if (currentEditId) { await api.goals.update(currentEditId, data); showToast('Goal updated!'); }
        else { await api.goals.create(data); showToast('Goal added!'); }
        closeModal(); loadData();
    } catch (error) { showToast('Error: ' + error.message, 'error'); }
}
