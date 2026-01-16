// Retyrment - Insurance Page (Simplified UX)

// Helper function for formatting numbers
function formatNumber(num) {
    if (!num || isNaN(num)) return '0';
    return Math.round(num).toLocaleString('en-IN');
}

// Simple policy categories that users understand
const POLICY_CATEGORIES = {
    'HEALTH': { 
        label: '🏥 Health Insurance', 
        description: 'Medical coverage for you and family',
        types: ['HEALTH'],
        questions: ['healthType', 'sumAssured', 'premium', 'renewalMonth']
    },
    'TERM_LIFE': { 
        label: '🛡️ Term Life Insurance', 
        description: 'Pure protection, no returns',
        types: ['TERM_LIFE'],
        questions: ['sumAssured', 'premium', 'coverageEndAge', 'renewalMonth']
    },
    'LIFE_SAVINGS': { 
        label: '💰 Life + Savings (LIC, etc.)', 
        description: 'Endowment, Money-back, ULIP policies',
        types: ['ENDOWMENT', 'MONEY_BACK', 'ULIP'],
        questions: ['subType', 'sumAssured', 'premium', 'policyTerm', 'maturityBenefit']
    },
    'PENSION': { 
        label: '🏖️ Pension / Annuity', 
        description: 'Pay now, receive monthly later',
        types: ['ANNUITY'],
        questions: ['premium', 'premiumYears', 'monthlyAnnuity', 'annuityStartYear']
    },
    'VEHICLE': { 
        label: '🚗 Vehicle Insurance', 
        description: 'Car, bike insurance',
        types: ['VEHICLE'],
        questions: ['sumAssured', 'premium', 'renewalMonth']
    },
    'OTHER': { 
        label: '📋 Other Insurance', 
        description: 'Travel, accident, gadget, etc.',
        types: ['OTHER'],
        questions: ['sumAssured', 'premium', 'renewalMonth']
    }
};

// Health insurance subtypes
const HEALTH_TYPES = {
    'PERSONAL': { label: 'Personal (Individual)', postRetirement: true },
    'FAMILY_FLOATER': { label: 'Family Floater', postRetirement: true },
    'GROUP': { label: 'Group (Employer)', postRetirement: false }
};

// Life savings subtypes
const LIFE_SUBTYPES = {
    'ENDOWMENT': { label: 'Endowment (e.g., LIC Jeevan Anand)', hasMaturity: true },
    'MONEY_BACK': { label: 'Money Back (periodic payouts)', hasMaturity: true, hasMoneyBack: true },
    'ULIP': { label: 'ULIP (market-linked)', hasMaturity: true, hasFundValue: true }
};

document.addEventListener('DOMContentLoaded', async () => { 
    console.log('Insurance page DOMContentLoaded');
    
    // Load feature access to get blocked insurance types
    try {
        const featuresResponse = await api.auth.features();
        const features = featuresResponse.features || {};
        // Always set it, even if empty array (to distinguish from undefined)
        if (features.blockedInsuranceTypes !== undefined) {
            window.blockedInsuranceTypes = features.blockedInsuranceTypes;
        }
    } catch (error) {
        console.error('Error loading feature access:', error);
    }
    
    // Show loading state but with timeout fallback
    setTimeout(() => {
        const table = document.getElementById('data-table');
        if (table && table.innerHTML.includes('Loading')) {
            console.log('Timeout reached, showing empty state');
            table.innerHTML = `<tr><td colspan="5" class="px-6 py-8 text-center text-slate-500">No policies yet. Click "Add Policy" to add your first insurance.</td></tr>`;
            document.getElementById('entry-count').textContent = '0 policies';
        }
    }, 5000); // 5 second timeout
    
    loadData(); 
});

async function loadData() {
    try {
        console.log('Loading insurance data...');
        const data = await api.insurance.getAll();
        console.log('Insurance data loaded:', data);
        renderTable(data || []);
        updateSummaryCards(data || []);
        document.getElementById('entry-count').textContent = `${(data || []).length} policies`;
    } catch (error) {
        console.error('Error loading insurance data:', error);
        document.getElementById('data-table').innerHTML = `<tr><td colspan="5" class="px-6 py-8 text-center text-danger-400">Error loading data: ${error.message || 'Unknown error'}</td></tr>`;
    }
}

