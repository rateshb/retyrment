// Retyrment - Expenses Page

const expenseFields = [
    { name: 'category', label: 'Category', type: 'select', required: true, options: ['RENT', 'UTILITIES', 'GROCERIES', 'TRANSPORT', 'ENTERTAINMENT', 'EDUCATION', 'HEALTHCARE', 'SHOPPING', 'DINING', 'TRAVEL', 'SUBSCRIPTIONS', 'OTHER'], hint: 'Required. Select an expense category.' },
    { name: 'name', label: 'Description', type: 'text', required: true, hint: 'Required. Expense description. Cannot be empty.' },
    { name: 'monthlyAmount', label: 'Monthly Amount (₹)', type: 'number', required: true, min: 0, hint: 'Required. Monthly expense amount. Must be 0 or greater.' },
    { name: 'isFixed', label: 'Fixed Expense', type: 'checkbox', checked: true, hint: 'Fixed = recurring every month, Variable = occasional' }
];

document.addEventListener('DOMContentLoaded', () => { loadData(); });

async function loadData() {
    try {
        const data = await api.expenses.getAll();
        renderTable(data);
        document.getElementById('entry-count').textContent = `${data.length} entries`;
    } catch (error) {
        document.getElementById('data-table').innerHTML = `<tr><td colspan="5" class="px-6 py-8 text-center text-danger-400">Error loading data</td></tr>`;
    }
}

function renderTable(data) {
    const tbody = document.getElementById('data-table');
    if (data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="px-6 py-8 text-center text-gray-500">No expenses yet.</td></tr>`;
        return;
    }

    tbody.innerHTML = data.map(item => `
        <tr class="hover:bg-dark-600">
            <td class="px-6 py-4 font-medium">${(item.category || 'Other').replace(/_/g, ' ')}</td>
            <td class="px-6 py-4">${item.name || '-'}</td>
            <td class="px-6 py-4 font-mono text-warning-400">${formatCurrency(item.monthlyAmount)}</td>
            <td class="px-6 py-4">${item.isFixed ? '<span class="badge badge-info">Fixed</span>' : '<span class="badge badge-warning">Variable</span>'}</td>
            <td class="px-6 py-4 text-right">
                <button onclick="editItem('${item.id}')" class="text-gray-400 hover:text-accent-400 mr-3">Edit</button>
                <button onclick="deleteItem('${item.id}')" class="text-gray-400 hover:text-danger-400">Delete</button>
            </td>
        </tr>
    `).join('');
}

function openAddModal() { currentEditId = null; openModal('Add Expense', expenseFields); }
async function editItem(id) { await openEditModal('Expense', '/expenses', id, expenseFields); }
async function deleteItem(id) { await confirmDelete('/expenses', id, loadData); }

async function handleSubmit(event) {
    event.preventDefault();
    const data = getFormData(event.target);
    try {
        if (currentEditId) { await api.expenses.update(currentEditId, data); showToast('Expense updated!'); }
        else { await api.expenses.create(data); showToast('Expense added!'); }
        closeModal(); loadData();
    } catch (error) { showToast('Error: ' + error.message, 'error'); }
}
