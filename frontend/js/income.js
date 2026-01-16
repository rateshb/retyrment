// Retyrment - Income Page

const incomeFields = [
    { name: 'source', label: 'Income Source', type: 'text', required: true, placeholder: 'e.g., Salary - TCS', hint: 'Required. Source name cannot be empty.' },
    { name: 'monthlyAmount', label: 'Monthly Amount (₹)', type: 'number', required: true, min: 0, hint: 'Required. Monthly income amount. Must be 0 or greater.' },
    { name: 'annualIncrement', label: 'Annual Increment (%)', type: 'number', value: 7, step: '0.1', min: 0, hint: 'Optional. Expected yearly salary increase. Must be 0 or greater.' },
    { name: 'startDate', label: 'Start Date', type: 'date', hint: 'Required. When this income source started.' },
    { name: 'isActive', label: 'Active', type: 'checkbox', checked: true, hint: 'Is this income source currently active?' }
];

document.addEventListener('DOMContentLoaded', () => {
    loadIncomeData();
});

async function loadIncomeData() {
    try {
        const data = await api.income.getAll();
        renderIncomeTable(data);
        document.getElementById('entry-count').textContent = `${data.length} entries`;
    } catch (error) {
        console.error('Error loading income:', error);
        document.getElementById('income-table').innerHTML = `
            <tr><td colspan="5" class="px-6 py-8 text-center text-danger-400">Error loading data. Is the server running?</td></tr>
        `;
    }
}

function renderIncomeTable(data) {
    const tbody = document.getElementById('income-table');
    
    if (data.length === 0) {
        tbody.innerHTML = `
            <tr><td colspan="5" class="px-6 py-8 text-center text-gray-500">
                No income sources yet. Click "Add Income" to add your first entry.
            </td></tr>
        `;
        return;
    }

    tbody.innerHTML = data.map(item => `
        <tr class="hover:bg-dark-600 transition-colors">
            <td class="px-6 py-4">
                <span class="font-medium">${item.source || 'Income'}</span>
            </td>
            <td class="px-6 py-4">
                <span class="font-mono text-accent-400">${formatCurrency(item.monthlyAmount)}</span>
            </td>
            <td class="px-6 py-4">${formatPercent(item.annualIncrement)}</td>
            <td class="px-6 py-4">
                ${item.isActive ? 
                    '<span class="badge badge-success">Active</span>' : 
                    '<span class="badge badge-danger">Inactive</span>'}
            </td>
            <td class="px-6 py-4 text-right">
                <button onclick="editIncome('${item.id}')" class="text-gray-400 hover:text-accent-400 mr-3">Edit</button>
                <button onclick="deleteIncome('${item.id}')" class="text-gray-400 hover:text-danger-400">Delete</button>
            </td>
        </tr>
    `).join('');
}

function openAddIncomeModal() {
    currentEditId = null;
    openModal('Add Income', incomeFields);
}

async function editIncome(id) {
    await openEditModal('Income', '/income', id, incomeFields);
}

async function deleteIncome(id) {
    await confirmDelete('/income', id, loadIncomeData);
}

async function handleIncomeSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const data = getFormData(form);

    try {
        if (currentEditId) {
            await api.income.update(currentEditId, data);
            showToast('Income updated successfully!');
        } else {
            await api.income.create(data);
            showToast('Income added successfully!');
        }
        closeModal();
        loadIncomeData();
    } catch (error) {
        showToast('Error saving income: ' + error.message, 'error');
    }
}
