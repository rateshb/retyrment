// Retyrment - My Account Page

let userDataSummary = null;

document.addEventListener('DOMContentLoaded', () => {
    loadAccountInfo();
    loadDataSummary();
    applyFeatureRestrictions();
});

async function loadAccountInfo() {
    const loading = document.getElementById('loading');
    const content = document.getElementById('account-content');
    const errorState = document.getElementById('error-state');

    loading.classList.remove('hidden');
    content.classList.add('hidden');
    errorState.classList.add('hidden');

    try {
        const user = auth.getUser();
        if (!user) {
            window.location.href = 'login.html';
            return;
        }

        // Load feature access
        const featuresResponse = await api.auth.features();
        const features = featuresResponse.features || {};

        // Update profile information
        document.getElementById('user-name').textContent = user.name || 'Not set';
        document.getElementById('user-email').textContent = user.email || 'Not set';
        document.getElementById('user-role').textContent = user.role || 'FREE';
        document.getElementById('effective-role').textContent = user.effectiveRole || user.role || 'FREE';

        // Update subscription info
        updateSubscriptionInfo(user);

        // Update feature access
        updateFeatureAccess(features);

        // Update investment types
        updateInvestmentTypes(features.allowedInvestmentTypes || []);

        // Update insurance restrictions
        updateInsuranceRestrictions(features.blockedInsuranceTypes || []);

        loading.classList.add('hidden');
        content.classList.remove('hidden');

    } catch (error) {
        console.error('Error loading account info:', error);
        loading.classList.add('hidden');
        errorState.classList.remove('hidden');
    }
}

function updateSubscriptionInfo(user) {
    const subscriptionInfo = document.getElementById('subscription-info');
    let html = '';

    // Trial information
    if (user.trial && user.trial.active) {
        html += `
            <div class="p-4 bg-warning-50 border border-warning-200 rounded-lg mb-4">
                <div class="flex items-center justify-between">
                    <div>
                        <div class="font-medium text-warning-800">Trial Period Active</div>
                        <div class="text-sm text-warning-600">${user.trial.daysRemaining || 0} days remaining</div>
                    </div>
                    <div class="text-2xl">⭐</div>
                </div>
            </div>
        `;
    }

    // PRO Subscription information
    if (user.subscription) {
        if (user.subscription.active) {
            html += `
                <div class="p-4 bg-success-50 border border-success-200 rounded-lg mb-4">
                    <div class="flex items-center justify-between">
                        <div>
                            <div class="font-medium text-success-800">PRO Subscription Active</div>
                            <div class="text-sm text-success-600">${user.subscription.daysRemaining || 0} days remaining</div>
                            ${user.subscription.endDate ? `<div class="text-xs text-success-500 mt-1">Expires: ${new Date(user.subscription.endDate).toLocaleDateString()}</div>` : ''}
                        </div>
                        <div class="text-2xl">✨</div>
                    </div>
                </div>
            `;
        } else {
            html += `
                <div class="p-4 bg-slate-50 border border-slate-200 rounded-lg mb-4">
                    <div class="flex items-center justify-between">
                        <div>
                            <div class="font-medium text-slate-800">PRO Subscription Expired</div>
                            <div class="text-sm text-slate-600">Your subscription has ended</div>
                        </div>
                        <div class="text-2xl">⏰</div>
                    </div>
                </div>
            `;
        }
    }

    // Role expiry information
    if (user.roleInfo && user.roleInfo.temporary) {
        html += `
            <div class="p-4 bg-primary-50 border border-primary-200 rounded-lg mb-4">
                <div class="flex items-center justify-between">
                    <div>
                        <div class="font-medium text-primary-800">Temporary Role Access</div>
                        <div class="text-sm text-primary-600">${user.roleInfo.daysRemaining || 0} days remaining</div>
                        ${user.roleInfo.originalRole ? `<div class="text-xs text-primary-500 mt-1">Will revert to: ${user.roleInfo.originalRole}</div>` : ''}
                    </div>
                    <div class="text-2xl">🔑</div>
                </div>
            </div>
        `;
    }

    if (!html) {
        html = '<div class="text-slate-500">No active subscription or trial</div>';
    }

    subscriptionInfo.innerHTML = html;
}

