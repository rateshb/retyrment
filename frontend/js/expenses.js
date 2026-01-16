// Retyrment - Expenses Page with Time-Bound Expense Support

// All expense data
let allExpenses = [];
// Note: currentEditId is declared in common.js
let currentView = 'table';

// Category labels for display
const categoryLabels = {
    'RENT': '🏠 Rent',
    'UTILITIES': '💡 Utilities',
    'GROCERIES': '🛒 Groceries',
    'TRANSPORT': '🚗 Transport',
    'ENTERTAINMENT': '🎬 Entertainment',
    'SCHOOL_FEE': '🎒 School Fee',
    'COLLEGE_FEE': '🎓 College Fee',
    'TUITION': '📖 Tuition',
    'COACHING': '✏️ Coaching',
    'BOOKS_SUPPLIES': '📕 Books & Supplies',
    'HOSTEL': '🏨 Hostel',
    'HEALTHCARE': '🏥 Healthcare',
    'SHOPPING': '🛍️ Shopping',
    'DINING': '🍽️ Dining',
    'TRAVEL': '✈️ Travel',
    'SUBSCRIPTIONS': '📺 Subscriptions',
    'CHILDCARE': '👶 Childcare',
    'DAYCARE': '🧒 Daycare',
    'ELDERLY_CARE': '👴 Elderly Care',
    'MAINTENANCE': '🔧 Maintenance',
    'SOCIETY_CHARGES': '🏘️ Society Charges',
    'INSURANCE_PREMIUM': '🛡️ Insurance Premium',
    'OTHER': '📋 Other'
};

const frequencyLabels = {
    'MONTHLY': 'Monthly',
    'QUARTERLY': 'Quarterly',
    'HALF_YEARLY': 'Half-Yearly',
    'YEARLY': 'Yearly',
    'ONE_TIME': 'One-Time'
};

document.addEventListener('DOMContentLoaded', () => {
    loadData();
    populateCategoryFilter();
});

async function loadData() {
    try {
        allExpenses = await api.expenses.getAll();
        renderView();
        updateSummaryCards();
        updateFilteredCount();
        checkInvestmentOpportunities();
    } catch (error) {
        console.error('Error loading expenses:', error);
        document.getElementById('data-table').innerHTML = `<tr><td colspan="7" class="px-6 py-8 text-center text-danger-400">Error loading data</td></tr>`;
    }
}

function renderView() {
    if (currentView === 'table') {
        renderTable(filterExpensesList());
    } else {
        renderSummary();
    }
}

function switchView(view) {
    currentView = view;
    
    // Update buttons
    document.getElementById('view-table').className = view === 'table' 
        ? 'px-3 py-2 text-sm bg-primary-100 text-primary-700 rounded-lg font-medium'
        : 'px-3 py-2 text-sm bg-white text-slate-600 rounded-lg border border-slate-300 hover:bg-slate-50';
    document.getElementById('view-summary').className = view === 'summary'
        ? 'px-3 py-2 text-sm bg-primary-100 text-primary-700 rounded-lg font-medium'
        : 'px-3 py-2 text-sm bg-white text-slate-600 rounded-lg border border-slate-300 hover:bg-slate-50';
    
    // Show/hide views
    document.getElementById('table-view').classList.toggle('hidden', view !== 'table');
    document.getElementById('summary-view').classList.toggle('hidden', view !== 'summary');
    
    renderView();
}

function populateCategoryFilter() {
    const select = document.getElementById('filter-category');
    if (!select) return;
    
    const categories = Object.keys(categoryLabels);
    categories.forEach(cat => {
        const option = document.createElement('option');
        option.value = cat;
        option.textContent = categoryLabels[cat];
        select.appendChild(option);
    });
}

function filterExpensesList() {
    const categoryFilter = document.getElementById('filter-category')?.value || '';
    const typeFilter = document.getElementById('filter-type')?.value || '';
    
    return allExpenses.filter(exp => {
        if (categoryFilter && exp.category !== categoryFilter) return false;
        
        if (typeFilter === 'fixed' && !exp.isFixed) return false;
        if (typeFilter === 'variable' && exp.isFixed) return false;
        if (typeFilter === 'timebound' && !exp.isTimeBound) return false;
        if (typeFilter === 'recurring' && exp.isTimeBound) return false;
        
        return true;
    });
}

function filterExpenses() {
    renderView();
    updateFilteredCount();
}

function updateFilteredCount() {
    const filtered = filterExpensesList();
    document.getElementById('entry-count').textContent = `${filtered.length} entries`;
}