function updateSummaryCards(data) {
    let healthCover = 0;
    let lifeCover = 0;
    let totalPremium = 0;
    let postRetirePremium = 0;
    
    data.forEach(policy => {
        const premium = policy.annualPremium || 0;
        const cover = policy.sumAssured || 0;
        totalPremium += premium;
        
        if (policy.type === 'HEALTH') {
            healthCover += cover;
            // Personal/Family health continues after retirement
            if (policy.healthType !== 'GROUP') {
                postRetirePremium += premium;
            }
        } else if (policy.type === 'TERM_LIFE') {
            lifeCover += cover;
            // Term life continues if explicitly set or by default
            if (policy.continuesAfterRetirement !== false) {
                postRetirePremium += premium;
            }
        } else if (['ULIP', 'ENDOWMENT', 'MONEY_BACK'].includes(policy.type)) {
            lifeCover += cover;
        }
    });
    
    document.getElementById('health-total').textContent = formatCurrency(healthCover);
    document.getElementById('life-total').textContent = formatCurrency(lifeCover);
    document.getElementById('premium-total').textContent = formatCurrency(totalPremium) + '/yr';
    document.getElementById('post-retire-premium').textContent = formatCurrency(postRetirePremium) + '/yr';
}

function renderTable(data) {
    const tbody = document.getElementById('data-table');
    if (data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="px-6 py-8 text-center text-slate-500">No insurance policies yet. Add your first policy!</td></tr>`;
        return;
    }
    
    tbody.innerHTML = data.map(item => {
        const category = getCategoryForType(item.type);
        const categoryInfo = POLICY_CATEGORIES[category] || {};
        const badge = getBadge(item);
        const premium = formatPremiumDisplay(item);
        
        return `
        <tr class="hover:bg-slate-50 border-b border-slate-100">
            <td class="px-6 py-4">
                <div class="flex items-center gap-3">
                    <div class="text-2xl">${categoryInfo.label?.split(' ')[0] || '📋'}</div>
                    <div>
                        <div class="font-medium text-slate-800">${item.policyName || 'Policy'}</div>
                        <div class="text-xs text-slate-500">${item.company || ''} ${badge}</div>
                    </div>
                </div>
            </td>
            <td class="px-6 py-4 font-mono text-success-600">${formatCurrency(item.sumAssured)}</td>
            <td class="px-6 py-4">
                <div class="font-mono">${premium}</div>
                <div class="text-xs text-slate-400">${getFrequencyLabel(item.premiumFrequency)}</div>
            </td>
            <td class="px-6 py-4 text-xs text-slate-500">${getStatusInfo(item)}</td>
            <td class="px-6 py-4 text-right">
                <button onclick="editItem('${item.id}')" class="text-primary-600 hover:text-primary-800 mr-3">Edit</button>
                <button onclick="deleteItem('${item.id}')" class="text-slate-400 hover:text-danger-500">Delete</button>
            </td>
        </tr>`;
    }).join('');
}

function getCategoryForType(type) {
    for (const [category, info] of Object.entries(POLICY_CATEGORIES)) {
        if (info.types.includes(type)) return category;
    }
    return 'OTHER';
}

function getBadge(item) {
    if (item.type === 'HEALTH') {
        if (item.healthType === 'GROUP') return '<span class="px-1.5 py-0.5 text-xs bg-slate-100 text-slate-600 rounded">Group</span>';
        if (item.healthType === 'FAMILY_FLOATER') return '<span class="px-1.5 py-0.5 text-xs bg-blue-100 text-blue-700 rounded">Family</span>';
        return '<span class="px-1.5 py-0.5 text-xs bg-green-100 text-green-700 rounded">Personal</span>';
    }
    if (['ULIP', 'ENDOWMENT', 'MONEY_BACK'].includes(item.type)) {
        return `<span class="px-1.5 py-0.5 text-xs bg-purple-100 text-purple-700 rounded">${item.type.replace('_', ' ')}</span>`;
    }
    return '';
}

function formatPremiumDisplay(item) {
    const amount = item.annualPremium || item.premiumAmount || 0;
    return formatCurrency(amount) + '/yr';
}

function getFrequencyLabel(freq) {
    const labels = { 'MONTHLY': 'Monthly', 'QUARTERLY': 'Quarterly', 'HALF_YEARLY': 'Half-Yearly', 'YEARLY': 'Yearly', 'SINGLE': 'One-time' };
    return labels[freq] || 'Yearly';
}

function getStatusInfo(item) {
    const info = [];
    if (item.maturityDate) {
        const matYear = new Date(item.maturityDate).getFullYear();
        info.push(`Mat: ${matYear}`);
    }
    if (item.coverageEndAge) info.push(`Till age ${item.coverageEndAge}`);
    if (item.monthlyAnnuityAmount) info.push(`₹${formatNumber(item.monthlyAnnuityAmount)}/mo annuity`);
    if (item.continuesAfterRetirement || (item.type === 'HEALTH' && item.healthType !== 'GROUP')) {
        info.push('🔄 Post-retire');
    }
    return info.join(' • ') || '-';
}

// ========== SIMPLIFIED ADD/EDIT MODAL ==========

// currentEditId is declared in common.js
let currentCategory = null;
let currentStep = 1;

function openAddModal() {
    console.log('openAddModal called');
    currentEditId = null;
    currentCategory = null;
    currentStep = 1;
    showCategorySelection();
    console.log('showCategorySelection completed');
}

function showCategorySelection() {
    // Close any existing modal first
    closeModal();
    
    const modalHtml = `
    <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" id="modal-backdrop" onclick="closeModal()">
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 max-h-[90vh] overflow-hidden" onclick="event.stopPropagation()">
            <div class="p-6 border-b border-slate-200">
                <h2 class="text-xl font-semibold text-slate-800">Add Insurance Policy</h2>
                <p class="text-sm text-slate-500 mt-1">What type of insurance do you want to add?</p>
            </div>
            <div class="p-4 space-y-2 max-h-96 overflow-y-auto">
                ${Object.entries(POLICY_CATEGORIES)
                    .filter(([key, cat]) => {
                        // Filter out blocked insurance types
                        const blockedTypes = window.blockedInsuranceTypes;
                        
                        // If blockedTypes is not set (undefined/null), show all (backward compatibility)
                        if (!blockedTypes || blockedTypes.length === 0) {
                            return true; // No restrictions, show all
                        }
                        
                        // Check if the category key itself is blocked (e.g., "VEHICLE", "PENSION", "LIFE_SAVINGS")
                        if (blockedTypes.includes(key)) {
                            return false;
                        }
                        
                        // Also check if any type in this category is blocked
                        // Note: "PENSION" category maps to "ANNUITY" type, but backend blocks "PENSION" as category name
                        // "LIFE_SAVINGS" category maps to ['ENDOWMENT', 'MONEY_BACK', 'ULIP']
                        const isBlocked = cat.types.some(type => blockedTypes.includes(type));
                        return !isBlocked;
                    })
                    .map(([key, cat]) => `
                    <button onclick="selectCategory('${key}')" 
                            class="w-full p-4 text-left rounded-xl border-2 border-slate-200 hover:border-primary-400 hover:bg-primary-50 transition-all">
                        <div class="font-medium text-slate-800">${cat.label}</div>
                        <div class="text-sm text-slate-500">${cat.description}</div>
                    </button>
                `).join('')}
            </div>
            <div class="p-4 border-t border-slate-200 bg-slate-50">
                <button onclick="closeModal()" class="w-full py-2 text-slate-600 hover:text-slate-800">Cancel</button>
            </div>
        </div>
    </div>`;
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
}

function selectCategory(category) {
    currentCategory = category;
    showPolicyForm(category);
}

function showPolicyForm(category, existingData = {}) {
    const cat = POLICY_CATEGORIES[category];
    const isHealth = category === 'HEALTH';
    const isTermLife = category === 'TERM_LIFE';
    const isLifeSavings = category === 'LIFE_SAVINGS';
    const isPension = category === 'PENSION';
    
    // Determine actual type
    let actualType = cat.types[0];
    if (existingData.type) actualType = existingData.type;
    
    const modalHtml = `
    <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" id="modal-backdrop" onclick="closeModal()">
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 max-h-[90vh] overflow-hidden" onclick="event.stopPropagation()">
            <div class="p-6 border-b border-slate-200 bg-gradient-to-r from-primary-50 to-white">
                <div class="flex items-center gap-3">
                    <div class="text-3xl">${cat.label.split(' ')[0]}</div>
                    <div>
                        <h2 class="text-xl font-semibold text-slate-800">${currentEditId ? 'Edit' : 'Add'} ${cat.label.substring(2)}</h2>
                        <p class="text-sm text-slate-500">${cat.description}</p>
                    </div>
                </div>
            </div>
            
            <form onsubmit="handleSubmit(event)" class="p-6 space-y-4 max-h-[60vh] overflow-y-auto">
                <input type="hidden" name="type" value="${actualType}">
                
                <!-- Basic Info -->
                <div class="grid grid-cols-2 gap-4">
                    <div class="col-span-2">
                        <label class="block text-sm font-medium text-slate-700 mb-1">Policy Name *</label>
                        <input type="text" name="policyName" value="${existingData.policyName || ''}" required
                               placeholder="e.g., Star Health Family" 
                               class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500">
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-slate-700 mb-1">Company</label>
                        <input type="text" name="company" value="${existingData.company || ''}"
                               placeholder="e.g., Star Health, LIC" 
                               class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500">
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-slate-700 mb-1">Start Date</label>
                        <input type="date" name="startDate" value="${existingData.startDate || ''}"
                               class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500">
                    </div>
                </div>
                
                ${isHealth ? `
                <!-- Health Insurance Specific -->
                <div class="p-4 bg-blue-50 rounded-lg border border-blue-200">
                    <label class="block text-sm font-medium text-blue-800 mb-2">Coverage Type *</label>
                    <div class="grid grid-cols-3 gap-2">
                        ${Object.entries(HEALTH_TYPES).map(([key, info]) => `
                            <label class="flex items-center p-2 bg-white rounded-lg border cursor-pointer hover:border-blue-400 ${existingData.healthType === key ? 'border-blue-500 ring-2 ring-blue-200' : 'border-slate-200'}">
                                <input type="radio" name="healthType" value="${key}" ${existingData.healthType === key || (!existingData.healthType && key === 'PERSONAL') ? 'checked' : ''} class="mr-2">
                                <span class="text-sm">${info.label.split(' ')[0]}</span>
                            </label>
                        `).join('')}
                    </div>
                    <p class="text-xs text-blue-600 mt-2">💡 Group insurance ends at retirement. Personal/Family continues.</p>
                </div>
                ` : ''}
                
                ${isLifeSavings ? `
                <!-- Life Savings Type Selection -->
                <div class="p-4 bg-purple-50 rounded-lg border border-purple-200">
                    <label class="block text-sm font-medium text-purple-800 mb-2">Policy Type *</label>
                    <select name="type" class="w-full px-3 py-2 border border-purple-300 rounded-lg bg-white">
                        ${Object.entries(LIFE_SUBTYPES).map(([key, info]) => `
                            <option value="${key}" ${existingData.type === key ? 'selected' : ''}>${info.label}</option>
                        `).join('')}
                    </select>
                </div>
                ` : ''}
                
                <!-- Coverage & Premium -->
                <div class="grid grid-cols-2 gap-4">
                    ${!isPension ? `
                    <div>
                        <label class="block text-sm font-medium text-slate-700 mb-1">
                            ${isHealth ? 'Coverage Amount' : 'Sum Assured'} (₹) *
                        </label>
                        <input type="number" name="sumAssured" value="${existingData.sumAssured || ''}" required
                               placeholder="e.g., 500000" 
                               class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500">
                        <p class="text-xs text-slate-400 mt-1" id="sum-words"></p>
                    </div>
                    ` : ''}
                    <div>
                        <label class="block text-sm font-medium text-slate-700 mb-1">Annual Premium (₹) *</label>
                        <input type="number" name="annualPremium" value="${existingData.annualPremium || ''}" required
                               placeholder="e.g., 25000" 
                               class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500">
                        <p class="text-xs text-slate-400 mt-1" id="premium-words"></p>
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-slate-700 mb-1">Payment Frequency</label>
                        <select name="premiumFrequency" class="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white">
                            <option value="YEARLY" ${existingData.premiumFrequency === 'YEARLY' || !existingData.premiumFrequency ? 'selected' : ''}>Yearly</option>
                            <option value="HALF_YEARLY" ${existingData.premiumFrequency === 'HALF_YEARLY' ? 'selected' : ''}>Half-Yearly</option>
                            <option value="QUARTERLY" ${existingData.premiumFrequency === 'QUARTERLY' ? 'selected' : ''}>Quarterly</option>
                            <option value="MONTHLY" ${existingData.premiumFrequency === 'MONTHLY' ? 'selected' : ''}>Monthly</option>
                        </select>
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-slate-700 mb-1">Renewal Month</label>
                        <select name="renewalMonth" class="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white">
                            ${['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'].map((m, i) => 
                                `<option value="${i+1}" ${existingData.renewalMonth === i+1 ? 'selected' : ''}>${m}</option>`
                            ).join('')}
                        </select>
                    </div>
                </div>
                
                ${isTermLife ? `
                <!-- Term Life Specific -->
                <div class="p-4 bg-green-50 rounded-lg border border-green-200">
                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="block text-sm font-medium text-green-800 mb-1">Coverage Until Age</label>
                            <input type="number" name="coverageEndAge" value="${existingData.coverageEndAge || 65}" min="50" max="99"
                                   class="w-full px-3 py-2 border border-green-300 rounded-lg bg-white">
                        </div>
                        <div class="flex items-center">
                            <label class="flex items-center gap-2 cursor-pointer">
                                <input type="checkbox" name="continuesAfterRetirement" ${existingData.continuesAfterRetirement ? 'checked' : ''}
                                       class="w-4 h-4 text-green-600 rounded">
                                <span class="text-sm text-green-800">Premium continues after retirement</span>
                            </label>
                        </div>
                    </div>
                </div>
                ` : ''}
                
                ${isLifeSavings ? `
                <!-- Life Savings Specific -->
                <div class="p-4 bg-amber-50 rounded-lg border border-amber-200 space-y-3">
                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="block text-sm font-medium text-amber-800 mb-1">Policy Term (Years)</label>
                            <input type="number" name="policyTerm" value="${existingData.policyTerm || 20}" min="5" max="50"
                                   class="w-full px-3 py-2 border border-amber-300 rounded-lg bg-white">
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-amber-800 mb-1">Expected Maturity (₹)</label>
                            <input type="number" name="maturityBenefit" value="${existingData.maturityBenefit || ''}"
                                   placeholder="e.g., 1000000" 
                                   class="w-full px-3 py-2 border border-amber-300 rounded-lg bg-white">
                        </div>
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-amber-800 mb-1">Maturity Date</label>
                        <input type="date" name="maturityDate" value="${existingData.maturityDate || ''}"
                               class="w-full px-3 py-2 border border-amber-300 rounded-lg bg-white">
                    </div>
                    <p class="text-xs text-amber-700">💡 Maturity amount will be added to your corpus in that year</p>
                </div>
                ` : ''}
                
                ${isPension ? `
                <!-- Pension/Annuity Specific -->
                <div class="p-4 bg-teal-50 rounded-lg border border-teal-200 space-y-3">
                    <p class="text-sm text-teal-800 font-medium">Pension Plan Details</p>
                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="block text-sm font-medium text-teal-800 mb-1">Pay Premium for (Years)</label>
                            <input type="number" name="premiumPaymentYears" value="${existingData.premiumPaymentYears || 15}" min="1" max="40"
                                   class="w-full px-3 py-2 border border-teal-300 rounded-lg bg-white">
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-teal-800 mb-1">Annuity Starts After (Years)</label>
                            <input type="number" name="annuityStartYear" value="${existingData.annuityStartYear || 15}" min="1" max="40"
                                   class="w-full px-3 py-2 border border-teal-300 rounded-lg bg-white">
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-teal-800 mb-1">Monthly Annuity (₹)</label>
                            <input type="number" name="monthlyAnnuityAmount" value="${existingData.monthlyAnnuityAmount || ''}"
                                   placeholder="Amount you'll receive monthly" 
                                   class="w-full px-3 py-2 border border-teal-300 rounded-lg bg-white">
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-teal-800 mb-1">Annual Increase (%)</label>
                            <input type="number" name="annuityGrowthRate" value="${existingData.annuityGrowthRate || 3}" step="0.5" min="0" max="10"
                                   class="w-full px-3 py-2 border border-teal-300 rounded-lg bg-white">
                        </div>
                    </div>
                    <p class="text-xs text-teal-700">💡 You pay premium for specified years, then receive monthly income</p>
                </div>
                ` : ''}
                
            </form>
            
            <div class="p-4 border-t border-slate-200 bg-slate-50 flex justify-between">
                <button onclick="${currentEditId ? 'closeModal()' : 'showCategorySelection()'}" class="px-4 py-2 text-slate-600 hover:text-slate-800">
                    ${currentEditId ? 'Cancel' : '← Back'}
                </button>
                <button onclick="submitForm()" class="px-6 py-2 bg-primary-600 hover:bg-primary-700 text-white rounded-lg font-medium">
                    ${currentEditId ? 'Save Changes' : 'Add Policy'}
                </button>
            </div>
        </div>
    </div>`;
    
    // Close existing modal and show new one
    closeModal();
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    // Setup number to words
    setupNumberToWords();
}

function setupNumberToWords() {
    const sumInput = document.querySelector('[name="sumAssured"]');
    const premiumInput = document.querySelector('[name="annualPremium"]');
    
    if (sumInput) {
        sumInput.addEventListener('input', () => {
            const words = document.getElementById('sum-words');
            if (words) words.textContent = numberToWords(sumInput.value);
        });
        // Trigger initial
        const words = document.getElementById('sum-words');
        if (words && sumInput.value) words.textContent = numberToWords(sumInput.value);
    }
    
    if (premiumInput) {
        premiumInput.addEventListener('input', () => {
            const words = document.getElementById('premium-words');
            if (words) words.textContent = numberToWords(premiumInput.value);
        });
        // Trigger initial
        const words = document.getElementById('premium-words');
        if (words && premiumInput.value) words.textContent = numberToWords(premiumInput.value);
    }
}

function submitForm() {
    const form = document.querySelector('#modal-backdrop form');
    if (form) {
        const submitEvent = new Event('submit', { cancelable: true });
        form.dispatchEvent(submitEvent);
    }
}

async function editItem(id) {
    try {
        const data = await api.get(`/insurance/${id}`);
        currentEditId = id;
        currentCategory = getCategoryForType(data.type);
        showPolicyForm(currentCategory, data);
    } catch (error) {
        showToast('Error loading policy', 'error');
    }
}

async function deleteItem(id) {
    await confirmDelete('/insurance', id, loadData);
}

function closeModal() {
    const modal = document.getElementById('modal-backdrop');
    if (modal) modal.remove();
}

async function handleSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const data = {};
    
    formData.forEach((value, key) => {
        if (value === '' || value === null) return;
        
        // Handle checkbox
        if (key === 'continuesAfterRetirement') {
            data[key] = true;
            return;
        }
        
        // Handle numbers
        if (['sumAssured', 'annualPremium', 'premiumAmount', 'coverageEndAge', 'policyTerm', 
             'maturityBenefit', 'premiumPaymentYears', 'annuityStartYear', 'monthlyAnnuityAmount',
             'annuityGrowthRate', 'renewalMonth'].includes(key)) {
            data[key] = parseFloat(value);
        } else {
            data[key] = value;
        }
    });
    
    // Handle unchecked checkbox
    if (!data.continuesAfterRetirement) {
        data.continuesAfterRetirement = false;
    }
    
    // Set premiumAmount same as annualPremium for yearly
    if (data.annualPremium && !data.premiumAmount) {
        data.premiumAmount = data.annualPremium;
    }
    
    // For annuity, set the flag
    if (currentCategory === 'PENSION') {
        data.isAnnuityPolicy = true;
        data.type = 'ANNUITY';
    }
    
    try {
        if (currentEditId) {
            await api.insurance.update(currentEditId, data);
            showToast('Policy updated!');
        } else {
            await api.insurance.create(data);
            showToast('Policy added!');
        }
        closeModal();
        loadData();
    } catch (error) {
        showToast('Error: ' + error.message, 'error');
    }
}

// Helper function for number to words
function numberToWords(num) {
    if (!num || isNaN(num)) return '';
    num = parseFloat(num);
    if (num >= 10000000) return `₹${(num / 10000000).toFixed(2)} Crore`;
    if (num >= 100000) return `₹${(num / 100000).toFixed(2)} Lakh`;
    if (num >= 1000) return `₹${(num / 1000).toFixed(1)} Thousand`;
    return `₹${num}`;
}