function updateFeatureAccess(features) {
    const featureList = document.getElementById('feature-access-list');
    const pageFeatures = [
        { key: 'incomePage', label: 'Income Page', icon: '💰' },
        { key: 'investmentPage', label: 'Investment Page', icon: '📈' },
        { key: 'loanPage', label: 'Loan Page', icon: '🏦' },
        { key: 'insurancePage', label: 'Insurance Page', icon: '🛡️' },
        { key: 'expensePage', label: 'Expense Page', icon: '🛒' },
        { key: 'goalsPage', label: 'Goals Page', icon: '🎯' },
        { key: 'calendarPage', label: 'Calendar Page', icon: '📅' },
        { key: 'retirementPage', label: 'Retirement Page', icon: '🏖️' },
        { key: 'reportsPage', label: 'Reports Page', icon: '📑' },
        { key: 'simulationPage', label: 'Monte Carlo Simulation', icon: '🎲' },
        { key: 'adminPanel', label: 'Admin Panel', icon: '👑' },
        { key: 'preferencesPage', label: 'Preferences Page', icon: '⚙️' },
        { key: 'settingsPage', label: 'Settings Page', icon: '🔧' },
        { key: 'accountPage', label: 'My Account Page', icon: '👤' }
    ];

    let html = '';
    let enabledCount = 0;
    
    // Only show enabled features
    pageFeatures.forEach(feature => {
        const hasAccess = features[feature.key] === true;
        if (hasAccess) {
            enabledCount++;
            html += `
                <div class="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
                    <div class="flex items-center gap-3">
                        <span class="text-xl">${feature.icon}</span>
                        <span class="font-medium text-slate-800">${feature.label}</span>
                    </div>
                    <span class="px-3 py-1 rounded-full text-sm font-medium bg-success-100 text-success-700">
                        ✓ Enabled
                    </span>
                </div>
            `;
        }
    });
    
    if (enabledCount === 0) {
        html = '<div class="text-slate-500 text-center py-4">No features are currently enabled for your account.</div>';
    }

    featureList.innerHTML = html;
}

function updateInvestmentTypes(allowedTypes) {
    const investmentTypes = document.getElementById('investment-types');
    const allTypes = ['MUTUAL_FUND', 'PPF', 'EPF', 'FD', 'RD', 'REAL_ESTATE', 'STOCK', 'NPS', 'GOLD', 'CRYPTO', 'CASH'];
    
    let html = '';
    allTypes.forEach(type => {
        const isAllowed = allowedTypes.includes(type);
        html += `
            <span class="px-3 py-1 rounded-full text-sm font-medium ${isAllowed ? 'bg-success-100 text-success-700' : 'bg-slate-200 text-slate-600'}">
                ${type.replace('_', ' ')} ${isAllowed ? '✓' : '✗'}
            </span>
        `;
    });

    investmentTypes.innerHTML = html;
}

function updateInsuranceRestrictions(blockedTypes) {
    const restrictions = document.getElementById('insurance-restrictions');
    
    if (blockedTypes.length === 0) {
        restrictions.innerHTML = '<div class="text-slate-500">No restrictions - All insurance types are allowed</div>';
        return;
    }

    let html = '<div class="text-sm text-slate-600 mb-2">The following insurance types are restricted:</div>';
    blockedTypes.forEach(type => {
        html += `
            <div class="flex items-center gap-2 p-2 bg-danger-50 rounded">
                <span class="text-danger-600">✗</span>
                <span class="text-slate-800">${type.replace('_', ' ')}</span>
            </div>
        `;
    });

    restrictions.innerHTML = html;
}

// Load user data summary
async function loadDataSummary() {
    try {
        userDataSummary = await api.userData.getSummary();
        updateDataSummaryDisplay(userDataSummary);
    } catch (error) {
        console.error('Error loading data summary:', error);
        // Show placeholder values on error
        const placeholderSummary = {
            income: 0, investments: 0, loans: 0, insurance: 0,
            expenses: 0, goals: 0, familyMembers: 0, total: 0
        };
        updateDataSummaryDisplay(placeholderSummary);
    }
}