function updateSummaryCards() {
    let totalMonthly = 0;
    let fixedTotal = 0;
    let variableTotal = 0;
    let timeboundTotal = 0;
    let fixedCount = 0;
    let variableCount = 0;
    let timeboundCount = 0;
    
    allExpenses.forEach(exp => {
        const monthlyEq = getMonthlyEquivalent(exp.amount, exp.frequency);
        totalMonthly += monthlyEq;
        
        if (exp.isTimeBound) {
            timeboundTotal += monthlyEq;
            timeboundCount++;
        }
        
        if (exp.isFixed) {
            fixedTotal += monthlyEq;
            fixedCount++;
        } else {
            variableTotal += monthlyEq;
            variableCount++;
        }
    });
    
    document.getElementById('total-monthly').textContent = formatCurrency(totalMonthly);
    document.getElementById('total-yearly').textContent = formatCurrency(totalMonthly * 12) + '/year';
    document.getElementById('fixed-total').textContent = formatCurrency(fixedTotal);
    document.getElementById('fixed-count').textContent = `${fixedCount} items`;
    document.getElementById('variable-total').textContent = formatCurrency(variableTotal);
    document.getElementById('variable-count').textContent = `${variableCount} items`;
    document.getElementById('timebound-total').textContent = formatCurrency(timeboundTotal);
    document.getElementById('timebound-count').textContent = `${timeboundCount} items`;
}

function checkInvestmentOpportunities() {
    const timeBoundExpenses = allExpenses.filter(exp => exp.isTimeBound);
    const banner = document.getElementById('investment-opportunity-banner');
    
    if (timeBoundExpenses.length > 0) {
        let totalFreedUp = 0;
        timeBoundExpenses.forEach(exp => {
            totalFreedUp += getMonthlyEquivalent(exp.amount, exp.frequency);
        });
        
        document.getElementById('opportunity-message').textContent = 
            `${timeBoundExpenses.length} expense(s) worth ${formatCurrency(totalFreedUp)}/month will end before retirement. This can become investment!`;
        banner.classList.remove('hidden');
    } else {
        banner.classList.add('hidden');
    }
}

function getMonthlyEquivalent(amount, frequency) {
    if (!amount || !frequency) return 0;
    switch (frequency) {
        case 'MONTHLY': return amount;
        case 'QUARTERLY': return amount / 3;
        case 'HALF_YEARLY': return amount / 6;
        case 'YEARLY': return amount / 12;
        case 'ONE_TIME': return 0;
        default: return amount;
    }
}

