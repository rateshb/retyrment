// Retyrment - Investments Page with Dynamic Fields

// Field configurations by investment type
const investmentTypeConfig = {
    MUTUAL_FUND: {
        fields: [
            { name: 'name', label: 'Fund Name', type: 'text', required: true, placeholder: 'e.g., Axis Bluechip Fund', hint: 'Required. Cannot be empty.' },
            { name: 'investedAmount', label: 'Total Invested (₹)', type: 'number', required: true, min: 0, hint: 'Required. Must be 0 or greater.' },
            { name: 'currentValue', label: 'Current NAV Value (₹)', type: 'number', required: true, min: 0, hint: 'Required. Must be 0 or greater.' },
            { name: 'evaluationDate', label: 'Value As Of (Evaluation Date)', type: 'date', hint: 'When was this NAV value checked?' },
            { name: 'monthlySip', label: 'Monthly SIP Amount (₹)', type: 'number', min: 0, placeholder: 'Leave empty if lumpsum', hint: 'Optional. Must be 0 or greater if provided.' },
            { name: 'sipDay', label: 'SIP Debit Day', type: 'number', min: 1, max: 28, placeholder: 'e.g., 5', hint: 'Optional. Day of month (1-28) when SIP is debited.' },
            { name: 'expectedReturn', label: 'Expected Annual Return (%)', type: 'number', value: 12, step: '0.1', min: 0, hint: 'Optional. Must be 0 or greater.' },
            { name: 'purchaseDate', label: 'First Investment Date', type: 'date', hint: 'When you first invested in this fund.' }
        ],
        icon: '📈'
    },
    STOCK: {
        fields: [
            { name: 'name', label: 'Stock/Company Name', type: 'text', required: true, placeholder: 'e.g., Reliance Industries', hint: 'Required. Cannot be empty.' },
            { name: 'investedAmount', label: 'Total Purchase Cost (₹)', type: 'number', required: true, min: 0, hint: 'Required. Must be 0 or greater.' },
            { name: 'currentValue', label: 'Current Market Value (₹)', type: 'number', required: true, min: 0, hint: 'Required. Must be 0 or greater.' },
            { name: 'expectedReturn', label: 'Expected Annual Return (%)', type: 'number', value: 15, step: '0.1', min: 0, hint: 'Optional. Must be 0 or greater.' },
            { name: 'purchaseDate', label: 'Purchase Date', type: 'date', hint: 'When you purchased the stock.' }
        ],
        icon: '📊'
    },
    FD: {
        fields: [
            { name: 'name', label: 'Bank/FD Name', type: 'text', required: true, placeholder: 'e.g., SBI Fixed Deposit', hint: 'Required. Cannot be empty.' },
            { name: 'investedAmount', label: 'Principal Amount (₹)', type: 'number', required: true, min: 0, hint: 'Required. Must be 0 or greater.' },
            { name: 'currentValue', label: 'Maturity Value (₹)', type: 'number', required: true, min: 0, hint: 'Required. Must be 0 or greater.' },
            { name: 'expectedReturn', label: 'Interest Rate (%)', type: 'number', value: 7, step: '0.1', min: 0, hint: 'Interest rate p.a. Must be 0 or greater.' },
            { name: 'purchaseDate', label: 'Deposit Date', type: 'date', hint: 'When FD was created.' },
            { name: 'maturityDate', label: 'Maturity Date', type: 'date', hint: 'When FD will mature.' }
        ],
        icon: '🏦'
    },
    RD: {
        fields: [
            { name: 'name', label: 'Bank/RD Name', type: 'text', required: true, placeholder: 'e.g., HDFC Recurring Deposit', hint: 'Required. Cannot be empty.' },
            { name: 'investedAmount', label: 'Total Deposited (₹)', type: 'number', required: true, min: 0, hint: 'Required. Total amount deposited so far.' },
            { name: 'currentValue', label: 'Current Value (₹)', type: 'number', required: true, min: 0, hint: 'Required. Current balance with interest.' },
            { name: 'monthlySip', label: 'Monthly Deposit (₹)', type: 'number', required: true, min: 0, hint: 'Required. Monthly RD installment.' },
            { name: 'rdDay', label: 'RD Debit Day', type: 'number', min: 1, max: 28, placeholder: 'e.g., 1', hint: 'Day of month (1-28) when RD is debited.' },
            { name: 'expectedReturn', label: 'Interest Rate (%)', type: 'number', value: 6.5, step: '0.1', min: 0, hint: 'Interest rate p.a. Must be 0 or greater.' },
            { name: 'purchaseDate', label: 'Start Date', type: 'date', hint: 'When RD was started.' },
            { name: 'maturityDate', label: 'Maturity Date', type: 'date', hint: 'When RD will mature.' }
        ],
        icon: '📅'
    },
    PPF: {
        fields: [
            { name: 'name', label: 'PPF Account', type: 'text', required: true, value: 'PPF Account', placeholder: 'e.g., SBI PPF', hint: 'Required. Cannot be empty.' },
            { name: 'investedAmount', label: 'Total Contribution Till Date (₹)', type: 'number', required: true, min: 0, hint: 'Required. Total amount deposited till now.' },
            { name: 'currentValue', label: 'Current Balance (₹)', type: 'number', required: true, min: 0, hint: 'Required. Current PPF balance.' },
            { name: 'yearlyContribution', label: 'Yearly Contribution (₹)', type: 'number', min: 0, placeholder: 'Max ₹1,50,000/year', hint: 'Optional. Max ₹1,50,000 per year allowed.' },
            { name: 'expectedReturn', label: 'PPF Interest Rate (%)', type: 'number', value: 7.1, step: '0.01', min: 0, hint: 'Current PPF rate. Must be 0 or greater.' },
            { name: 'purchaseDate', label: 'Account Opening Date', type: 'date', hint: 'When PPF account was opened.' }
        ],
        icon: '🏛️'
    },
    EPF: {
        fields: [
            { name: 'name', label: 'EPF Account', type: 'text', required: true, value: 'Employee Provident Fund', hint: 'Required. Cannot be empty.' },
            { name: 'investedAmount', label: 'Your Contribution Till Date (₹)', type: 'number', required: true, min: 0, hint: 'Required. Employee contribution only.' },
            { name: 'currentValue', label: 'Current EPF Balance (₹)', type: 'number', required: true, min: 0, hint: 'Required. Total balance (employee + employer + interest).' },
            { name: 'evaluationDate', label: 'Balance As Of Date', type: 'date', hint: 'When was this balance last checked?' },
            { name: 'monthlySip', label: 'Monthly Contribution (Employee + Employer) (₹)', type: 'number', required: true, min: 0, hint: 'Required. Combined monthly contribution.' },
            { name: 'expectedReturn', label: 'EPF Interest Rate (%)', type: 'number', value: 8.15, step: '0.01', min: 0, hint: 'Current EPF rate. Must be 0 or greater.' },
            { name: 'purchaseDate', label: 'Employment Start Date', type: 'date', hint: 'When EPF contributions started.' }
        ],
        icon: '👔'
    },
    NPS: {
        fields: [
            { name: 'name', label: 'NPS Account', type: 'text', required: true, value: 'National Pension System', hint: 'Required. Cannot be empty.' },
            { name: 'investedAmount', label: 'Total Contribution (₹)', type: 'number', required: true, min: 0, hint: 'Required. Total amount contributed.' },
            { name: 'currentValue', label: 'Current NAV Value (₹)', type: 'number', required: true, min: 0, hint: 'Required. Current portfolio value.' },
            { name: 'evaluationDate', label: 'NAV As Of Date', type: 'date', hint: 'When was this NAV value checked?' },
            { name: 'monthlySip', label: 'Monthly Contribution (₹)', type: 'number', min: 0, hint: 'Optional. Monthly NPS contribution if any.' },
            { name: 'sipDay', label: 'Contribution Day', type: 'number', min: 1, max: 28, placeholder: 'e.g., 10', hint: 'Day of month (1-28) for auto-debit.' },
            { name: 'expectedReturn', label: 'Expected Return (%)', type: 'number', value: 10, step: '0.1', min: 0, hint: 'Expected annual return. Must be 0 or greater.' },
            { name: 'purchaseDate', label: 'Account Opening Date', type: 'date', hint: 'When NPS account was opened.' }
        ],
        icon: '🧓'
    },
    REAL_ESTATE: {
        fields: [
            { name: 'name', label: 'Property Description', type: 'text', required: true, placeholder: 'e.g., 2BHK Flat in Mumbai', hint: 'Required. Cannot be empty.' },
            { name: 'investedAmount', label: 'Purchase Price (₹)', type: 'number', required: true, min: 0, hint: 'Required. Total purchase cost including registration.' },
            { name: 'currentValue', label: 'Current Market Value (₹)', type: 'number', required: true, min: 0, hint: 'Required. Estimated current market value.' },
            { name: 'expectedReturn', label: 'Expected Annual Appreciation (%)', type: 'number', value: 8, step: '0.1', min: 0, hint: 'Expected yearly appreciation. Must be 0 or greater.' },
            { name: 'purchaseDate', label: 'Purchase Date', type: 'date', hint: 'Date of property purchase/registration.' }
        ],
        icon: '🏠'
    },
    GOLD: {
        fields: [
            { name: 'name', label: 'Gold Description', type: 'text', required: true, placeholder: 'e.g., Gold Coins, Jewelry, SGB', hint: 'Required. Cannot be empty.' },
            { name: 'investedAmount', label: 'Purchase Cost (₹)', type: 'number', required: true, min: 0, hint: 'Required. Total purchase cost.' },
            { name: 'currentValue', label: 'Current Market Value (₹)', type: 'number', required: true, min: 0, hint: 'Required. Current value based on gold prices.' },
            { name: 'expectedReturn', label: 'Expected Annual Return (%)', type: 'number', value: 8, step: '0.1', min: 0, hint: 'Expected yearly return. Must be 0 or greater.' },
            { name: 'purchaseDate', label: 'Purchase Date', type: 'date', hint: 'When gold was purchased.' }
        ],
        icon: '🥇'
    },
    CRYPTO: {
        fields: [
            { name: 'name', label: 'Cryptocurrency', type: 'text', required: true, placeholder: 'e.g., Bitcoin, Ethereum', hint: 'Required. Cannot be empty.' },
            { name: 'investedAmount', label: 'Total Invested (₹)', type: 'number', required: true, min: 0, hint: 'Required. Total amount invested in INR.' },
            { name: 'currentValue', label: 'Current Value (₹)', type: 'number', required: true, min: 0, hint: 'Required. Current portfolio value in INR.' },
            { name: 'expectedReturn', label: 'Expected Return (%)', type: 'number', value: 20, step: '0.1', min: 0, hint: 'Expected yearly return (highly volatile). Must be 0 or greater.' },
            { name: 'purchaseDate', label: 'First Purchase Date', type: 'date', hint: 'Date of first purchase.' }
        ],
        icon: '₿'
    },
    CASH: {
        fields: [
            { name: 'name', label: 'Account/Description', type: 'text', required: true, placeholder: 'e.g., Savings Account, Emergency Fund', hint: 'Required. Cannot be empty.' },
            { name: 'currentValue', label: 'Current Balance (₹)', type: 'number', required: true, min: 0, hint: 'Required. Current account balance.' },
            { name: 'expectedReturn', label: 'Interest Rate (%)', type: 'number', value: 3, step: '0.1', min: 0, hint: 'Savings account interest rate. Must be 0 or greater.' }
        ],
        icon: '💵'
    },
    OTHER: {
        fields: [
            { name: 'name', label: 'Investment Name', type: 'text', required: true, placeholder: 'Describe your investment', hint: 'Required. Cannot be empty.' },
            { name: 'investedAmount', label: 'Invested Amount (₹)', type: 'number', required: true, min: 0, hint: 'Required. Must be 0 or greater.' },
            { name: 'currentValue', label: 'Current Value (₹)', type: 'number', required: true, min: 0, hint: 'Required. Must be 0 or greater.' },
            { name: 'monthlySip', label: 'Monthly Contribution (₹)', type: 'number', min: 0, hint: 'Optional. Must be 0 or greater if provided.' },
            { name: 'expectedReturn', label: 'Expected Return (%)', type: 'number', value: 10, step: '0.1', min: 0, hint: 'Optional. Must be 0 or greater.' },
            { name: 'purchaseDate', label: 'Investment Date', type: 'date', hint: 'When the investment was made.' }
        ],
        icon: '📦'
    }
};