// Update data summary display
function updateDataSummaryDisplay(summary) {
    document.getElementById('count-income').textContent = summary.income || 0;
    document.getElementById('count-investments').textContent = summary.investments || 0;
    document.getElementById('count-loans').textContent = summary.loans || 0;
    document.getElementById('count-insurance').textContent = summary.insurance || 0;
    document.getElementById('count-expenses').textContent = summary.expenses || 0;
    document.getElementById('count-goals').textContent = summary.goals || 0;
    document.getElementById('count-family').textContent = summary.familyMembers || 0;
    document.getElementById('count-total').textContent = summary.total || 0;
}

// Open delete data modal
function openDeleteDataModal() {
    const modal = document.getElementById('delete-data-modal');
    
    // Update modal counts
    if (userDataSummary) {
        document.getElementById('modal-count-income').textContent = userDataSummary.income || 0;
        document.getElementById('modal-count-investments').textContent = userDataSummary.investments || 0;
        document.getElementById('modal-count-loans').textContent = userDataSummary.loans || 0;
        document.getElementById('modal-count-insurance').textContent = userDataSummary.insurance || 0;
        document.getElementById('modal-count-expenses').textContent = userDataSummary.expenses || 0;
        document.getElementById('modal-count-goals').textContent = userDataSummary.goals || 0;
        document.getElementById('modal-count-family').textContent = userDataSummary.familyMembers || 0;
        document.getElementById('modal-count-total').textContent = userDataSummary.total || 0;
    }
    
    // Reset form
    document.getElementById('understand-checkbox').checked = false;
    document.getElementById('delete-confirmation-input').value = '';
    updateDeleteButton();
    
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

// Close delete data modal
function closeDeleteDataModal() {
    const modal = document.getElementById('delete-data-modal');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

// Update delete button state
function updateDeleteButton() {
    const checkbox = document.getElementById('understand-checkbox');
    const input = document.getElementById('delete-confirmation-input');
    const button = document.getElementById('confirm-delete-btn');
    
    const isChecked = checkbox.checked;
    const isTextCorrect = input.value.trim() === 'DELETE';
    
    button.disabled = !(isChecked && isTextCorrect);
}

// Confirm and execute deletion
async function confirmDeleteData() {
    const button = document.getElementById('confirm-delete-btn');
    
    // Disable button to prevent double-click
    button.disabled = true;
    button.textContent = '🔄 Deleting...';
    
    try {
        // Call API with confirmation token
        const result = await api.userData.deleteAll('DELETE_ALL_DATA');
        
        if (result.success) {
            // Close modal
            closeDeleteDataModal();
            
            // Show success message
            alert(`✅ Success!\n\nDeleted ${result.total || 0} records:\n` +
                  `- Income: ${result.income || 0}\n` +
                  `- Investments: ${result.investments || 0}\n` +
                  `- Loans: ${result.loans || 0}\n` +
                  `- Insurance: ${result.insurance || 0}\n` +
                  `- Expenses: ${result.expenses || 0}\n` +
                  `- Goals: ${result.goals || 0}\n` +
                  `- Family Members: ${result.familyMembers || 0}\n` +
                  `- Preferences: ${result.preferences || 0}\n\n` +
                  `Your account remains active. You can start adding data again or log out.`);
            
            // Reload data summary
            await loadDataSummary();
            
            // Optionally redirect to dashboard
            // window.location.href = 'index.html';
        } else {
            throw new Error(result.error || result.message || 'Failed to delete data');
        }
        
    } catch (error) {
        console.error('Error deleting user data:', error);
        alert(`❌ Error!\n\nFailed to delete data: ${error.message}\n\nPlease try again or contact support if the problem persists.`);
        
        // Re-enable button
        button.disabled = false;
        button.textContent = '🗑️ Delete All Data';
    }
}