function renderTable(data) {
    const tbody = document.getElementById('data-table');
    if (data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="px-6 py-8 text-center text-gray-500">No expenses yet. Click "Add Expense" to get started.</td></tr>`;
        return;
    }

    tbody.innerHTML = data.map(item => {
        const monthlyEq = getMonthlyEquivalent(item.amount, item.frequency);
        const categoryLabel = categoryLabels[item.category] || item.category;
        const frequencyLabel = frequencyLabels[item.frequency] || item.frequency;
        
        // Determine end date display
        let endDateDisplay = '-';
        if (item.isTimeBound) {
            if (item.endDate) {
                endDateDisplay = formatDate(item.endDate);
            } else if (item.endAge && item.dependentDob) {
                const dependentAge = calculateAge(item.dependentDob);
                const yearsLeft = item.endAge - dependentAge;
                const endYear = new Date().getFullYear() + yearsLeft;
                endDateDisplay = `~${endYear} (${item.dependentName || 'Dependent'} turns ${item.endAge})`;
            }
        }
        
        // Type badges
        let typeBadges = '';
        if (item.isFixed) {
            typeBadges += '<span class="px-2 py-0.5 text-xs rounded-full bg-blue-100 text-blue-700 mr-1">Fixed</span>';
        } else {
            typeBadges += '<span class="px-2 py-0.5 text-xs rounded-full bg-amber-100 text-amber-700 mr-1">Variable</span>';
        }
        if (item.isTimeBound) {
            typeBadges += '<span class="px-2 py-0.5 text-xs rounded-full bg-purple-100 text-purple-700">Time-Bound</span>';
        }
        
        return `
        <tr class="hover:bg-slate-50 ${item.isTimeBound ? 'border-l-4 border-purple-400' : ''}">
            <td class="px-4 py-3">
                <span class="font-medium">${categoryLabel}</span>
            </td>
            <td class="px-4 py-3">
                <div class="font-medium text-slate-800">${item.name || '-'}</div>
                ${item.dependentName ? `<div class="text-xs text-slate-400">For: ${item.dependentName}</div>` : ''}
            </td>
            <td class="px-4 py-3">
                <div class="font-mono text-slate-800">${formatCurrency(item.amount)}</div>
                <div class="text-xs text-slate-400">${formatCurrency(monthlyEq)}/mo</div>
            </td>
            <td class="px-4 py-3">
                <span class="px-2 py-1 text-xs rounded bg-slate-100 text-slate-600">${frequencyLabel}</span>
            </td>
            <td class="px-4 py-3">${typeBadges}</td>
            <td class="px-4 py-3 text-sm ${item.isTimeBound ? 'text-purple-600 font-medium' : 'text-slate-400'}">${endDateDisplay}</td>
            <td class="px-4 py-3 text-right">
                <button onclick="editItem('${item.id}')" class="text-slate-400 hover:text-primary-500 mr-3">Edit</button>
                <button onclick="deleteItem('${item.id}')" class="text-slate-400 hover:text-danger-500">Delete</button>
            </td>
        </tr>
    `}).join('');
}

function renderSummary() {
    renderCategoryBreakdown();
    renderDependentBreakdown();
    renderExpenseTimeline();
    renderExpenseProjection();
}

function renderCategoryBreakdown() {
    const container = document.getElementById('category-breakdown');
    if (!container) return;
    
    // Group by category
    const byCategory = {};
    allExpenses.forEach(exp => {
        const monthlyEq = getMonthlyEquivalent(exp.amount, exp.frequency);
        byCategory[exp.category] = (byCategory[exp.category] || 0) + monthlyEq;
    });
    
    // Sort by amount
    const sorted = Object.entries(byCategory).sort((a, b) => b[1] - a[1]);
    const totalMonthly = sorted.reduce((sum, [_, amt]) => sum + amt, 0);
    
    if (sorted.length === 0) {
        container.innerHTML = '<div class="text-slate-400 text-sm">No expenses</div>';
        return;
    }
    
    container.innerHTML = sorted.map(([cat, amount]) => {
        const percent = totalMonthly > 0 ? (amount / totalMonthly * 100) : 0;
        const label = categoryLabels[cat] || cat;
        return `
            <div class="flex items-center gap-3">
                <div class="flex-1">
                    <div class="flex justify-between text-sm mb-1">
                        <span class="text-slate-700">${label}</span>
                        <span class="font-mono text-slate-600">${formatCurrency(amount)}/mo</span>
                    </div>
                    <div class="h-2 bg-slate-100 rounded-full overflow-hidden">
                        <div class="h-full bg-primary-500 rounded-full" style="width: ${percent}%"></div>
                    </div>
                </div>
                <span class="text-xs text-slate-400 w-12 text-right">${percent.toFixed(0)}%</span>
            </div>
        `;
    }).join('');
}

function renderDependentBreakdown() {
    const container = document.getElementById('dependent-breakdown');
    if (!container) return;
    
    // Group by dependent
    const byDependent = {};
    allExpenses.forEach(exp => {
        if (exp.dependentName) {
            const monthlyEq = getMonthlyEquivalent(exp.amount, exp.frequency);
            if (!byDependent[exp.dependentName]) {
                byDependent[exp.dependentName] = { total: 0, expenses: [] };
            }
            byDependent[exp.dependentName].total += monthlyEq;
            byDependent[exp.dependentName].expenses.push(exp);
        }
    });
    
    const dependents = Object.entries(byDependent);
    
    if (dependents.length === 0) {
        container.innerHTML = '<div class="text-slate-400 text-sm">No dependent-linked expenses. Add school fees or childcare expenses with a dependent name.</div>';
        return;
    }
    
    container.innerHTML = dependents.map(([name, data]) => {
        const expensesList = data.expenses.map(e => categoryLabels[e.category] || e.category).join(', ');
        return `
            <div class="p-3 bg-slate-50 rounded-lg">
                <div class="flex justify-between items-center mb-1">
                    <span class="font-medium text-slate-800">👦 ${name}</span>
                    <span class="font-mono text-slate-700">${formatCurrency(data.total)}/mo</span>
                </div>
                <div class="text-xs text-slate-500">${data.expenses.length} expense(s): ${expensesList}</div>
            </div>
        `;
    }).join('');
}

function renderExpenseTimeline() {
    const container = document.getElementById('expense-timeline');
    if (!container) return;
    
    const timeBound = allExpenses.filter(exp => exp.isTimeBound);
    
    if (timeBound.length === 0) {
        container.innerHTML = '<div class="text-slate-400 text-sm">No time-bound expenses. Add school fees or temporary expenses to see them here.</div>';
        return;
    }
    
    // Calculate end years and sort
    const currentYear = new Date().getFullYear();
    const withEndYears = timeBound.map(exp => {
        let endYear = currentYear + 30; // Default far future
        
        if (exp.endDate) {
            endYear = new Date(exp.endDate).getFullYear();
        } else if (exp.endAge && exp.dependentDob) {
            const dependentAge = calculateAge(exp.dependentDob);
            endYear = currentYear + (exp.endAge - dependentAge);
        }
        
        return { ...exp, endYear, monthlyEq: getMonthlyEquivalent(exp.amount, exp.frequency) };
    }).sort((a, b) => a.endYear - b.endYear);
    
    container.innerHTML = withEndYears.map(exp => {
        const yearsLeft = exp.endYear - currentYear;
        return `
            <div class="flex items-center gap-4 p-3 bg-purple-50 rounded-lg border border-purple-200">
                <div class="w-16 h-16 rounded-full bg-purple-200 text-purple-700 flex flex-col items-center justify-center">
                    <span class="text-lg font-bold">${exp.endYear}</span>
                    <span class="text-xs">${yearsLeft}y left</span>
                </div>
                <div class="flex-1">
                    <div class="font-medium text-slate-800">${exp.name}</div>
                    <div class="text-sm text-slate-500">${categoryLabels[exp.category] || exp.category}</div>
                    ${exp.dependentName ? `<div class="text-xs text-purple-600">For: ${exp.dependentName}</div>` : ''}
                </div>
                <div class="text-right">
                    <div class="font-mono text-purple-700 font-medium">${formatCurrency(exp.monthlyEq)}/mo</div>
                    <div class="text-xs text-emerald-600">💰 Freed up after ${exp.endYear}</div>
                </div>
            </div>
        `;
    }).join('');
}

function renderExpenseProjection() {
    const container = document.getElementById('expense-projection-table');
    if (!container) return;
    
    const currentYear = new Date().getFullYear();
    const yearsToShow = 15;
    
    // Build projection
    const projection = [];
    for (let i = 0; i <= yearsToShow; i++) {
        const year = currentYear + i;
        let totalMonthly = 0;
        let freedUp = 0;
        
        allExpenses.forEach(exp => {
            const monthlyEq = getMonthlyEquivalent(exp.amount, exp.frequency);
            let isActive = true;
            
            if (exp.isTimeBound) {
                let endYear = currentYear + 30;
                if (exp.endDate) {
                    endYear = new Date(exp.endDate).getFullYear();
                } else if (exp.endAge && exp.dependentDob) {
                    const dependentAge = calculateAge(exp.dependentDob);
                    endYear = currentYear + (exp.endAge - dependentAge);
                }
                
                if (year > endYear) {
                    isActive = false;
                    if (year === endYear + 1) {
                        freedUp += monthlyEq;
                    }
                }
            }
            
            if (isActive) {
                // Apply annual increase if set
                let inflatedAmount = monthlyEq;
                if (exp.annualIncreasePercent && exp.annualIncreasePercent > 0) {
                    inflatedAmount = monthlyEq * Math.pow(1 + exp.annualIncreasePercent / 100, i);
                }
                totalMonthly += inflatedAmount;
            }
        });
        
        projection.push({ year, monthly: totalMonthly, yearly: totalMonthly * 12, freedUp });
    }
    
    container.innerHTML = projection.map(p => `
        <tr class="${p.freedUp > 0 ? 'bg-emerald-50' : ''}">
            <td class="px-3 py-2 text-slate-700">${p.year}</td>
            <td class="px-3 py-2 text-right font-mono text-slate-700">${formatCurrency(p.monthly)}</td>
            <td class="px-3 py-2 text-right font-mono text-slate-700">${formatCurrency(p.yearly)}</td>
            <td class="px-3 py-2 text-right font-mono ${p.freedUp > 0 ? 'text-emerald-600 font-medium' : 'text-slate-400'}">
                ${p.freedUp > 0 ? '+' + formatCurrency(p.freedUp) + '/mo' : '-'}
            </td>
        </tr>
    `).join('');
}

function calculateAge(dob) {
    if (!dob) return 0;
    const birthDate = new Date(dob);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
        age--;
    }
    return age;
}

// Modal Functions
function openAddModal() {
    currentEditId = null;
    document.getElementById('modal-title').textContent = 'Add Expense';
    document.getElementById('expense-form').reset();
    
    // Reset time-bound fields visibility
    document.getElementById('timebound-fields').classList.add('hidden');
    document.getElementById('investment-preview').classList.add('hidden');
    document.getElementById('monthly-equivalent').textContent = '₹0';
    document.getElementById('amount-words').textContent = '';
    document.getElementById('dependent-age-info').classList.add('hidden');
    
    // Set default date to today
    document.querySelector('[name="startDate"]').value = new Date().toISOString().split('T')[0];
    
    const modal = document.getElementById('modal');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

async function editItem(id) {
    currentEditId = id;
    
    try {
        const expense = await api.get(`/expenses/${id}`);
        
        document.getElementById('modal-title').textContent = 'Edit Expense';
        
        // Populate basic fields
        document.querySelector('[name="category"]').value = expense.category || '';
        document.querySelector('[name="name"]').value = expense.name || '';
        document.querySelector('[name="amount"]').value = expense.amount || '';
        document.querySelector('[name="frequency"]').value = expense.frequency || 'MONTHLY';
        document.querySelector('[name="isFixed"]').checked = expense.isFixed !== false;
        
        // Populate time-bound fields
        document.querySelector('[name="isTimeBound"]').checked = expense.isTimeBound || false;
        toggleTimeBoundFields();
        
        if (expense.isTimeBound) {
            document.querySelector('[name="startDate"]').value = expense.startDate || '';
            document.querySelector('[name="endDate"]').value = expense.endDate || '';
            document.querySelector('[name="dependentName"]').value = expense.dependentName || '';
            document.querySelector('[name="dependentDob"]').value = expense.dependentDob || '';
            document.querySelector('[name="endAge"]').value = expense.endAge || '';
            document.querySelector('[name="annualIncreasePercent"]').value = expense.annualIncreasePercent || '';
            
            calculateDependentAge();
        }
        
        updateMonthlyEquivalent();
        updateAmountWords();
        
        const modal = document.getElementById('modal');
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    } catch (error) {
        console.error('Error loading expense:', error);
        showToast('Error loading expense', 'error');
    }
}

function closeModal() {
    const modal = document.getElementById('modal');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
    currentEditId = null;
}

async function deleteItem(id) {
    if (!confirm('Are you sure you want to delete this expense?')) return;
    
    try {
        await api.expenses.delete(id);
        showToast('Expense deleted!');
        loadData();
    } catch (error) {
        console.error('Error deleting expense:', error);
        showToast('Error deleting expense', 'error');
    }
}

async function handleSubmit(event) {
    event.preventDefault();
    
    const form = event.target;
    const isTimeBound = form.querySelector('[name="isTimeBound"]').checked;
    
    const data = {
        category: form.querySelector('[name="category"]').value,
        name: form.querySelector('[name="name"]').value,
        amount: parseFloat(form.querySelector('[name="amount"]').value) || 0,
        frequency: form.querySelector('[name="frequency"]').value,
        isFixed: form.querySelector('[name="isFixed"]').checked,
        isTimeBound: isTimeBound
    };
    
    if (isTimeBound) {
        const startDate = form.querySelector('[name="startDate"]').value;
        const endDate = form.querySelector('[name="endDate"]').value;
        const dependentName = form.querySelector('[name="dependentName"]').value;
        const dependentDob = form.querySelector('[name="dependentDob"]').value;
        const endAge = form.querySelector('[name="endAge"]').value;
        const annualIncreasePercent = form.querySelector('[name="annualIncreasePercent"]').value;
        
        if (startDate) data.startDate = startDate;
        if (endDate) data.endDate = endDate;
        if (dependentName) data.dependentName = dependentName;
        if (dependentDob) data.dependentDob = dependentDob;
        if (endAge) data.endAge = parseInt(endAge);
        if (annualIncreasePercent) data.annualIncreasePercent = parseFloat(annualIncreasePercent);
    }
    
    try {
        if (currentEditId) {
            await api.expenses.update(currentEditId, data);
            showToast('Expense updated!');
        } else {
            await api.expenses.create(data);
            showToast('Expense added!');
        }
        closeModal();
        loadData();
    } catch (error) {
        console.error('Error saving expense:', error);
        showToast('Error saving expense: ' + error.message, 'error');
    }
}

// UI Helper Functions
function toggleTimeBoundFields() {
    const isTimeBound = document.getElementById('isTimeBound').checked;
    const fields = document.getElementById('timebound-fields');
    const preview = document.getElementById('investment-preview');
    
    if (isTimeBound) {
        fields.classList.remove('hidden');
        updateInvestmentPreview();
    } else {
        fields.classList.add('hidden');
        preview.classList.add('hidden');
    }
}

function updateMonthlyEquivalent() {
    const amount = parseFloat(document.querySelector('[name="amount"]').value) || 0;
    const frequency = document.querySelector('[name="frequency"]').value;
    const monthlyEq = getMonthlyEquivalent(amount, frequency);
    
    document.getElementById('monthly-equivalent').textContent = formatCurrency(monthlyEq);
    updateInvestmentPreview();
}

function updateAmountWords() {
    const amount = parseFloat(document.querySelector('[name="amount"]').value) || 0;
    const wordsEl = document.getElementById('amount-words');
    
    if (amount > 0 && typeof numberToWords === 'function') {
        wordsEl.textContent = '💡 ' + numberToWords(amount);
    } else {
        wordsEl.textContent = '';
    }
}

function calculateDependentAge() {
    const dob = document.querySelector('[name="dependentDob"]').value;
    const endAge = parseInt(document.querySelector('[name="endAge"]').value) || 0;
    const infoEl = document.getElementById('dependent-age-info');
    
    if (dob) {
        const age = calculateAge(dob);
        const currentYear = new Date().getFullYear();
        let message = `Current age: ${age} years`;
        
        if (endAge > age) {
            const yearsLeft = endAge - age;
            const endYear = currentYear + yearsLeft;
            message += ` | Expense ends in ${endYear} (${yearsLeft} years from now)`;
        }
        
        infoEl.textContent = message;
        infoEl.classList.remove('hidden');
        updateInvestmentPreview();
    } else {
        infoEl.classList.add('hidden');
    }
}

function updateInvestmentPreview() {
    const isTimeBound = document.getElementById('isTimeBound').checked;
    const preview = document.getElementById('investment-preview');
    const previewText = document.getElementById('investment-preview-text');
    
    if (!isTimeBound) {
        preview.classList.add('hidden');
        return;
    }
    
    const amount = parseFloat(document.querySelector('[name="amount"]').value) || 0;
    const frequency = document.querySelector('[name="frequency"]').value;
    const monthlyEq = getMonthlyEquivalent(amount, frequency);
    
    if (monthlyEq <= 0) {
        preview.classList.add('hidden');
        return;
    }
    
    // Calculate end year
    const dob = document.querySelector('[name="dependentDob"]').value;
    const endAge = parseInt(document.querySelector('[name="endAge"]').value) || 0;
    const endDate = document.querySelector('[name="endDate"]').value;
    const currentYear = new Date().getFullYear();
    
    let endYear = currentYear + 20; // Default
    let yearsAfterEnd = 0;
    const retirementAge = 60; // Assume
    const currentAge = 35; // Assume
    const retirementYear = currentYear + (retirementAge - currentAge);
    
    if (endDate) {
        endYear = new Date(endDate).getFullYear();
    } else if (dob && endAge) {
        const dependentAge = calculateAge(dob);
        endYear = currentYear + (endAge - dependentAge);
    }
    
    yearsAfterEnd = retirementYear - endYear;
    
    if (yearsAfterEnd > 0) {
        // Calculate potential corpus
        const monthlyRate = 0.12 / 12; // 12% annual
        const months = yearsAfterEnd * 12;
        const futureValue = monthlyEq * ((Math.pow(1 + monthlyRate, months) - 1) / monthlyRate) * (1 + monthlyRate);
        
        previewText.innerHTML = `
            When this expense ends in <strong>${endYear}</strong>, <strong>${formatCurrency(monthlyEq)}/month</strong> will be freed up.<br>
            If invested at 12% for ${yearsAfterEnd} years until retirement, potential corpus: <strong class="text-emerald-700">${formatCurrency(futureValue, true)}</strong>
        `;
        preview.classList.remove('hidden');
    } else {
        previewText.textContent = `This expense ends after retirement (${endYear}). No investment opportunity before retirement.`;
        preview.classList.remove('hidden');
    }
}

// Add input listener for amount words
document.addEventListener('DOMContentLoaded', () => {
    const amountInput = document.querySelector('[name="amount"]');
    if (amountInput) {
        amountInput.addEventListener('input', updateAmountWords);
    }
});