let selectedType = 'MUTUAL_FUND';

document.addEventListener('DOMContentLoaded', async () => {
    // Load feature access to get allowed investment types
    try {
        const featuresResponse = await api.auth.features();
        const features = featuresResponse.features || {};
        // Always set it, even if empty array (to distinguish from undefined)
        if (features.allowedInvestmentTypes !== undefined) {
            window.allowedInvestmentTypes = features.allowedInvestmentTypes;
        }
    } catch (error) {
        console.error('Error loading feature access:', error);
    }
    loadData();
});

async function loadData() {
    try {
        const data = await api.investments.getAll();
        renderTable(data);
        document.getElementById('entry-count').textContent = `${data.length} entries`;
    } catch (error) {
        console.error('Error loading investments:', error);
        document.getElementById('data-table').innerHTML = `
            <tr><td colspan="6" class="px-6 py-8 text-center text-danger-500">Error loading data. Is the backend running?</td></tr>
        `;
    }
}

function renderTable(data) {
    const tbody = document.getElementById('data-table');
    
    if (data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="px-6 py-8 text-center text-slate-500">No investments yet. Click "Add Investment" to add your first entry.</td></tr>`;
        return;
    }

    tbody.innerHTML = data.map(item => {
        const config = investmentTypeConfig[item.type] || investmentTypeConfig.OTHER;
        const invested = item.investedAmount || item.currentValue || 0;
        const current = item.currentValue || 0;
        const gain = current - invested;
        const gainPct = invested ? ((gain / invested) * 100).toFixed(1) : 0;
        
        return `
        <tr class="hover:bg-slate-50 transition-colors">
            <td class="px-6 py-4">
                <div class="flex items-center gap-3">
                    <span class="text-2xl">${config.icon}</span>
                    <div>
                        <div class="font-medium text-slate-800">${item.name || 'Investment'}</div>
                        <div class="text-xs text-slate-500">${(item.type || '').replace(/_/g, ' ')}</div>
                    </div>
                </div>
            </td>
            <td class="px-6 py-4 font-mono text-slate-700">${formatCurrency(invested)}</td>
            <td class="px-6 py-4 font-mono text-primary-600 font-medium">${formatCurrency(current)}</td>
            <td class="px-6 py-4 font-mono ${gain >= 0 ? 'text-success-600' : 'text-danger-600'}">
                ${gain >= 0 ? '+' : ''}${formatCurrency(gain)} (${gain >= 0 ? '+' : ''}${gainPct}%)
            </td>
            <td class="px-6 py-4 text-slate-600">
                ${item.monthlySip ? 
                    formatCurrency(item.monthlySip) + '/mo' + 
                    (item.sipDay ? ` <span class="text-xs text-slate-400">(${getOrdinal(item.sipDay)})</span>` : '') : 
                  item.yearlyContribution ? formatCurrency(item.yearlyContribution) + '/yr' : '-'}
            </td>
            <td class="px-6 py-4 text-right">
                <button onclick="editItem('${item.id}')" class="text-primary-500 hover:text-primary-700 mr-3 font-medium">Edit</button>
                <button onclick="deleteItem('${item.id}')" class="text-slate-400 hover:text-danger-500">Delete</button>
            </td>
        </tr>
    `}).join('');
}

function openAddInvestmentModal() {
    currentEditId = null;
    selectedType = 'MUTUAL_FUND';
    showTypeSelector();
}

function showTypeSelector() {
    const modal = document.getElementById('modal');
    document.getElementById('modal-title').textContent = 'Select Investment Type';
    
    // Get allowed investment types from feature access
    // If allowedInvestmentTypes is set (even if empty array), use it to filter
    // If undefined/null, show all types (backward compatibility)
    const allowedTypes = window.allowedInvestmentTypes;
    const allTypes = Object.entries(investmentTypeConfig);
    
    // Filter types based on feature access
    // If allowedTypes is explicitly set (array), filter by it
    // If undefined/null, show all types
    const availableTypes = (allowedTypes !== undefined && allowedTypes !== null)
        ? allTypes.filter(([type]) => allowedTypes.includes(type))
        : allTypes; // If not set, show all types (backward compatibility)
    
    const fieldsContainer = document.getElementById('modal-fields');
    
    if (availableTypes.length === 0) {
        fieldsContainer.innerHTML = `
            <div class="text-center py-8 text-slate-500">
                <p class="mb-2">No investment types available.</p>
                <p class="text-sm">Contact your administrator to enable investment types.</p>
            </div>
        `;
    } else {
        fieldsContainer.innerHTML = `
            <div class="grid grid-cols-3 gap-3">
                ${availableTypes.map(([type, config]) => `
                    <button type="button" onclick="selectTypeAndShowForm('${type}')"
                        class="p-4 rounded-xl border-2 border-slate-200 hover:border-primary-400 hover:bg-primary-50 transition-all text-center group">
                        <div class="text-3xl mb-2">${config.icon}</div>
                        <div class="text-sm font-medium text-slate-700 group-hover:text-primary-700">${type.replace(/_/g, ' ')}</div>
                    </button>
                `).join('')}
            </div>
        `;
    }
    
    // Hide the save button for type selector
    const saveBtn = modal.querySelector('button[type="submit"]');
    if (saveBtn) saveBtn.style.display = 'none';
    
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

function selectTypeAndShowForm(type) {
    selectedType = type;
    const config = investmentTypeConfig[type];
    
    document.getElementById('modal-title').textContent = `Add ${type.replace(/_/g, ' ')} ${config.icon}`;
    
    // Build form with type-specific fields
    const fields = [
        { name: 'type', type: 'hidden', value: type },
        ...config.fields
    ];
    
    const fieldsContainer = document.getElementById('modal-fields');
    fieldsContainer.innerHTML = fields.map(field => {
        if (field.type === 'hidden') {
            return `<input type="hidden" name="${field.name}" value="${field.value}">`;
        }
        
        let value = field.value || '';
        if (field.type === 'date' && !value) {
            value = getTodayDate();
        }
        
        const isAmountField = field.type === 'number' && 
            (field.name.toLowerCase().includes('amount') || 
             field.name.toLowerCase().includes('value') || 
             field.name.toLowerCase().includes('contribution') ||
             field.name.toLowerCase().includes('sip'));
        
        const amountHint = isAmountField ? `<div class="text-xs text-primary-600 mt-1.5 amount-words italic" data-for="${field.name}"></div>` : '';
        const fieldHint = field.hint ? `<div class="text-xs text-slate-400 mt-1">${field.hint}</div>` : '';
        const onInputHandler = isAmountField ? `oninput="updateAmountWords('${field.name}', this.value)"` : '';
        const requiredStar = field.required ? '<span class="text-danger-500 ml-1">*</span>' : '';
        
        return `
            <div class="mb-4">
                <label class="block text-sm text-slate-600 mb-2 font-medium">${field.label}${requiredStar}</label>
                <input type="${field.type}" name="${field.name}" value="${value}"
                       ${field.required ? 'required' : ''}
                       ${field.placeholder ? `placeholder="${field.placeholder}"` : ''}
                       ${field.step ? `step="${field.step}"` : ''}
                       ${field.min !== undefined ? `min="${field.min}"` : ''}
                       ${field.max !== undefined ? `max="${field.max}"` : ''}
                       ${onInputHandler}
                       class="w-full px-4 py-2.5 bg-white border border-slate-300 rounded-lg text-slate-800 focus:border-primary-500 focus:ring-2 focus:ring-primary-200 focus:outline-none transition-all">
                ${amountHint}
                ${fieldHint}
            </div>
        `;
    }).join('');
    
    // Show the save button
    const modal = document.getElementById('modal');
    const saveBtn = modal.querySelector('button[type="submit"]');
    if (saveBtn) saveBtn.style.display = 'block';
}

async function editItem(id) {
    try {
        const data = await api.get(`/investments/${id}`);
        currentEditId = id;
        selectedType = data.type || 'OTHER';
        
        const config = investmentTypeConfig[selectedType] || investmentTypeConfig.OTHER;
        document.getElementById('modal-title').textContent = `Edit ${selectedType.replace(/_/g, ' ')} ${config.icon}`;
        
        const fields = [
            { name: 'type', type: 'hidden', value: selectedType },
            ...config.fields
        ];
        
        const fieldsContainer = document.getElementById('modal-fields');
        fieldsContainer.innerHTML = fields.map(field => {
            if (field.type === 'hidden') {
                return `<input type="hidden" name="${field.name}" value="${data[field.name] || field.value}">`;
            }
            
            let value = data[field.name] !== undefined ? data[field.name] : (field.value || '');
            if (field.type === 'date' && value && typeof value === 'string') {
                value = value.split('T')[0];
            }
            
            const isAmountField = field.type === 'number' && 
                (field.name.toLowerCase().includes('amount') || 
                 field.name.toLowerCase().includes('value') || 
                 field.name.toLowerCase().includes('contribution') ||
                 field.name.toLowerCase().includes('sip'));
            
            const amountHint = isAmountField ? `<div class="text-xs text-primary-600 mt-1.5 amount-words italic" data-for="${field.name}"></div>` : '';
            const fieldHint = field.hint ? `<div class="text-xs text-slate-400 mt-1">${field.hint}</div>` : '';
            const onInputHandler = isAmountField ? `oninput="updateAmountWords('${field.name}', this.value)"` : '';
            const requiredStar = field.required ? '<span class="text-danger-500 ml-1">*</span>' : '';
            
            return `
                <div class="mb-4">
                    <label class="block text-sm text-slate-600 mb-2 font-medium">${field.label}${requiredStar}</label>
                    <input type="${field.type}" name="${field.name}" value="${value}"
                           ${field.required ? 'required' : ''}
                           ${field.placeholder ? `placeholder="${field.placeholder}"` : ''}
                           ${field.step ? `step="${field.step}"` : ''}
                           ${field.min !== undefined ? `min="${field.min}"` : ''}
                           ${field.max !== undefined ? `max="${field.max}"` : ''}
                           ${onInputHandler}
                           class="w-full px-4 py-2.5 bg-white border border-slate-300 rounded-lg text-slate-800 focus:border-primary-500 focus:ring-2 focus:ring-primary-200 focus:outline-none transition-all">
                    ${amountHint}
                    ${fieldHint}
                </div>
            `;
        }).join('');
        
        // Initialize amount words for existing values
        setTimeout(() => {
            fields.forEach(field => {
                if (data[field.name] && typeof updateAmountWords === 'function') {
                    updateAmountWords(field.name, data[field.name]);
                }
            });
        }, 100);
        
        const modal = document.getElementById('modal');
        const saveBtn = modal.querySelector('button[type="submit"]');
        if (saveBtn) saveBtn.style.display = 'block';
        
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    } catch (error) {
        showToast('Error loading investment', 'error');
    }
}

async function deleteItem(id) {
    await confirmDelete('/investments', id, loadData);
}

async function handleSubmit(event) {
    event.preventDefault();
    const data = getFormData(event.target);
    
    // Ensure type is set
    if (!data.type) {
        data.type = selectedType;
    }
    
    // For CASH type, set investedAmount = currentValue if not provided
    if (data.type === 'CASH' && !data.investedAmount) {
        data.investedAmount = data.currentValue;
    }

    try {
        if (currentEditId) {
            await api.investments.update(currentEditId, data);
            showToast('Investment updated!');
        } else {
            await api.investments.create(data);
            showToast('Investment added!');
        }
        closeModal();
        loadData();
    } catch (error) {
        showToast('Error: ' + error.message, 'error');
    }
}

// Helper function to get ordinal suffix (1st, 2nd, 3rd, etc.)
function getOrdinal(n) {
    if (!n) return '';
    const s = ['th', 'st', 'nd', 'rd'];
    const v = n % 100;
    return n + (s[(v - 20) % 10] || s[v] || s[0]);
}
