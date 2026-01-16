// Retyrment - Loans Page

const loanFields = [
    { name: 'type', label: 'Type', type: 'select', required: true, options: ['HOME', 'VEHICLE', 'PERSONAL', 'EDUCATION', 'CREDIT_CARD', 'OTHER'], hint: 'Required. Select the type of loan.' },
    { name: 'name', label: 'Bank / Description', type: 'text', required: true, hint: 'Required. Bank name or loan description. Cannot be empty.' },
    { name: 'originalAmount', label: 'Original Loan Amount (₹)', type: 'number', required: true, min: 0, hint: 'Required. Original sanctioned loan amount. Must be 0 or greater.' },
    { name: 'outstandingAmount', label: 'Outstanding Amount (₹)', type: 'number', required: true, min: 0, hint: 'Required. Current outstanding principal. Must be 0 or greater.' },
    { name: 'interestRate', label: 'Interest Rate (%)', type: 'number', required: true, step: '0.01', min: 0, hint: 'Required. Annual interest rate. Must be 0 or greater.' },
    { name: 'emi', label: 'Monthly EMI (₹)', type: 'number', required: true, min: 0, hint: 'Required. Monthly EMI amount. Must be 0 or greater.' },
    { name: 'tenureMonths', label: 'Original Tenure (Months)', type: 'number', required: true, min: 1, hint: 'Required. Original loan tenure in months. Must be at least 1.' },
    { name: 'remainingMonths', label: 'Remaining Months', type: 'number', min: 0, hint: 'Optional. Remaining EMIs to pay. Must be 0 or greater.' },
    { name: 'startDate', label: 'Start Date', type: 'date', hint: 'Required. When the loan was disbursed.' }
];

document.addEventListener('DOMContentLoaded', () => { loadData(); });

async function loadData() {
    try {
        const data = await api.loans.getAll();
        renderTable(data);
        document.getElementById('entry-count').textContent = `${data.length} entries`;
    } catch (error) {
        document.getElementById('data-table').innerHTML = `<tr><td colspan="6" class="px-6 py-8 text-center text-danger-400">Error loading data</td></tr>`;
    }
}

function renderTable(data) {
    const tbody = document.getElementById('data-table');
    if (data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="px-6 py-8 text-center text-gray-500">No loans yet.</td></tr>`;
        return;
    }

    tbody.innerHTML = data.map(item => `
        <tr class="hover:bg-dark-600">
            <td class="px-6 py-4"><div class="font-medium">${item.name || 'Loan'}</div><div class="text-xs text-gray-500">${(item.type || '').replace(/_/g, ' ')}</div></td>
            <td class="px-6 py-4 font-mono text-danger-400">${formatCurrency(item.outstandingAmount)}</td>
            <td class="px-6 py-4 font-mono">${formatCurrency(item.emi)}/mo</td>
            <td class="px-6 py-4">${formatPercent(item.interestRate)}</td>
            <td class="px-6 py-4">${item.remainingMonths || 0} months</td>
            <td class="px-6 py-4 text-right">
                <button onclick="editItem('${item.id}')" class="text-gray-400 hover:text-accent-400 mr-3">Edit</button>
                <button onclick="deleteItem('${item.id}')" class="text-gray-400 hover:text-danger-400">Delete</button>
            </td>
        </tr>
    `).join('');
}

function openAddModal() { currentEditId = null; openModal('Add Loan', loanFields); }
async function editItem(id) { await openEditModal('Loan', '/loans', id, loanFields); }
async function deleteItem(id) { await confirmDelete('/loans', id, loadData); }

async function handleSubmit(event) {
    event.preventDefault();
    const data = getFormData(event.target);
    try {
        if (currentEditId) { await api.loans.update(currentEditId, data); showToast('Loan updated!'); }
        else { await api.loans.create(data); showToast('Loan added!'); }
        closeModal(); loadData();
    } catch (error) { showToast('Error: ' + error.message, 'error'); }
}
